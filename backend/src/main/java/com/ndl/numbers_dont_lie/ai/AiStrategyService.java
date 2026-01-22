package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.ndl.numbers_dont_lie.ai.cache.AiSessionCache;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureResult;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;

import java.time.LocalTime;
import java.util.*;

/**
 * Service to craft prompts, call Groq, validate JSON schema and store in-session.
 * Supports sequential prompting: STEP 4.1 → STEP 4.2 → STEP 4.3.
 * No persistence; strictly analysis/strategy metadata.
 */
public class AiStrategyService {
    private final GroqClient groqClient;
    private final AiSessionCache cache;

    public AiStrategyService(GroqClient groqClient, AiSessionCache cache) {
        this.groqClient = groqClient;
        this.cache = cache;
    }

    // ========== STEP 4.1: Strategy Analysis ==========
    
    /**
     * STEP 4.1: Generate nutrition strategy from user profile.
     * Stores result in cache for STEP 4.2.
     */
    public AiStrategyResult analyzeStrategy(AiStrategyRequest req) {
        String prompt = buildStrategyPrompt(req);
        JsonNode json = groqClient.callForJson(prompt);
        AiStrategyResult result = validateAndMapStrategy(json);
        cache.putStrategyResult(req.getUserId(), result);
        return result;
    }

    public AiStrategyResult getCachedStrategy(String userId) {
        return cache.getStrategyResult(userId);
    }

    // ========== STEP 4.2: Meal Structure Distribution ==========
    
    /**
     * STEP 4.2: Distribute daily calories across meal slots.
     * Depends on STEP 4.1 (AiStrategyResult must be cached).
     * Stores result in cache for STEP 4.3.
     * 
     * DOES NOT generate recipes or name dishes.
     */
    public AiMealStructureResult analyzeMealStructure(AiMealStructureRequest req) {
        // Enforce sequential flow: STEP 4.1 must complete first
        if (req.getStrategyResult() == null) {
            AiStrategyResult cached = cache.getStrategyResult(req.getUserId());
            if (cached == null) {
                throw new AiClientException(
                    "Cannot analyze meal structure: STEP 4.1 (strategy) not completed. " +
                    "Call analyzeStrategy() first."
                );
            }
            req.setStrategyResult(cached);
        }

        String prompt = buildMealStructurePrompt(req);
        JsonNode json = groqClient.callForJson(prompt);
        AiMealStructureResult result = validateAndMapMealStructure(json);
        cache.putMealStructureResult(req.getUserId(), result);
        return result;
    }

    public AiMealStructureResult getCachedMealStructure(String userId) {
        return cache.getMealStructureResult(userId);
    }

    // ========== Private: STEP 4.1 Prompt Building ==========

