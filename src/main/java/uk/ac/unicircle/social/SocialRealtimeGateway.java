package uk.ac.unicircle.social;

import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import uk.ac.unicircle.identity.CurrentIdentity;

/**
 * Authenticated, server-to-browser event channel. The browser cannot select an
 * account id; delivery is bound to the signed-in Spring Security principal.
 */
@Component
class SocialRealtimeGateway extends TextWebSocketHandler {

    private static final String ACCOUNT_ATTRIBUTE = "unicircle.account-id";

    private final CurrentIdentity currentIdentity;
    private final Map<UUID, Set<WebSocketSession>> sessionsByAccount = new ConcurrentHashMap<>();

    SocialRealtimeGateway(CurrentIdentity currentIdentity) {
        this.currentIdentity = currentIdentity;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Principal principal = session.getPrincipal();
        if (!(principal instanceof Authentication authentication)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("A signed-in account is required."));
            return;
        }
        UUID accountId = currentIdentity.require(authentication).accountId();
        session.getAttributes().put(ACCOUNT_ATTRIBUTE, accountId);
        sessionsByAccount.computeIfAbsent(accountId, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object value = session.getAttributes().get(ACCOUNT_ATTRIBUTE);
        if (value instanceof UUID accountId) removeSession(accountId, session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Object value = session.getAttributes().get(ACCOUNT_ATTRIBUTE);
        if (value instanceof UUID accountId) removeSession(accountId, session);
        if (session.isOpen()) session.close(CloseStatus.SERVER_ERROR);
    }

    void publish(UUID accountId, String eventType, UUID resourceId) {
        String resource = resourceId == null ? "null" : "\"" + resourceId + "\"";
        String payload = "{\"type\":\"" + eventType + "\",\"resourceId\":" + resource + "}";

        Set<WebSocketSession> sessions = sessionsByAccount.get(accountId);
        if (sessions == null) return;
        sessions.forEach(session -> send(accountId, session, payload));
    }

    private void send(UUID accountId, WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            removeSession(accountId, session);
            return;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException exception) {
            removeSession(accountId, session);
        }
    }

    private void removeSession(UUID accountId, WebSocketSession session) {
        sessionsByAccount.computeIfPresent(accountId, (ignored, sessions) -> {
            sessions.remove(session);
            return sessions.isEmpty() ? null : sessions;
        });
    }
}

@Configuration
@EnableWebSocket
class SocialWebSocketConfiguration implements WebSocketConfigurer {

    private final SocialRealtimeGateway gateway;

    SocialWebSocketConfiguration(SocialRealtimeGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gateway, "/ws/social");
    }
}
