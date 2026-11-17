package uk.ac.unicircle.assistant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.assistant.AssistantRequests.AskRequest;
import uk.ac.unicircle.assistant.AssistantRequests.CreateConversationRequest;
import uk.ac.unicircle.assistant.AssistantResponses.Answer;
import uk.ac.unicircle.assistant.AssistantResponses.ConversationDetail;
import uk.ac.unicircle.assistant.AssistantResponses.ConversationMessage;
import uk.ac.unicircle.assistant.AssistantResponses.ConversationSummary;
import uk.ac.unicircle.assistant.AssistantResponses.Source;

@Service
class AssistantConversationService {
    private static final String DEFAULT_TITLE = "New chat";

    private final AssistantService assistant;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    AssistantConversationService(AssistantService assistant, JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.assistant = assistant;
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    List<ConversationSummary> list(String ownerKey) {
        return jdbc.query("""
                SELECT conversation.id, conversation.title, conversation.mode,
                       COALESCE((SELECT message.content FROM assistant_messages message
                                 WHERE message.conversation_id = conversation.id
                                 ORDER BY message.created_at DESC,
                                          CASE message.role WHEN 'ASSISTANT' THEN 0 ELSE 1 END,
                                          message.id DESC LIMIT 1), ''),
                       conversation.created_at, conversation.updated_at
                FROM assistant_conversations conversation
                WHERE conversation.owner_key = ?
                ORDER BY conversation.updated_at DESC, conversation.id DESC
                """, (result, row) -> summary(result), ownerKey);
    }

    AssistantResponses.ConversationDetail create(String ownerKey, CreateConversationRequest request) {
        UUID id = UUID.randomUUID();
        String title = cleanTitle(request.title());
        AssistantMode mode = request.mode() == null ? AssistantMode.GENERAL : request.mode();
        jdbc.update("""
                INSERT INTO assistant_conversations (id, owner_key, title, mode)
                VALUES (?, ?, ?, ?)
                """, id, ownerKey, title, mode.name());
        return detail(ownerKey, id);
    }

    @Transactional(readOnly = true)
    ConversationDetail detail(String ownerKey, UUID conversationId) {
        ConversationSummary conversation = conversation(ownerKey, conversationId);
        List<StoredMessage> storedMessages = jdbc.query("""
                SELECT id, role, content, mode, image_data_url, notice, created_at
                FROM assistant_messages
                WHERE conversation_id = ?
                ORDER BY created_at, CASE role WHEN 'USER' THEN 0 ELSE 1 END, id
                """, (result, row) -> storedMessage(result), conversationId);
        List<ConversationMessage> messages = storedMessages.stream()
                .map(this::message)
                .toList();
        return new ConversationDetail(conversation, messages);
    }

    ConversationDetail ask(String ownerKey, UUID conversationId, AskRequest request) {
        conversation(ownerKey, conversationId);
        Answer answer = assistant.ask(ownerKey, request);
        transactions.executeWithoutResult(status -> persistTurn(ownerKey, conversationId, request, answer));
        return detail(ownerKey, conversationId);
    }

    private void persistTurn(String ownerKey, UUID conversationId, AskRequest request, Answer answer) {
        ConversationSummary current = conversation(ownerKey, conversationId);
        UUID userMessageId = UUID.randomUUID();
        UUID assistantMessageId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO assistant_messages
                  (id, conversation_id, role, content, mode)
                VALUES (?, ?, 'USER', ?, ?)
                """, userMessageId, conversationId, request.message().trim(), request.mode().name());
        jdbc.update("""
                INSERT INTO assistant_messages
                  (id, conversation_id, role, content, mode, image_data_url, notice)
                VALUES (?, ?, 'ASSISTANT', ?, ?, ?, ?)
                """, assistantMessageId, conversationId, answer.answer(), answer.mode().name(),
                answer.imageDataUrl(), answer.notice());
        for (int position = 0; position < answer.sources().size(); position++) {
            Source source = answer.sources().get(position);
            jdbc.update("""
                    INSERT INTO assistant_message_sources (message_id, source_position, title, url)
                    VALUES (?, ?, ?, ?)
                    """, assistantMessageId, position, source.title(), source.url());
        }
        String title = DEFAULT_TITLE.equals(current.title())
                ? titleFromQuestion(request.message()) : current.title();
        jdbc.update("""
                UPDATE assistant_conversations
                SET title = ?, mode = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND owner_key = ?
                """, title, request.mode().name(), conversationId, ownerKey);
    }

    private ConversationSummary conversation(String ownerKey, UUID conversationId) {
        try {
            return jdbc.queryForObject("""
                    SELECT conversation.id, conversation.title, conversation.mode,
                           COALESCE((SELECT message.content FROM assistant_messages message
                                     WHERE message.conversation_id = conversation.id
                                     ORDER BY message.created_at DESC,
                                              CASE message.role WHEN 'ASSISTANT' THEN 0 ELSE 1 END,
                                              message.id DESC LIMIT 1), ''),
                           conversation.created_at, conversation.updated_at
                    FROM assistant_conversations conversation
                    WHERE conversation.id = ? AND conversation.owner_key = ?
                    """, (result, row) -> summary(result), conversationId, ownerKey);
        } catch (EmptyResultDataAccessException missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "This Ask UniCircle chat is unavailable.", missing);
        }
    }

    private ConversationSummary summary(ResultSet result) throws SQLException {
        return new ConversationSummary(result.getObject(1, UUID.class), result.getString(2),
                AssistantMode.valueOf(result.getString(3)), preview(result.getString(4)),
                result.getObject(5, OffsetDateTime.class), result.getObject(6, OffsetDateTime.class));
    }

    private StoredMessage storedMessage(ResultSet result) throws SQLException {
        return new StoredMessage(result.getObject(1, UUID.class), result.getString(2),
                result.getString(3), AssistantMode.valueOf(result.getString(4)),
                result.getString(5), result.getString(6),
                result.getObject(7, OffsetDateTime.class));
    }

    private ConversationMessage message(StoredMessage message) {
        List<Source> sources = jdbc.query("""
                SELECT title, url FROM assistant_message_sources
                WHERE message_id = ? ORDER BY source_position
                """, (sourceResult, row) -> new Source(sourceResult.getString(1), sourceResult.getString(2)),
                message.id());
        return new ConversationMessage(message.id(), message.role(), message.content(),
                message.mode(), message.imageDataUrl(), sources, message.notice(), message.createdAt());
    }

    private String cleanTitle(String requested) {
        if (requested == null || requested.isBlank()) return DEFAULT_TITLE;
        String clean = requested.replaceAll("\\s+", " ").trim();
        return clean.length() <= 120 ? clean : clean.substring(0, 117).stripTrailing() + "…";
    }

    private String titleFromQuestion(String question) {
        String clean = question.replaceAll("\\s+", " ").trim();
        return clean.length() <= 64 ? clean : clean.substring(0, 61).stripTrailing() + "…";
    }

    private String preview(String content) {
        String clean = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        return clean.length() <= 90 ? clean : clean.substring(0, 87).stripTrailing() + "…";
    }

    private record StoredMessage(UUID id, String role, String content, AssistantMode mode,
                                 String imageDataUrl, String notice, OffsetDateTime createdAt) {}
}
