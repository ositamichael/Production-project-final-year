package uk.ac.unicircle.assistant;

interface AssistantGateway {
    boolean enabled();

    AssistantResponses.Answer answer(AssistantMode mode, String message);
}
