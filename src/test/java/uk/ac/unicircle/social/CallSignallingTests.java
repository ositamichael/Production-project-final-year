package uk.ac.unicircle.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CallSignallingTests {

    private static final UUID MICHAEL_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be101");
    private static final UUID AISHA_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be102");
    private static final UUID JAMES_ID = UUID.fromString("7ccb9282-ebcb-4ea0-94f1-3af1f84be103");

    @Autowired
    SocialRealtimeGateway gateway;

    @Autowired
    ObjectMapper json;

    @Autowired
    MockMvc mockMvc;

    private final List<TestWebSocketSession> connectedSessions = new ArrayList<>();

    @AfterEach
    void disconnectSessions() {
        connectedSessions.forEach(session -> gateway.afterConnectionClosed(session, CloseStatus.NORMAL));
    }

    @Test
    void acceptedConnectionReceivesServerStampedCallOffer() throws Exception {
        var michael = session("student");
        var aisha = session("aisha.khan@students.unicircle.test");
        gateway.afterConnectionEstablished(michael);
        gateway.afterConnectionEstablished(aisha);

        UUID id = UUID.randomUUID();
        gateway.handleTextMessage(michael, new TextMessage("""
                {"type":"CALL_OFFER","callId":"%s","targetAccountId":"%s",
                 "fromAccountId":"spoofed","fromDisplayName":"Spoofed",
                 "media":"VIDEO","sdp":"fictional-offer-sdp"}
                """.formatted(id, AISHA_ID)));

        assertThat(aisha.messages).hasSize(1);
        JsonNode delivered = json.readTree(aisha.messages.getFirst());
        assertThat(delivered.get("type").asText()).isEqualTo("CALL_OFFER");
        assertThat(delivered.get("callId").asText()).isEqualTo(id.toString());
        assertThat(delivered.get("fromAccountId").asText()).isEqualTo(MICHAEL_ID.toString());
        assertThat(delivered.get("fromDisplayName").asText()).isEqualTo("Michael Carter");
        assertThat(delivered.get("media").asText()).isEqualTo("VIDEO");
        assertThat(delivered.get("sdp").asText()).isEqualTo("fictional-offer-sdp");
    }

    @Test
    void callToUnacceptedStudentIsRefusedAndNotDelivered() throws Exception {
        var michael = session("student");
        var james = session("james.wilson@students.unicircle.test");
        gateway.afterConnectionEstablished(michael);
        gateway.afterConnectionEstablished(james);

        UUID id = UUID.randomUUID();
        gateway.handleTextMessage(michael, new TextMessage("""
                {"type":"CALL_OFFER","callId":"%s","targetAccountId":"%s",
                 "media":"AUDIO","sdp":"fictional-offer-sdp"}
                """.formatted(id, JAMES_ID)));

        assertThat(james.messages).isEmpty();
        assertThat(michael.messages).hasSize(1);
        JsonNode refusal = json.readTree(michael.messages.getFirst());
        assertThat(refusal.get("type").asText()).isEqualTo("CALL_FORBIDDEN");
        assertThat(refusal.get("reason").asText()).contains("accepted connections");
    }

    @Test
    void authenticatedBrowserReceivesIceServerConfiguration() throws Exception {
        mockMvc.perform(get("/api/social/calls/configuration").with(user("student")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iceServers[0].urls[0]").value("stun:stun.l.google.com:19302"))
                .andExpect(jsonPath("$.relayConfigured").value(false))
                .andExpect(jsonPath("$.privacyNotice").value("Calls use browser WebRTC and are never recorded by UniCircle."));

        mockMvc.perform(get("/api/social/calls/configuration"))
                .andExpect(status().isUnauthorized());
    }

    private TestWebSocketSession session(String username) {
        TestWebSocketSession session = new TestWebSocketSession(username);
        connectedSessions.add(session);
        return session;
    }

    private static final class TestWebSocketSession implements WebSocketSession {
        private final String id = UUID.randomUUID().toString();
        private final Map<String, Object> attributes = new HashMap<>();
        private final Principal principal;
        private final List<String> messages = new ArrayList<>();
        private boolean open = true;
        private int textLimit = 64 * 1024;
        private int binaryLimit = 64 * 1024;

        private TestWebSocketSession(String username) {
            principal = UsernamePasswordAuthenticationToken.authenticated(username, "unused", List.of());
        }

        @Override public String getId() { return id; }
        @Override public URI getUri() { return URI.create("ws://localhost/ws/social"); }
        @Override public HttpHeaders getHandshakeHeaders() { return HttpHeaders.EMPTY; }
        @Override public Map<String, Object> getAttributes() { return attributes; }
        @Override public Principal getPrincipal() { return principal; }
        @Override public InetSocketAddress getLocalAddress() { return new InetSocketAddress("127.0.0.1", 8080); }
        @Override public InetSocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 49152); }
        @Override public String getAcceptedProtocol() { return null; }
        @Override public void setTextMessageSizeLimit(int messageSizeLimit) { textLimit = messageSizeLimit; }
        @Override public int getTextMessageSizeLimit() { return textLimit; }
        @Override public void setBinaryMessageSizeLimit(int messageSizeLimit) { binaryLimit = messageSizeLimit; }
        @Override public int getBinaryMessageSizeLimit() { return binaryLimit; }
        @Override public List<WebSocketExtension> getExtensions() { return List.of(); }
        @Override public void sendMessage(WebSocketMessage<?> message) { messages.add(String.valueOf(message.getPayload())); }
        @Override public boolean isOpen() { return open; }
        @Override public void close() { open = false; }
        @Override public void close(CloseStatus status) throws IOException { open = false; }
    }
}
