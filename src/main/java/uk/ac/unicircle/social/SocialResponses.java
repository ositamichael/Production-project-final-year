package uk.ac.unicircle.social;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class SocialResponses {

    private SocialResponses() {
    }

    record ConnectionResponse(
            UUID connectionId,
            UUID accountId,
            String displayName,
            String status,
            String direction) {
    }

    record StudentDirectoryResponse(
            UUID accountId,
            String displayName,
            String courseName,
            Integer yearOfStudy,
            OffsetDateTime joinedAt,
            UUID connectionId,
            String connectionStatus,
            String connectionDirection) {
    }

    record ConversationResponse(
            UUID conversationId,
            UUID participantAccountId,
            String participantDisplayName,
            String participantInitials,
            String lastMessage,
            OffsetDateTime lastMessageAt) {
    }

    record MessageResponse(
            UUID messageId,
            UUID senderAccountId,
            String senderDisplayName,
            String body,
            OffsetDateTime sentAt,
            boolean sentByCurrentAccount) {
    }

    record ConversationMessagesResponse(
            ConversationResponse conversation,
            List<MessageResponse> messages) {
    }
}
