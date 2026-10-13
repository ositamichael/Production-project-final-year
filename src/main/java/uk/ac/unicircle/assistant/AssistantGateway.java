package uk.ac.unicircle.assistant;

interface AssistantGateway {
    boolean enabled();

    boolean advancedEnabled();

    AssistantResponses.Answer answer(AssistantMode mode, String message);
}
