package com.ndl.numbers_dont_lie.assistant.controller;

import com.ndl.numbers_dont_lie.assistant.dto.AssistantChatRequest;
import com.ndl.numbers_dont_lie.assistant.dto.AssistantChatResponse;
import com.ndl.numbers_dont_lie.assistant.dto.AssistantCreateSessionResponse;
import com.ndl.numbers_dont_lie.assistant.dto.AssistantMessageResponse;
import com.ndl.numbers_dont_lie.assistant.service.AssistantAuthService;
import com.ndl.numbers_dont_lie.assistant.service.AssistantConversationService;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantAuthService authService;
    private final AssistantConversationService conversationService;

    public AssistantController(
            AssistantAuthService authService,
            AssistantConversationService conversationService) {
        this.authService = authService;
        this.conversationService = conversationService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AssistantChatRequest request) {
        try {
            UserEntity user = authService.requireUser(authorization);
            AssistantChatResponse response = conversationService.chat(user, request);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sessions")
    public ResponseEntity<?> createSession(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            UserEntity user = authService.requireUser(authorization);
            String title = body != null && body.get("title") != null ? body.get("title").toString() : null;
            Long sessionId = conversationService.createSession(user, title);
            return ResponseEntity.ok(new AssistantCreateSessionResponse(sessionId));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<?> getMessages(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long sessionId) {
        try {
            UserEntity user = authService.requireUser(authorization);
            List<AssistantMessageResponse> messages = conversationService.getMessages(user, sessionId);
            return ResponseEntity.ok(messages);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
