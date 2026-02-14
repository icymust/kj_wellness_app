package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Groq client wrapper for JSON-only responses with optional function calling.
 * Reads API key from env GROQ_API_KEY. Does not persist results.
 */
public class GroqClient {
    private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
    private static final double DEFAULT_TEMPERATURE = 0.2;
    private static final double DEFAULT_TOP_P = 0.95;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GroqClient() {
        this.apiKey = System.getenv("GROQ_API_KEY");
        if (this.apiKey == null || this.apiKey.isBlank()) {
            // Note: This should never happen if @ConditionalOnProperty works correctly
            // But kept as defensive check
            throw new AiClientException("Groq API key not configured. Set GROQ_API_KEY.");
        }
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Call Groq API with structured prompt for JSON-only response.
     * Generic method supporting all prompt types (strategy, meal structure, etc.).
     */
    public JsonNode callForJson(String prompt) {
        return callForJson(prompt, (List<Map<String, Object>>) null, DEFAULT_TEMPERATURE);
    }

    /**
     * Call Groq API with function calling support.
     * 
     * If AI invokes a function, the response will contain a function_call object.
     * Caller must handle function execution and send result back via callWithFunctionResult.
     * 
     * @param prompt User prompt
     * @param functions List of function definitions (OpenAI format)
     * @return JsonNode containing either message content or function_call
     */
    public JsonNode callForJson(String prompt, List<Map<String, Object>> functions) {
        return callForJson(prompt, functions, DEFAULT_TEMPERATURE);
    }

    public JsonNode callForJson(String prompt, Double temperature) {
        return callForJson(prompt, (List<Map<String, Object>>) null, temperature);
    }

    /**
     * Call Groq API with custom system prompt.
     * This is used by the conversational assistant so prompt policy can be
     * scoped per feature instead of reusing nutrition-only defaults.
     */
    public JsonNode callForJson(String systemPrompt, String prompt, Double temperature) {
        try {
            List<Object> messages = new ArrayList<>();
            messages.add(Map.of(
                "role", "system",
                "content", systemPrompt
            ));
            messages.add(Map.of("role", "user", "content", prompt));

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature != null ? temperature : DEFAULT_TEMPERATURE);
            requestBody.put("top_p", DEFAULT_TOP_P);

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content == null || content.isMissingNode() || !content.isTextual()) {
                throw new AiClientException("Malformed Groq response. No content.");
            }

            String contentText = content.asText();
            return parseAssistantJson(contentText);
        } catch (HttpTimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq request timed out. Please retry.", e);
        } catch (ConnectException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq connectivity error. Check network.", e);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq request failed.", e);
        }
    }

    public JsonNode callForJson(String prompt, List<Map<String, Object>> functions, Double temperature) {
        try {
            List<Object> messages = new ArrayList<>();
            messages.add(Map.of(
                "role", "system", 
                "content", "You are a nutrition assistant. Respond STRICTLY with valid JSON. " +
                          "No markdown, no code fences, no prose outside JSON. " +
                          "When you need nutritional data, call the calculate_nutrition function."
            ));
            messages.add(Map.of("role", "user", "content", prompt));

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("messages", messages);
            requestBody.put("temperature", temperature != null ? temperature : DEFAULT_TEMPERATURE);
            requestBody.put("top_p", DEFAULT_TOP_P);
            
            if (functions != null && !functions.isEmpty()) {
                requestBody.put("functions", functions);
                requestBody.put("function_call", "auto");
            }

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode choice = root.path("choices").path(0);
            JsonNode message = choice.path("message");

            // Check for function call
            if (message.has("function_call")) {
                return message; // Return entire message with function_call
            }

            // Regular content response
            JsonNode content = message.path("content");
            if (content == null || content.isMissingNode() || !content.isTextual()) {
                throw new AiClientException("Malformed Groq response. No content.");
            }
            
            String contentText = content.asText();
            try {
                return objectMapper.readTree(contentText);
            } catch (JsonProcessingException e) {
                throw new AiClientException("Model did not return valid JSON.", e);
            }
        } catch (HttpTimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq request timed out. Please retry.", e);
        } catch (ConnectException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq connectivity error. Check network.", e);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq request failed.", e);
        }
    }

    /**
     * Continue conversation after function execution.
     * 
     * @param originalPrompt Original user prompt
     * @param functionName Name of executed function
     * @param functionResult JSON result from function execution
     * @param functions Function definitions
     * @return Final JSON response from AI
     */
    public JsonNode callWithFunctionResult(
            String originalPrompt,
            String functionName,
            String functionResult,
            List<Map<String, Object>> functions) {
        
        try {
            List<Object> messages = new ArrayList<>();
            messages.add(Map.of(
                "role", "system",
                "content", "You are a nutrition assistant. Use function results to complete the recipe JSON."
            ));
            messages.add(Map.of("role", "user", "content", originalPrompt));
            messages.add(Map.of(
                "role", "assistant",
                "content", "",
                "function_call", Map.of(
                    "name", functionName,
                    "arguments", "{}" // Simplified
                )
            ));
            messages.add(Map.of(
                "role", "function",
                "name", functionName,
                "content", functionResult
            ));

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("messages", messages);
            requestBody.put("temperature", DEFAULT_TEMPERATURE);
            requestBody.put("top_p", DEFAULT_TOP_P);

            String body = objectMapper.writeValueAsString(requestBody);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            handleHttpErrors(response);

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            
            if (content == null || content.isMissingNode() || !content.isTextual()) {
                throw new AiClientException("Malformed Groq response after function call.");
            }

            String contentText = content.asText();
            try {
                return objectMapper.readTree(contentText);
            } catch (JsonProcessingException e) {
                throw new AiClientException("Model did not return valid JSON after function.", e);
            }
        } catch (HttpTimeoutException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq request timed out. Please retry.", e);
        } catch (ConnectException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq connectivity error. Check network.", e);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiClientException("Groq function result request failed.", e);
        }
    }

    private void handleHttpErrors(HttpResponse<String> response) {
        String body = response.body();
        if (response.statusCode() == 401) {
            throw new AiClientException("Groq authentication failed. Check GROQ_API_KEY. " + body);
        }
        if (response.statusCode() == 429) {
            throw new AiClientException("Groq rate limit reached. Please retry later. " + body);
        }
        if (response.statusCode() >= 500) {
            throw new AiClientException("Groq service unavailable. Please try again later. " + body);
        }
        if (response.statusCode() >= 400) {
            throw new AiClientException("Groq request error: " + response.statusCode() + " " + body);
        }
    }

    private JsonNode parseAssistantJson(String contentText) {
        try {
            return objectMapper.readTree(contentText);
        } catch (JsonProcessingException ignored) {
            // Continue with lenient parsing
        }

        String withoutFences = contentText
                .replaceFirst("(?s)^\\s*```(?:json)?\\s*", "")
                .replaceFirst("(?s)\\s*```\\s*$", "")
                .trim();
        try {
            return objectMapper.readTree(withoutFences);
        } catch (JsonProcessingException ignored) {
            // Continue with object extraction
        }
        try {
            return objectMapper.readTree(escapeNewlinesInsideStrings(withoutFences));
        } catch (JsonProcessingException ignored) {
            // Continue with object extraction
        }

        int firstBrace = withoutFences.indexOf('{');
        int lastBrace = withoutFences.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String objectSlice = withoutFences.substring(firstBrace, lastBrace + 1);
            try {
                return objectMapper.readTree(objectSlice);
            } catch (JsonProcessingException ignored) {
                // Try to repair multiline strings inside JSON-like object
            }
            try {
                return objectMapper.readTree(escapeNewlinesInsideStrings(objectSlice));
            } catch (JsonProcessingException ignored) {
                // Fall through to wrapped answer
            }
        }

        ObjectNode wrapped = objectMapper.createObjectNode();
        wrapped.put(
                "answer",
                withoutFences.isBlank() ? "I could not generate a structured answer." : withoutFences
        );
        wrapped.put("lastTopic", "general");
        wrapped.set("entities", objectMapper.createObjectNode());
        ArrayNode warnings = objectMapper.createArrayNode();
        warnings.add("Model returned non-JSON response; raw answer was used.");
        wrapped.set("warnings", warnings);
        return wrapped;
    }

    private String escapeNewlinesInsideStrings(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"' && !escaped) {
                inString = !inString;
                out.append(c);
                continue;
            }
            if (inString && (c == '\n' || c == '\r')) {
                out.append("\\n");
                escaped = false;
                continue;
            }
            out.append(c);
            if (c == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }
        }
        return out.toString();
    }
}
