package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyRequest;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Minimal Groq client wrapper for JSON-only responses.
 * Reads API key from env GROQ_API_KEY. Does not persist results.
 */
public class GroqClient {
    private static final String DEFAULT_MODEL = "llama-3.1-70b-versatile"; // can be made configurable
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqClient() {
        this.apiKey = System.getenv("GROQ_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            throw new AiClientException("Groq API key not configured. Set GROQ_API_KEY.");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode callForStrategyJson(AiStrategyRequest request, String prompt) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "model", DEFAULT_MODEL,
                    "messages", new Object[]{
                            Map.of("role", "system", "content", "You are a nutrition strategy assistant. Respond STRICTLY with valid JSON that matches the requested schema. No markdown, no code fences, no prose outside JSON."),
                            Map.of("role", "user", "content", prompt)
                    },
                    "temperature", 0.2
            ));

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 401) {
                throw new AiClientException("Groq authentication failed. Check GROQ_API_KEY.");
            }
            if (response.statusCode() == 429) {
                throw new AiClientException("Groq rate limit reached. Please retry later.");
            }
            if (response.statusCode() >= 500) {
                throw new AiClientException("Groq service unavailable. Please try again later.");
            }
            if (response.statusCode() >= 400) {
                throw new AiClientException("Groq request error: " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content == null || content.isMissingNode() || !content.isTextual()) {
                throw new AiClientException("Malformed Groq response. No content.");
            }
            String contentText = content.asText();
            try {
                return objectMapper.readTree(contentText);
            } catch (JsonProcessingException e) {
                throw new AiClientException("Model did not return valid JSON.", e);
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq request failed.", e);
        }
    }
}
