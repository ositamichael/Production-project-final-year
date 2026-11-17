package uk.ac.unicircle.social;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import uk.ac.unicircle.identity.CurrentIdentity;
import uk.ac.unicircle.identity.CurrentIdentity.SignedInAccount;
import uk.ac.unicircle.social.SocialResponses.ConnectionResponse;
import uk.ac.unicircle.social.SocialResponses.ConversationMessagesResponse;
import uk.ac.unicircle.social.SocialResponses.ConversationResponse;
import uk.ac.unicircle.social.SocialResponses.MessageResponse;
import uk.ac.unicircle.social.SocialResponses.NotificationResponse;
import uk.ac.unicircle.social.SocialResponses.SafetyActionResponse;
import uk.ac.unicircle.social.SocialResponses.StudentDirectoryResponse;

@Service
class SocialService {

    private final CurrentIdentity currentIdentity;
    private final JdbcTemplate jdbc;
    private final SocialRealtimeGateway realtime;

    SocialService(CurrentIdentity currentIdentity, JdbcTemplate jdbc, SocialRealtimeGateway realtime) {
        this.currentIdentity = currentIdentity;
        this.jdbc = jdbc;
        this.realtime = realtime;
    }

    @Transactional(readOnly = true)
    List<ConnectionResponse> connections(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT c.id,
                       CASE WHEN c.requester_id = ? THEN c.addressee_id ELSE c.requester_id END,
                       other.display_name,
                       c.status,
                       CASE WHEN c.requester_id = ? THEN 'OUTGOING' ELSE 'INCOMING' END
                FROM connections c
                JOIN user_accounts other ON other.id =
                    CASE WHEN c.requester_id = ? THEN c.addressee_id ELSE c.requester_id END
                WHERE c.university_id = ?
                  AND (c.requester_id = ? OR c.addressee_id = ?)
                  AND c.status IN ('PENDING', 'ACCEPTED')
                ORDER BY CASE c.status WHEN 'PENDING' THEN 0 ELSE 1 END, other.display_name
                """, (result, row) -> new ConnectionResponse(
                        result.getObject(1, UUID.class),
                        result.getObject(2, UUID.class),
                        result.getString(3),
                        result.getString(4),
                        result.getString(5)),
                current.accountId(), current.accountId(), current.accountId(), current.universityId(),
                current.accountId(), current.accountId());
    }

    @Transactional(readOnly = true)
    List<StudentDirectoryResponse> directory(Authentication authentication, String query) {
        SignedInAccount current = currentIdentity.require(authentication);
        String cleanQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String search = "%" + cleanQuery + "%";
        return jdbc.query("""
                SELECT ua.id, ua.display_name, ua.course_name, ua.year_of_study, ua.created_at,
                       c.id, c.status,
                       CASE
                         WHEN c.id IS NULL THEN 'NONE'
                         WHEN c.requester_id = ? THEN 'OUTGOING'
                         ELSE 'INCOMING'
                       END,
                       ua.profile_photo_key
                FROM user_accounts ua
                LEFT JOIN connections c
                  ON c.university_id = ua.university_id
                 AND ((c.requester_id = ? AND c.addressee_id = ua.id)
                   OR (c.requester_id = ua.id AND c.addressee_id = ?))
                 AND c.status IN ('PENDING', 'ACCEPTED', 'BLOCKED')
                WHERE ua.university_id = ? AND ua.id <> ?
                  AND (ua.profile_visibility = 'UNIVERSITY_ONLY'
                    OR (ua.profile_visibility = 'CONNECTIONS' AND c.status = 'ACCEPTED'))
                  AND (c.status IS NULL OR c.status <> 'BLOCKED' OR c.blocked_by_id = ?)
                  AND (? = '' OR LOWER(ua.display_name) LIKE ? OR LOWER(COALESCE(ua.course_name, '')) LIKE ?)
                ORDER BY ua.created_at DESC, ua.display_name
                """, (result, row) -> new StudentDirectoryResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getObject(4, Integer.class), result.getObject(5, OffsetDateTime.class),
                        result.getObject(6, UUID.class), result.getString(7), result.getString(8),
                        result.getString(9) != null),
                current.accountId(), current.accountId(), current.accountId(), current.universityId(),
                current.accountId(), current.accountId(), cleanQuery, search, search);
    }

    @Transactional
    ConnectionResponse requestConnection(Authentication authentication, UUID targetAccountId) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireVisibleUniversityAccount(current, targetAccountId);
        if (current.accountId().equals(targetAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot connect to your own account.");
        }

        String pairKey = pairKey(current.accountId(), targetAccountId);
        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM connections WHERE university_id = ? AND pair_key = ?",
                Integer.class, current.universityId(), pairKey);
        if (existing != null && existing > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A connection decision already exists for these accounts.");
        }

        UUID connectionId = UUID.randomUUID();
        try {
            jdbc.update("""
                    INSERT INTO connections
                      (id, university_id, requester_id, addressee_id, status, pair_key, created_at)
                    VALUES (?, ?, ?, ?, 'PENDING', ?, CURRENT_TIMESTAMP)
                    """, connectionId, current.universityId(), current.accountId(), targetAccountId, pairKey);
        } catch (DataIntegrityViolationException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A connection decision already exists for these accounts.", error);
        }
        createNotification(current.universityId(), targetAccountId, "CONNECTION_REQUEST",
                "New connection request", displayName(current.accountId()) + " would like to connect.",
                "CONNECTION", connectionId);
        publishAfterCommit(targetAccountId, "NOTIFICATIONS_CHANGED", connectionId);
        return connection(connectionId, current);
    }

    @Transactional
    ConnectionResponse accept(Authentication authentication, UUID connectionId) {
        SignedInAccount current = currentIdentity.require(authentication);
        int changed = jdbc.update("""
                UPDATE connections
                SET status = 'ACCEPTED', responded_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ? AND addressee_id = ? AND status = 'PENDING'
                """, connectionId, current.universityId(), current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only the recipient can accept an active connection request.");
        }
        UUID requesterId = jdbc.queryForObject(
                "SELECT requester_id FROM connections WHERE id = ?", UUID.class, connectionId);
        createNotification(current.universityId(), requesterId, "CONNECTION_ACCEPTED",
                "Connection accepted", displayName(current.accountId()) + " accepted your request.",
                "CONNECTION", connectionId);
        publishAfterCommit(requesterId, "CONNECTIONS_CHANGED", connectionId);
        return connection(connectionId, current);
    }

    @Transactional
    ConnectionResponse decline(Authentication authentication, UUID connectionId) {
        SignedInAccount current = currentIdentity.require(authentication);
        int changed = jdbc.update("""
                UPDATE connections
                SET status = 'DECLINED', responded_at = CURRENT_TIMESTAMP
                WHERE id = ? AND university_id = ? AND addressee_id = ? AND status = 'PENDING'
                """, connectionId, current.universityId(), current.accountId());
        if (changed == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only the recipient can decline an active connection request.");
        }
        return connection(connectionId, current);
    }

    @Transactional(readOnly = true)
    List<ConversationResponse> conversations(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT c.id, other.id, other.display_name,
                       (SELECT m.body FROM messages m
                        WHERE m.conversation_id = c.id AND m.deleted_at IS NULL
                        ORDER BY m.sent_at DESC, m.id DESC LIMIT 1),
                       (SELECT m.sent_at FROM messages m
                        WHERE m.conversation_id = c.id AND m.deleted_at IS NULL
                        ORDER BY m.sent_at DESC, m.id DESC LIMIT 1),
                       (SELECT COUNT(*) FROM messages unread
                        WHERE unread.conversation_id = c.id
                          AND unread.sender_id <> ?
                          AND unread.deleted_at IS NULL
                          AND (mine.last_read_at IS NULL OR unread.sent_at > mine.last_read_at))
                FROM conversations c
                JOIN conversation_members mine ON mine.conversation_id = c.id AND mine.user_id = ?
                JOIN conversation_members theirs ON theirs.conversation_id = c.id AND theirs.user_id <> ?
                JOIN user_accounts other ON other.id = theirs.user_id AND other.university_id = c.university_id
                WHERE c.university_id = ? AND c.conversation_type = 'DIRECT'
                  AND EXISTS (
                    SELECT 1 FROM connections accepted
                    WHERE accepted.university_id = c.university_id
                      AND accepted.pair_key = c.direct_pair_key
                      AND accepted.status = 'ACCEPTED'
                  )
                ORDER BY COALESCE(
                    (SELECT MAX(m.sent_at) FROM messages m WHERE m.conversation_id = c.id), c.created_at
                ) DESC
                """, (result, row) -> conversationResponse(
                        result.getObject(1, UUID.class),
                        result.getObject(2, UUID.class),
                        result.getString(3),
                        result.getString(4),
                        result.getObject(5, OffsetDateTime.class),
                        result.getLong(6)),
                current.accountId(), current.accountId(), current.accountId(), current.universityId());
    }

    @Transactional
    ConversationResponse openDirectConversation(Authentication authentication, UUID targetAccountId) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireVisibleUniversityAccount(current, targetAccountId);
        if (current.accountId().equals(targetAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot message your own account.");
        }

        String pairKey = pairKey(current.accountId(), targetAccountId);
        Integer connected = jdbc.queryForObject("""
                SELECT COUNT(*) FROM connections
                WHERE university_id = ? AND pair_key = ? AND status = 'ACCEPTED'
                """, Integer.class, current.universityId(), pairKey);
        if (connected == null || connected == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Private messaging requires an accepted connection.");
        }

        List<UUID> existing = jdbc.query(
                "SELECT id FROM conversations WHERE university_id = ? AND direct_pair_key = ?",
                (result, row) -> result.getObject(1, UUID.class), current.universityId(), pairKey);
        UUID conversationId;
        if (!existing.isEmpty()) {
            conversationId = existing.getFirst();
        } else {
            conversationId = UUID.randomUUID();
            try {
                jdbc.update("""
                        INSERT INTO conversations
                          (id, university_id, conversation_type, created_by, direct_pair_key, created_at)
                        VALUES (?, ?, 'DIRECT', ?, ?, CURRENT_TIMESTAMP)
                        """, conversationId, current.universityId(), current.accountId(), pairKey);
                jdbc.update("INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)",
                        conversationId, current.accountId());
                jdbc.update("INSERT INTO conversation_members (conversation_id, user_id) VALUES (?, ?)",
                        conversationId, targetAccountId);
            } catch (DataIntegrityViolationException error) {
                List<UUID> winner = jdbc.query(
                        "SELECT id FROM conversations WHERE university_id = ? AND direct_pair_key = ?",
                        (result, row) -> result.getObject(1, UUID.class), current.universityId(), pairKey);
                if (winner.isEmpty()) throw error;
                conversationId = winner.getFirst();
            }
        }
        return conversationSummary(conversationId, current);
    }

    @Transactional
    ConversationMessagesResponse messages(Authentication authentication, UUID conversationId) {
        SignedInAccount current = currentIdentity.require(authentication);
        ConversationResponse conversation = conversationSummary(conversationId, current);
        List<MessageResponse> messages = jdbc.query("""
                SELECT m.id, m.sender_id, sender.display_name, m.body, m.sent_at
                FROM messages m
                JOIN user_accounts sender ON sender.id = m.sender_id
                WHERE m.conversation_id = ? AND m.university_id = ? AND m.deleted_at IS NULL
                ORDER BY m.sent_at, m.id
                """, (result, row) -> new MessageResponse(
                        result.getObject(1, UUID.class),
                        result.getObject(2, UUID.class),
                        result.getString(3),
                        result.getString(4),
                        result.getObject(5, OffsetDateTime.class),
                        current.accountId().equals(result.getObject(2, UUID.class))),
                conversationId, current.universityId());
        jdbc.update("""
                UPDATE conversation_members SET last_read_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ? AND user_id = ?
                """, conversationId, current.accountId());
        return new ConversationMessagesResponse(conversation, messages);
    }

    @Transactional
    MessageResponse send(Authentication authentication, UUID conversationId, String body) {
        SignedInAccount current = currentIdentity.require(authentication);
        conversationSummary(conversationId, current);
        String cleanBody = body == null ? "" : body.trim();
        if (cleanBody.isBlank() || cleanBody.length() > 4000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A message must contain between 1 and 4000 characters.");
        }

        UUID messageId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO messages
                  (id, university_id, conversation_id, sender_id, body, sent_at)
                VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """, messageId, current.universityId(), conversationId, current.accountId(), cleanBody);
        jdbc.update("""
                UPDATE conversation_members SET last_read_at = CURRENT_TIMESTAMP
                WHERE conversation_id = ? AND user_id = ?
                """, conversationId, current.accountId());
        MessageResponse response = jdbc.queryForObject("""
                SELECT m.id, m.sender_id, sender.display_name, m.body, m.sent_at
                FROM messages m JOIN user_accounts sender ON sender.id = m.sender_id
                WHERE m.id = ?
                """, (result, row) -> new MessageResponse(
                        result.getObject(1, UUID.class), result.getObject(2, UUID.class),
                        result.getString(3), result.getString(4),
                        result.getObject(5, OffsetDateTime.class), true), messageId);
        UUID recipientId = otherConversationMember(conversationId, current.accountId());
        createNotification(current.universityId(), recipientId, "MESSAGE",
                "New message from " + displayName(current.accountId()), cleanBody,
                "CONVERSATION", conversationId);
        publishAfterCommit(recipientId, "MESSAGE_RECEIVED", conversationId);
        publishAfterCommit(current.accountId(), "MESSAGE_SENT", conversationId);
        return response;
    }

    @Transactional(readOnly = true)
    List<NotificationResponse> notifications(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        return jdbc.query("""
                SELECT id, notification_type, title, body, resource_type, resource_id, created_at, read_at
                FROM notifications
                WHERE university_id = ? AND recipient_id = ?
                ORDER BY created_at DESC, id DESC
                LIMIT 40
                """, (result, row) -> new NotificationResponse(
                        result.getObject(1, UUID.class), result.getString(2), result.getString(3),
                        result.getString(4), result.getString(5), result.getObject(6, UUID.class),
                        result.getObject(7, OffsetDateTime.class), result.getObject(8) != null),
                current.universityId(), current.accountId());
    }

    @Transactional
    void markNotificationsRead(Authentication authentication) {
        SignedInAccount current = currentIdentity.require(authentication);
        jdbc.update("""
                UPDATE notifications SET read_at = CURRENT_TIMESTAMP
                WHERE university_id = ? AND recipient_id = ? AND read_at IS NULL
                """, current.universityId(), current.accountId());
        publishAfterCommit(current.accountId(), "NOTIFICATIONS_CHANGED", null);
    }

    @Transactional
    SafetyActionResponse block(Authentication authentication, UUID targetAccountId) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireUniversityAccount(current, targetAccountId);
        if (current.accountId().equals(targetAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot block your own account.");
        }
        String pairKey = pairKey(current.accountId(), targetAccountId);
        int changed = jdbc.update("""
                UPDATE connections
                SET status = 'BLOCKED', blocked_by_id = ?, responded_at = CURRENT_TIMESTAMP
                WHERE university_id = ? AND pair_key = ?
                """, current.accountId(), current.universityId(), pairKey);
        if (changed == 0) {
            jdbc.update("""
                    INSERT INTO connections
                      (id, university_id, requester_id, addressee_id, status, pair_key, blocked_by_id,
                       created_at, responded_at)
                    VALUES (?, ?, ?, ?, 'BLOCKED', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, UUID.randomUUID(), current.universityId(), current.accountId(), targetAccountId,
                    pairKey, current.accountId());
        }
        createNotification(current.universityId(), current.accountId(), "SAFETY",
                "Account blocked", "You will no longer receive connections or messages from "
                        + displayName(targetAccountId) + ".", "ACCOUNT", targetAccountId);
        publishAfterCommit(current.accountId(), "CONNECTIONS_CHANGED", targetAccountId);
        publishAfterCommit(targetAccountId, "CONNECTIONS_CHANGED", current.accountId());
        return new SafetyActionResponse(targetAccountId, displayName(targetAccountId), "BLOCKED", null);
    }

    @Transactional
    SafetyActionResponse report(
            Authentication authentication,
            UUID targetAccountId,
            String reason,
            String details) {
        SignedInAccount current = currentIdentity.require(authentication);
        requireUniversityAccount(current, targetAccountId);
        if (current.accountId().equals(targetAccountId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot report your own account.");
        }
        String cleanReason = reason == null ? "" : reason.trim().toUpperCase(Locale.ROOT);
        if (!List.of("HARASSMENT", "SPAM", "IMPERSONATION", "UNSAFE_BEHAVIOUR", "OTHER")
                .contains(cleanReason)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid reporting reason.");
        }
        String cleanDetails = details == null ? "" : details.trim();
        if (cleanDetails.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Report details must be 1000 characters or fewer.");
        }
        UUID reportId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO safety_reports
                  (id, university_id, reporter_id, reported_account_id, reason, details)
                VALUES (?, ?, ?, ?, ?, ?)
                """, reportId, current.universityId(), current.accountId(), targetAccountId,
                cleanReason, cleanDetails.isBlank() ? null : cleanDetails);
        createNotification(current.universityId(), current.accountId(), "SAFETY",
                "Report received", "Your confidential report has been recorded for safeguarding review.",
                "REPORT", reportId);
        publishAfterCommit(current.accountId(), "NOTIFICATIONS_CHANGED", reportId);
        return new SafetyActionResponse(targetAccountId, displayName(targetAccountId), "REPORTED", reportId);
    }

    private ConnectionResponse connection(UUID connectionId, SignedInAccount current) {
        return jdbc.query("""
                SELECT c.id,
                       CASE WHEN c.requester_id = ? THEN c.addressee_id ELSE c.requester_id END,
                       other.display_name,
                       c.status,
                       CASE WHEN c.requester_id = ? THEN 'OUTGOING' ELSE 'INCOMING' END
                FROM connections c
                JOIN user_accounts other ON other.id =
                    CASE WHEN c.requester_id = ? THEN c.addressee_id ELSE c.requester_id END
                WHERE c.id = ? AND c.university_id = ?
                  AND (c.requester_id = ? OR c.addressee_id = ?)
                """, (result, row) -> new ConnectionResponse(
                        result.getObject(1, UUID.class), result.getObject(2, UUID.class),
                        result.getString(3), result.getString(4), result.getString(5)),
                current.accountId(), current.accountId(), current.accountId(), connectionId,
                current.universityId(), current.accountId(), current.accountId())
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The connection could not be found."));
    }

    private ConversationResponse conversationSummary(UUID conversationId, SignedInAccount current) {
        return jdbc.query("""
                SELECT c.id, other.id, other.display_name,
                       (SELECT m.body FROM messages m
                        WHERE m.conversation_id = c.id AND m.deleted_at IS NULL
                        ORDER BY m.sent_at DESC, m.id DESC LIMIT 1),
                       (SELECT m.sent_at FROM messages m
                        WHERE m.conversation_id = c.id AND m.deleted_at IS NULL
                        ORDER BY m.sent_at DESC, m.id DESC LIMIT 1),
                       (SELECT COUNT(*) FROM messages unread
                        WHERE unread.conversation_id = c.id
                          AND unread.sender_id <> ?
                          AND unread.deleted_at IS NULL
                          AND (mine.last_read_at IS NULL OR unread.sent_at > mine.last_read_at))
                FROM conversations c
                JOIN conversation_members mine ON mine.conversation_id = c.id AND mine.user_id = ?
                JOIN conversation_members theirs ON theirs.conversation_id = c.id AND theirs.user_id <> ?
                JOIN user_accounts other ON other.id = theirs.user_id AND other.university_id = c.university_id
                WHERE c.id = ? AND c.university_id = ? AND c.conversation_type = 'DIRECT'
                  AND EXISTS (
                    SELECT 1 FROM connections accepted
                    WHERE accepted.university_id = c.university_id
                      AND accepted.pair_key = c.direct_pair_key
                      AND accepted.status = 'ACCEPTED'
                  )
                """, (result, row) -> conversationResponse(
                        result.getObject(1, UUID.class), result.getObject(2, UUID.class),
                        result.getString(3), result.getString(4),
                        result.getObject(5, OffsetDateTime.class), result.getLong(6)),
                current.accountId(), current.accountId(), current.accountId(), conversationId,
                current.universityId())
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The conversation is unavailable or you are not a member."));
    }

    private void requireVisibleUniversityAccount(SignedInAccount current, UUID targetAccountId) {
        Integer visible = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_accounts
                WHERE id = ? AND university_id = ? AND profile_visibility <> 'PRIVATE'
                """, Integer.class, targetAccountId, current.universityId());
        if (visible == null || visible == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "That university account is unavailable.");
        }
    }

    private void requireUniversityAccount(SignedInAccount current, UUID targetAccountId) {
        Integer exists = jdbc.queryForObject("""
                SELECT COUNT(*) FROM user_accounts WHERE id = ? AND university_id = ?
                """, Integer.class, targetAccountId, current.universityId());
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "That university account is unavailable.");
        }
    }

    private ConversationResponse conversationResponse(
            UUID conversationId,
            UUID participantAccountId,
            String participantDisplayName,
            String lastMessage,
            OffsetDateTime lastMessageAt,
            long unreadCount) {
        return new ConversationResponse(
                conversationId,
                participantAccountId,
                participantDisplayName,
                initials(participantDisplayName),
                lastMessage == null ? "No messages yet" : lastMessage,
                lastMessageAt,
                unreadCount);
    }

    private UUID otherConversationMember(UUID conversationId, UUID currentAccountId) {
        return jdbc.query("""
                SELECT user_id FROM conversation_members
                WHERE conversation_id = ? AND user_id <> ?
                """, (result, row) -> result.getObject(1, UUID.class), conversationId, currentAccountId)
                .stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "The conversation participant is unavailable."));
    }

    private String displayName(UUID accountId) {
        return jdbc.queryForObject("SELECT display_name FROM user_accounts WHERE id = ?", String.class, accountId);
    }

    private void createNotification(
            UUID universityId,
            UUID recipientId,
            String type,
            String title,
            String body,
            String resourceType,
            UUID resourceId) {
        jdbc.update("""
                INSERT INTO notifications
                  (id, university_id, recipient_id, notification_type, title, body, resource_type, resource_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), universityId, recipientId, type, title, body, resourceType, resourceId);
    }

    private void publishAfterCommit(UUID accountId, String type, UUID resourceId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            realtime.publish(accountId, type, resourceId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtime.publish(accountId, type, resourceId);
            }
        });
    }

    private String initials(String displayName) {
        String[] parts = displayName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (int index = 0; index < Math.min(parts.length, 2); index++) {
            if (!parts[index].isBlank()) initials.append(parts[index].substring(0, 1).toUpperCase(Locale.ROOT));
        }
        return initials.isEmpty() ? "U" : initials.toString();
    }

    static String pairKey(UUID first, UUID second) {
        String left = first.toString();
        String right = second.toString();
        return left.compareTo(right) < 0 ? left + ":" + right : right + ":" + left;
    }
}
