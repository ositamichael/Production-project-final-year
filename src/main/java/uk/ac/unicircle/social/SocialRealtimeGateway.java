package uk.ac.unicircle.social;

import java.io.IOException;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import uk.ac.unicircle.identity.CurrentIdentity;

/**
 * Authenticated, server-to-browser event channel. The browser cannot select an
 * account id; delivery is bound to the signed-in Spring Security principal.
 */
@Component
class SocialRealtimeGateway extends TextWebSocketHandler {

    private static final String ACCOUNT_ATTRIBUTE = "unicircle.account-id";
    private static final String CALL_SIGNAL_TIMES_ATTRIBUTE = "unicircle.call-signal-times";
    private static final int MAX_CALL_SIGNALS_PER_MINUTE = 300;
    private static final int MAX_CALL_PAYLOAD_BYTES = 65_536;
    private static final Set<String> CALL_SIGNAL_TYPES = Set.of(
            "CALL_OFFER", "CALL_ANSWER", "CALL_ICE_CANDIDATE", "CALL_REJECTED",
            "CALL_CANCELLED", "CALL_ENDED", "CALL_BUSY");

    private final CurrentIdentity currentIdentity;
    private final CallPermissionService callPermission;
    private final ObjectMapper json;
    private final Map<UUID, Set<WebSocketSession>> sessionsByAccount = new ConcurrentHashMap<>();

    SocialRealtimeGateway(
            CurrentIdentity currentIdentity,
            CallPermissionService callPermission,
            ObjectMapper json) {
        this.currentIdentity = currentIdentity;
        this.callPermission = callPermission;
        this.json = json;
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

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Object account = session.getAttributes().get(ACCOUNT_ATTRIBUTE);
        if (!(account instanceof UUID senderId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("A signed-in account is required."));
            return;
        }
        if (message.getPayloadLength() > MAX_CALL_PAYLOAD_BYTES || !withinSignalLimit(session)) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Call signalling limit exceeded."));
            return;
        }

        JsonNode incoming;
        try {
            incoming = json.readTree(message.getPayload());
        } catch (RuntimeException exception) {
            sendCallError(senderId, session, null, "CALL_INVALID", "The call signal was not valid JSON.");
            return;
        }

        String type = text(incoming, "type", 40);
        if (!CALL_SIGNAL_TYPES.contains(type)) return;

        UUID callId = uuid(incoming, "callId");
        UUID targetId = uuid(incoming, "targetAccountId");
        if (callId == null || targetId == null || senderId.equals(targetId)) {
            sendCallError(senderId, session, callId, "CALL_INVALID", "The call signal was incomplete.");
            return;
        }

        CallPermissionService.CallPeer sender = callPermission.acceptedPeer(senderId, targetId);
        if (sender == null) {
            sendCallError(senderId, session, callId, "CALL_FORBIDDEN",
                    "Calls are available only between accepted connections.");
            return;
        }

        ObjectNode outgoing = json.createObjectNode();
        outgoing.put("type", type);
        outgoing.put("callId", callId.toString());
        outgoing.put("fromAccountId", senderId.toString());
        outgoing.put("fromDisplayName", sender.displayName());
        outgoing.put("sentAt", OffsetDateTime.now(ZoneOffset.UTC).toString());

        if (!copyCallFields(type, incoming, outgoing)) {
            sendCallError(senderId, session, callId, "CALL_INVALID", "The call signal contained invalid data.");
            return;
        }

        boolean delivered = sendToAccount(targetId, outgoing.toString(), null);
        if (type.equals("CALL_OFFER") && !delivered) {
            sendCallError(senderId, session, callId, "CALL_UNAVAILABLE",
                    "This person is not currently online in UniCircle.");
        }
        if (Set.of("CALL_ANSWER", "CALL_REJECTED", "CALL_BUSY").contains(type)) {
            ObjectNode resolved = json.createObjectNode();
            resolved.put("type", "CALL_RESOLVED_ELSEWHERE");
            resolved.put("callId", callId.toString());
            sendToAccount(senderId, resolved.toString(), session);
        }
    }

    void publish(UUID accountId, String eventType, UUID resourceId) {
        String resource = resourceId == null ? "null" : "\"" + resourceId + "\"";
        String payload = "{\"type\":\"" + eventType + "\",\"resourceId\":" + resource + "}";

        sendToAccount(accountId, payload, null);
    }

    private boolean sendToAccount(UUID accountId, String payload, WebSocketSession excluded) {
        Set<WebSocketSession> sessions = sessionsByAccount.get(accountId);
        if (sessions == null) return false;
        boolean delivered = false;
        for (WebSocketSession session : sessions) {
            if (session != excluded) delivered |= send(accountId, session, payload);
        }
        return delivered;
    }

    private boolean send(UUID accountId, WebSocketSession session, String payload) {
        if (!session.isOpen()) {
            removeSession(accountId, session);
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
            return true;
        } catch (IOException exception) {
            removeSession(accountId, session);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean withinSignalLimit(WebSocketSession session) {
        Deque<Long> times = (Deque<Long>) session.getAttributes().computeIfAbsent(
                CALL_SIGNAL_TIMES_ATTRIBUTE, ignored -> new ArrayDeque<Long>());
        long now = System.currentTimeMillis();
        synchronized (times) {
            while (!times.isEmpty() && times.peekFirst() < now - 60_000) times.removeFirst();
            if (times.size() >= MAX_CALL_SIGNALS_PER_MINUTE) return false;
            times.addLast(now);
            return true;
        }
    }

    private boolean copyCallFields(String type, JsonNode incoming, ObjectNode outgoing) {
        if (type.equals("CALL_OFFER")) {
            String media = text(incoming, "media", 10);
            String sdp = text(incoming, "sdp", 40_000);
            if (!List.of("AUDIO", "VIDEO").contains(media) || sdp.isBlank()) return false;
            outgoing.put("media", media);
            outgoing.put("sdp", sdp);
            outgoing.put("sdpType", "offer");
        } else if (type.equals("CALL_ANSWER")) {
            String sdp = text(incoming, "sdp", 40_000);
            if (sdp.isBlank()) return false;
            outgoing.put("sdp", sdp);
            outgoing.put("sdpType", "answer");
        } else if (type.equals("CALL_ICE_CANDIDATE")) {
            String candidate = text(incoming, "candidate", 8_000);
            if (candidate.isBlank()) return false;
            outgoing.put("candidate", candidate);
            String mid = text(incoming, "sdpMid", 128);
            if (!mid.isBlank()) outgoing.put("sdpMid", mid);
            JsonNode line = incoming.get("sdpMLineIndex");
            if (line != null && line.canConvertToInt()) {
                int value = line.asInt();
                if (value < 0 || value > 64) return false;
                outgoing.put("sdpMLineIndex", value);
            }
        }
        return true;
    }

    private void sendCallError(
            UUID accountId,
            WebSocketSession session,
            UUID callId,
            String type,
            String reason) {
        ObjectNode response = json.createObjectNode();
        response.put("type", type);
        if (callId != null) response.put("callId", callId.toString());
        response.put("reason", reason);
        send(accountId, session, response.toString());
    }

    private String text(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || !value.isString()) return "";
        String text = value.asText();
        return text.length() <= maxLength ? text : "";
    }

    private UUID uuid(JsonNode node, String field) {
        try {
            String value = text(node, field, 40);
            return value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
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
