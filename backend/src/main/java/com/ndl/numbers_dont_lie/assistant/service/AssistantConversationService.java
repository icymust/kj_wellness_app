package com.ndl.numbers_dont_lie.assistant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.GroqClient;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import com.ndl.numbers_dont_lie.assistant.dto.AssistantChatRequest;
import com.ndl.numbers_dont_lie.assistant.dto.AssistantChatResponse;
import com.ndl.numbers_dont_lie.assistant.dto.AssistantMessageResponse;
import com.ndl.numbers_dont_lie.assistant.entity.AssistantMessage;
import com.ndl.numbers_dont_lie.assistant.entity.AssistantSession;
import com.ndl.numbers_dont_lie.assistant.repository.AssistantMessageRepository;
import com.ndl.numbers_dont_lie.assistant.repository.AssistantSessionRepository;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssistantConversationService {

    private static final int MAX_MESSAGE_LENGTH = 4000;

    private final AssistantSessionRepository sessionRepository;
    private final AssistantMessageRepository messageRepository;
    private final AssistantToolPlanner toolPlanner;
    private final AssistantToolValidator toolValidator;
    private final AssistantToolService toolService;
    private final AssistantPromptService promptService;
    private final AssistantSafetyService safetyService;
    private final AssistantPiiFilterService piiFilterService;
    private final ObjectProvider<GroqClient> groqClientProvider;
    private final ObjectMapper objectMapper;

    public AssistantConversationService(
            AssistantSessionRepository sessionRepository,
            AssistantMessageRepository messageRepository,
            AssistantToolPlanner toolPlanner,
            AssistantToolValidator toolValidator,
            AssistantToolService toolService,
            AssistantPromptService promptService,
            AssistantSafetyService safetyService,
            AssistantPiiFilterService piiFilterService,
            ObjectProvider<GroqClient> groqClientProvider,
            ObjectMapper objectMapper) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.toolPlanner = toolPlanner;
        this.toolValidator = toolValidator;
        this.toolService = toolService;
        this.promptService = promptService;
        this.safetyService = safetyService;
        this.piiFilterService = piiFilterService;
        this.groqClientProvider = groqClientProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AssistantChatResponse chat(UserEntity user, AssistantChatRequest request) {
        validateRequest(request);

        AssistantSession session = loadOrCreateSession(user, request.getSessionId(), request.getMessage());
        saveMessage(session, "user", request.getMessage(), null);

        AssistantChatResponse response = new AssistantChatResponse();
        response.setSessionId(session.getId());

        if (safetyService.isPiiRequest(request.getMessage()) || safetyService.isJailbreakAttempt(request.getMessage())) {
            String blocked = safetyService.piiRefusalResponse();
            response.setAssistantMessage(blocked);
            response.setWarnings(List.of("Sensitive data access is blocked by policy."));
            saveMessage(session, "assistant", blocked, metadataJson(response.getToolCalls(), response.getWarnings()));
            updateSessionState(session, "safety", "{}");
            return response;
        }

        if (safetyService.isMedicalRisk(request.getMessage())) {
            String medical = safetyService.medicalSafetyResponse();
            response.setAssistantMessage(medical);
            response.setWarnings(List.of("Medical safety limitation applied."));
            saveMessage(session, "assistant", medical, metadataJson(response.getToolCalls(), response.getWarnings()));
            updateSessionState(session, "safety", "{}");
            return response;
        }

        List<AssistantToolPlanner.ToolCallSpec> planned = toolPlanner.plan(request.getMessage(), session.getLastTopic());
        List<Map<String, Object>> toolCalls = new ArrayList<>();
        List<Map<String, Object>> toolResults = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (AssistantToolPlanner.ToolCallSpec call : planned) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", call.getName());
            entry.put("args", call.getArgs());
            try {
                toolValidator.validate(call.getName(), call.getArgs());
                Map<String, Object> result = toolService.execute(call.getName(), call.getArgs(), user);
                toolResults.add(Map.of("name", call.getName(), "result", result));
                entry.put("status", "ok");
            } catch (IllegalArgumentException ex) {
                Map<String, Object> error = Map.of("error", ex.getMessage());
                toolResults.add(Map.of("name", call.getName(), "result", error));
                entry.put("status", "validation_error");
                entry.put("error", ex.getMessage());
                warnings.add("Tool " + call.getName() + " validation failed: " + ex.getMessage());
            } catch (Exception ex) {
                Map<String, Object> error = Map.of("error", "Execution failed");
                toolResults.add(Map.of("name", call.getName(), "result", error));
                entry.put("status", "execution_error");
                entry.put("error", ex.getMessage());
                warnings.add("Tool " + call.getName() + " execution failed.");
            }
            toolCalls.add(entry);
        }

        response.setToolCalls(toolCalls);
        response.setWarnings(warnings);

        String historyText = recentHistoryText(session.getId());
        String toolPayload = toJson(toolResults);
        String userName = userName(user);
        String mode = normalizedMode(request.getResponseMode());

        String prompt = promptService.buildUserPrompt(
            userName,
            mode,
            request.getMessage(),
            historyText,
            toolPayload,
            session.getLastTopic(),
            session.getLastEntitiesJson()
        );

        String assistantMessage;
        String lastTopic = session.getLastTopic();
        String entitiesJson = session.getLastEntitiesJson();
        List<String> aiWarnings = new ArrayList<>();

        try {
            GroqClient groqClient = groqClientProvider.getIfAvailable();
            if (groqClient == null) {
                throw new AiClientException("AI service unavailable");
            }
            JsonNode aiResponse = groqClient.callForJson(promptService.systemPrompt(), prompt, "detailed".equals(mode) ? 0.35 : 0.2);
            aiResponse = unwrapNestedAssistantJson(aiResponse);
            assistantMessage = safeText(aiResponse.path("answer").asText());
            if (assistantMessage.isBlank()) {
                assistantMessage = fallbackFromTools(toolResults, userName, mode);
            }
            String topicCandidate = aiResponse.path("lastTopic").asText();
            if (!topicCandidate.isBlank()) {
                lastTopic = topicCandidate;
            }
            JsonNode entitiesNode = aiResponse.path("entities");
            if (!entitiesNode.isMissingNode() && !entitiesNode.isNull()) {
                entitiesJson = entitiesNode.toString();
            }
            JsonNode warningsNode = aiResponse.path("warnings");
            if (warningsNode.isArray()) {
                warningsNode.forEach(w -> {
                    if (!w.asText().isBlank()) aiWarnings.add(w.asText());
                });
            }
        } catch (Exception ex) {
            assistantMessage = fallbackFromTools(toolResults, userName, mode);
            aiWarnings.add("AI generation fallback was used.");
            if (!(ex instanceof AiClientException)) {
                aiWarnings.add("Reason: " + ex.getClass().getSimpleName());
            }
        }

        if (!aiWarnings.isEmpty()) {
            List<String> merged = new ArrayList<>(warnings);
            merged.addAll(aiWarnings);
            response.setWarnings(merged);
        }

        assistantMessage = piiFilterService.sanitizeOutput(assistantMessage);
        response.setAssistantMessage(assistantMessage);

        saveMessage(session, "assistant", assistantMessage, metadataJson(toolCalls, response.getWarnings()));
        updateSessionState(session, lastTopic == null || lastTopic.isBlank() ? inferTopicFromTools(toolCalls) : lastTopic, entitiesJson);

        return response;
    }

    @Transactional
    public Long createSession(UserEntity user, String title) {
        AssistantSession session = new AssistantSession();
        session.setUser(user);
        session.setTitle(title == null || title.isBlank() ? "New conversation" : title.trim());
        session = sessionRepository.save(session);
        return session.getId();
    }

    @Transactional(readOnly = true)
    public List<AssistantMessageResponse> getMessages(UserEntity user, Long sessionId) {
        AssistantSession session = sessionRepository.findByIdAndUserId(sessionId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        return messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId()).stream()
            .map(m -> new AssistantMessageResponse(m.getRole(), m.getContent(), m.getCreatedAt()))
            .collect(Collectors.toList());
    }

    private void validateRequest(AssistantChatRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        if (request.getMessage().length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("message is too long (max " + MAX_MESSAGE_LENGTH + " characters)");
        }
    }

    private AssistantSession loadOrCreateSession(UserEntity user, Long sessionId, String firstMessage) {
        if (sessionId != null) {
            Optional<AssistantSession> existing = sessionRepository.findByIdAndUserId(sessionId, user.getId());
            if (existing.isPresent()) {
                return existing.get();
            }
            throw new IllegalArgumentException("Session not found");
        }

        AssistantSession session = new AssistantSession();
        session.setUser(user);
        session.setTitle(shortTitle(firstMessage));
        return sessionRepository.save(session);
    }

    private void saveMessage(AssistantSession session, String role, String content, String metadataJson) {
        AssistantMessage message = new AssistantMessage();
        message.setSession(session);
        message.setRole(role);
        message.setContent(content);
        message.setMetadataJson(metadataJson);
        messageRepository.save(message);
    }

    private void updateSessionState(AssistantSession session, String topic, String entitiesJson) {
        session.setLastTopic(topic);
        session.setLastEntitiesJson(entitiesJson == null ? "{}" : entitiesJson);
        sessionRepository.save(session);
    }

    private String recentHistoryText(Long sessionId) {
        List<AssistantMessage> recent = messageRepository.findRecentBySessionId(sessionId, PageRequest.of(0, 10));
        List<AssistantMessage> ordered = new ArrayList<>(recent);
        ordered.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));

        StringBuilder sb = new StringBuilder();
        for (AssistantMessage item : ordered) {
            sb.append("[").append(item.getRole()).append("] ")
                .append(item.getContent())
                .append("\n");
        }
        return sb.toString();
    }

    private String fallbackFromTools(List<Map<String, Object>> toolResults, String userName, String mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(userName).append(", ");
        sb.append("here is what I found from your current data:\n");

        for (Map<String, Object> row : toolResults) {
            sb.append("- ").append(row.get("name")).append(": ");
            sb.append(toJson(row.get("result"))).append("\n");
        }

        if ("concise".equals(mode)) {
            return sb.toString();
        }
        sb.append("\nIf you want, ask a follow-up like 'tell me more about protein this week' and I will refine this further.");
        return sb.toString();
    }

    private JsonNode unwrapNestedAssistantJson(JsonNode response) {
        if (response == null || response.isMissingNode()) {
            return response;
        }
        JsonNode answerNode = response.path("answer");
        if (!answerNode.isTextual()) {
            return response;
        }

        String raw = safeText(answerNode.asText());
        if (raw.isBlank()) {
            return response;
        }

        String cleaned = raw
            .replaceFirst("(?s)^\\s*```(?:json)?\\s*", "")
            .replaceFirst("(?s)\\s*```\\s*$", "")
            .trim();

        if (!cleaned.startsWith("{")) {
            return response;
        }

        try {
            JsonNode nested = objectMapper.readTree(cleaned);
            if (nested.has("answer")) {
                return nested;
            }
        } catch (JsonProcessingException ignored) {
            // keep original response
        }
        return response;
    }

    private String metadataJson(List<Map<String, Object>> toolCalls, List<String> warnings) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("toolCalls", toolCalls == null ? List.of() : toolCalls);
        metadata.put("warnings", warnings == null ? List.of() : warnings);
        return toJson(metadata);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String shortTitle(String message) {
        String clean = message == null ? "New conversation" : message.trim();
        if (clean.length() <= 60) {
            return clean;
        }
        return clean.substring(0, 60) + "...";
    }

    private String inferTopicFromTools(List<Map<String, Object>> toolCalls) {
        if (toolCalls == null || toolCalls.isEmpty()) {
            return "general";
        }
        String first = String.valueOf(toolCalls.get(0).get("name")).toLowerCase(Locale.ROOT);
        if (first.contains("health")) return "health";
        if (first.contains("goal")) return "progress";
        if (first.contains("meal")) return "meal_plan";
        if (first.contains("recipe")) return "recipe";
        if (first.contains("nutrition")) return "nutrition";
        if (first.contains("trend")) return "trend";
        return "general";
    }

    private String normalizedMode(String mode) {
        if (mode == null) {
            return "concise";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        return "detailed".equals(normalized) ? "detailed" : "concise";
    }

    private String userName(UserEntity user) {
        String email = user.getEmail() == null ? "User" : user.getEmail();
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
