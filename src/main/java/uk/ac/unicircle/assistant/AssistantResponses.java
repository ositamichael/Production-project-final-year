package uk.ac.unicircle.assistant;

import java.util.List;

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
}
