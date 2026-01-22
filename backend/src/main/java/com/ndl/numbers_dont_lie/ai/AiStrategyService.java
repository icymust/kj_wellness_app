package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.ndl.numbers_dont_lie.ai.cache.AiSessionCache;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Service to craft prompt, call Groq, validate JSON schema and store in-session.
 * No persistence; strictly analysis/strategy metadata.
 */
public class AiStrategyService {
    private final GroqClient groqClient;
    private final AiSessionCache cache;

    public AiStrategyService(GroqClient groqClient, AiSessionCache cache) {
        this.groqClient = groqClient;
        this.cache = cache;
    }

    public AiStrategyResult analyze(AiStrategyRequest req) {
        String prompt = buildPrompt(req);
        JsonNode json = groqClient.callForStrategyJson(req, prompt);
        AiStrategyResult result = validateAndMap(json);
        cache.put(req.getUserId(), result);
        return result;
    }

    public AiStrategyResult getCached(String userId) {
        return cache.get(userId);
    }

    private String buildPrompt(AiStrategyRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Given the following normalized user profile inputs, compute a nutrition strategy.\n");
        sb.append("Inputs:\n");
        sb.append("- user_id: ").append(req.getUserId()).append("\n");
        sb.append("- timezone: ").append(req.getTimezone()).append("\n");
        sb.append("- age: ").append(req.getAge()).append("\n");
        sb.append("- sex: ").append(req.getSex()).append("\n");
        sb.append("- height_cm: ").append(req.getHeightCm()).append("\n");
        sb.append("- weight_kg: ").append(req.getWeightKg()).append("\n");
        sb.append("- goal: ").append(req.getGoal()).append("\n");
        sb.append("- dietary_preferences: ").append(req.getDietaryPreferences()).append("\n");
        sb.append("- allergies: ").append(req.getAllergies()).append("\n");
        sb.append("- meal_frequency: ").append(req.getMealFrequency()).append("\n\n");

        sb.append("Return STRICTLY valid JSON with this schema, and nothing else. No code fences, no markdown, no extra text.\n");
        sb.append("Schema (keys + types):\n");
        sb.append("{\n");
        sb.append("  \"strategyName\": string,\n");
        sb.append("  \"rationale\": string,\n");
        sb.append("  \"targetCalories\": { \"daily\": int, \"breakfast\": int, \"lunch\": int, \"dinner\": int, \"snack\": int },\n");
        sb.append("  \"macroSplit\": { \"protein\": number, \"carbs\": number, \"fat\": number },\n");
        sb.append("  \"constraints\": string[],\n");
        sb.append("  \"recommendations\": string[]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private AiStrategyResult validateAndMap(JsonNode json) {
        // Minimal schema validation
        if (!json.hasNonNull("strategyName") || !json.get("strategyName").isTextual())
            throw new AiClientException("Invalid JSON: missing strategyName");
        if (!json.hasNonNull("rationale") || !json.get("rationale").isTextual())
            throw new AiClientException("Invalid JSON: missing rationale");

        JsonNode targetCalories = json.get("targetCalories");
        if (targetCalories == null || !targetCalories.isObject())
            throw new AiClientException("Invalid JSON: targetCalories must be object");

        JsonNode macroSplit = json.get("macroSplit");
        if (macroSplit == null || !macroSplit.isObject())
            throw new AiClientException("Invalid JSON: macroSplit must be object");

        AiStrategyResult r = new AiStrategyResult();
        r.setStrategyName(json.get("strategyName").asText());
        r.setRationale(json.get("rationale").asText());

        Map<String, Integer> calories = new HashMap<>();
        targetCalories.fields().forEachRemaining(e -> {
            if (e.getValue().isInt()) {
                calories.put(e.getKey(), e.getValue().asInt());
            }
        });
        r.setTargetCalories(calories);

        Map<String, Double> macros = new HashMap<>();
        macroSplit.fields().forEachRemaining(e -> {
            if (e.getValue().isNumber()) {
                macros.put(e.getKey(), e.getValue().asDouble());
            }
        });
        r.setMacroSplit(macros);

        if (json.has("constraints") && json.get("constraints").isArray()) {
            var arr = json.get("constraints");
            java.util.List<String> list = new java.util.ArrayList<>();
            arr.forEach(n -> { if (n.isTextual()) list.add(n.asText()); });
            r.setConstraints(list);
        }
        if (json.has("recommendations") && json.get("recommendations").isArray()) {
            var arr = json.get("recommendations");
            java.util.List<String> list = new java.util.ArrayList<>();
            arr.forEach(n -> { if (n.isTextual()) list.add(n.asText()); });
            r.setRecommendations(list);
        }
        return r;
    }
}