    private String buildStrategyPrompt(AiStrategyRequest req) {
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

    private AiStrategyResult validateAndMapStrategy(JsonNode json) {
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

    // ========== Private: STEP 4.2 Prompt Building ==========

    private String buildMealStructurePrompt(AiMealStructureRequest req) {
        AiStrategyResult strategy = req.getStrategyResult();
        StringBuilder sb = new StringBuilder();
        
        sb.append("STEP 4.2: Meal Structure Distribution\n\n");
        sb.append("You are given a nutrition strategy (STEP 4.1) and meal frequency requirements.\n");
        sb.append("Your task: distribute daily calories across all meal slots.\n\n");
        
        sb.append("=== INPUT: Strategy from STEP 4.1 ===\n");
        sb.append("Strategy Name: ").append(strategy.getStrategyName()).append("\n");
        sb.append("Daily Calorie Target: ").append(req.getDailyCalorieTarget()).append(" kcal\n");
        sb.append("Macro Split: ").append(strategy.getMacroSplit()).append("\n");
        sb.append("Constraints: ").append(strategy.getConstraints()).append("\n\n");
        
        sb.append("=== INPUT: Meal Frequency ===\n");
        req.getMealFrequency().forEach((type, count) -> 
            sb.append("- ").append(type).append(": ").append(count).append("\n")
        );
        sb.append("\n");
        
        if (req.getMealTimingPreferences() != null && !req.getMealTimingPreferences().isEmpty()) {
            sb.append("=== INPUT: Timing Preferences ===\n");
            req.getMealTimingPreferences().forEach((type, time) -> 
                sb.append("- ").append(type).append(": ").append(time).append("\n")
            );
            sb.append("\n");
        }
        
        sb.append("=== STRICT REQUIREMENTS ===\n");
        sb.append("1. DO NOT generate recipes\n");
        sb.append("2. DO NOT name specific dishes\n");
        sb.append("3. DO NOT mention ingredients\n");
        sb.append("4. ONLY distribute calories and macro focus across meal slots\n");
        sb.append("5. Respect the exact meal frequency counts\n");
        sb.append("6. For multiple snacks, use index 0, 1, 2, etc.\n\n");
        
        sb.append("=== OUTPUT FORMAT (STRICT JSON) ===\n");
        sb.append("Return ONLY valid JSON matching this schema. No markdown, no code fences.\n");
        sb.append("{\n");
        sb.append("  \"meals\": [\n");
        sb.append("    {\n");
        sb.append("      \"meal_type\": string,  // breakfast | lunch | dinner | snack\n");
        sb.append("      \"index\": int,         // 0 for main meals, 0-N for multiple snacks\n");
        sb.append("      \"calorie_target\": int,\n");
        sb.append("      \"macro_focus\": { \"protein\": number, \"carbs\": number, \"fat\": number },\n");
        sb.append("      \"timing_note\": string // e.g. \"Pre-workout\", \"Mid-morning\", optional context\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"total_calories_distributed\": int\n");
        sb.append("}\n");
        
        return sb.toString();
    }

    private AiMealStructureResult validateAndMapMealStructure(JsonNode json) {
        if (!json.has("meals") || !json.get("meals").isArray()) {
            throw new AiClientException("Invalid JSON: meals array missing or not an array");
        }
        if (!json.has("total_calories_distributed") || !json.get("total_calories_distributed").isInt()) {
            throw new AiClientException("Invalid JSON: total_calories_distributed missing or not an integer");
        }

        JsonNode mealsArray = json.get("meals");
        if (mealsArray.size() == 0) {
            throw new AiClientException("Invalid JSON: meals array is empty");
        }

        List<AiMealStructureResult.MealSlot> slots = new ArrayList<>();
        for (JsonNode node : mealsArray) {
            if (!node.has("meal_type") || !node.get("meal_type").isTextual()) {
                throw new AiClientException("Invalid JSON: meal_type missing or not text");
            }
            if (!node.has("index") || !node.get("index").isInt()) {
                throw new AiClientException("Invalid JSON: index missing or not integer");
            }
            if (!node.has("calorie_target") || !node.get("calorie_target").isInt()) {
                throw new AiClientException("Invalid JSON: calorie_target missing or not integer");
            }
            if (!node.has("macro_focus") || !node.get("macro_focus").isObject()) {
                throw new AiClientException("Invalid JSON: macro_focus missing or not object");
            }

            AiMealStructureResult.MealSlot slot = new AiMealStructureResult.MealSlot();
            slot.setMealType(node.get("meal_type").asText());
            slot.setIndex(node.get("index").asInt());
            slot.setCalorieTarget(node.get("calorie_target").asInt());

            Map<String, Double> macroFocus = new HashMap<>();
            JsonNode macroNode = node.get("macro_focus");
            macroNode.fields().forEachRemaining(e -> {
                if (e.getValue().isNumber()) {
                    macroFocus.put(e.getKey(), e.getValue().asDouble());
                }
            });
            slot.setMacroFocus(macroFocus);

            if (node.has("timing_note") && node.get("timing_note").isTextual()) {
                slot.setTimingNote(node.get("timing_note").asText());
            }

            slots.add(slot);
        }

        AiMealStructureResult result = new AiMealStructureResult();
        result.setMeals(slots);
        result.setTotalCaloriesDistributed(json.get("total_calories_distributed").asInt());
        return result;
    }
}
