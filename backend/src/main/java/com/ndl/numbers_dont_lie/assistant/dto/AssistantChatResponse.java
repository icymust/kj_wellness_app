package com.ndl.numbers_dont_lie.assistant.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssistantChatResponse {

    private Long sessionId;
    private String assistantMessage;
    private List<Map<String, Object>> toolCalls = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private Map<String, Object> error = new HashMap<>();

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAssistantMessage() {
        return assistantMessage;
    }

    public void setAssistantMessage(String assistantMessage) {
        this.assistantMessage = assistantMessage;
    }

    public List<Map<String, Object>> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<Map<String, Object>> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public Map<String, Object> getError() {
        return error;
    }

    public void setError(Map<String, Object> error) {
        this.error = error;
    }
}
