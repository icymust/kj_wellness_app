package com.ndl.numbers_dont_lie.assistant.dto;

public class AssistantCreateSessionResponse {

    private Long sessionId;

    public AssistantCreateSessionResponse() {
    }

    public AssistantCreateSessionResponse(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}
