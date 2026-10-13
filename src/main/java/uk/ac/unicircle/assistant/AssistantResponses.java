package uk.ac.unicircle.assistant;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

final class AssistantResponses {
    private AssistantResponses() {
    }

    record Source(String title, String url) {
    }

    record Answer(
            String answer,
            List<Source> sources,
            String imageDataUrl,
            AssistantMode mode,
            boolean live,
            String notice) {
    }

    record Status(boolean enabled, String level, List<AssistantMode> modes, String message) {
    }

    record ConversationSummary(
            UUID id,
            String title,
            AssistantMode mode,
            String preview,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    record ConversationMessage(
            UUID id,
            String role,
            String content,
            AssistantMode mode,
            String imageDataUrl,
            List<Source> sources,
            String notice,
            OffsetDateTime createdAt) {
    }

    record ConversationDetail(
            ConversationSummary conversation,
            List<ConversationMessage> messages) {
    }
}
