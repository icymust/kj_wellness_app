package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.ndl.numbers_dont_lie.ai.cache.AiSessionCache;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureResult;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalTime;
import java.util.*;

/**
 * Service to craft prompts, call Groq, validate JSON schema and store in-session.
 * Supports sequential prompting: STEP 4.1 → STEP 4.2 → STEP 4.3.
 * No persistence; strictly analysis/strategy metadata.
 * 
 * DEBUG ONLY: When app.debug.ai.mock=true, returns hardcoded responses to bypass Groq API.
 */
public class AiStrategyService {
    private static final Logger logger = LoggerFactory.getLogger(AiStrategyService.class);
    
    private final GroqClient groqClient;
    private final AiSessionCache cache;
    
    // DEBUG ONLY: Flag to enable mock AI responses
    @Value("${app.debug.ai.mock:false}")
    private boolean mockMode;

    public AiStrategyService(GroqClient groqClient, AiSessionCache cache) {
        this.groqClient = groqClient;
        this.cache = cache;
    }

    // ========== STEP 4.1: Strategy Analysis ==========
    
    /**
     * STEP 4.1: Generate nutrition strategy from user profile.
     * Stores result in cache for STEP 4.2.
     * 
     * DEBUG ONLY: When mockMode=true, returns hardcoded strategy.
     */
    public AiStrategyResult analyzeStrategy(AiStrategyRequest req) {
        // DEBUG ONLY: Return mock response if flag is enabled
        if (mockMode) {
            logger.warn("[DEBUG MOCK] Returning mock AI strategy (Groq API bypassed)");
            AiStrategyResult mockResult = createMockStrategy(req);
            cache.putStrategyResult(req.getUserId(), mockResult);
            return mockResult;
        }
        
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
     * 
     * DEBUG ONLY: When mockMode=true, returns hardcoded meal structure.
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

        // DEBUG ONLY: Return mock response if flag is enabled
        if (mockMode) {
            logger.warn("[DEBUG MOCK] Returning mock meal structure (Groq API bypassed)");
            AiMealStructureResult mockResult = createMockMealStructure(req);
            cache.putMealStructureResult(req.getUserId(), mockResult);
            return mockResult;
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

    // ========== DEBUG ONLY: Mock Response Generators ==========
    
    /**
     * DEBUG ONLY: Creates hardcoded AI strategy response.
     * Used when app.debug.ai.mock=true to bypass Groq API.
     */
    private AiStrategyResult createMockStrategy(AiStrategyRequest req) {
        AiStrategyResult result = new AiStrategyResult();
        
        // Calculate mock calories based on user profile
        int dailyCalories = calculateMockCalories(req);
        
        result.setStrategyName("Mock High-Protein Moderate Deficit");
        result.setRationale("DEBUG MODE: Hardcoded strategy for " + req.getGoal() + 
                           ". Real AI analysis bypassed to unblock frontend development.");
        
        Map<String, Integer> targetCalories = new HashMap<>();
        targetCalories.put("daily", dailyCalories);
        targetCalories.put("breakfast", (int)(dailyCalories * 0.25));
        targetCalories.put("lunch", (int)(dailyCalories * 0.35));
        targetCalories.put("dinner", (int)(dailyCalories * 0.30));
        targetCalories.put("snack", (int)(dailyCalories * 0.10));
        result.setTargetCalories(targetCalories);
        
        Map<String, Double> macroSplit = new HashMap<>();
        macroSplit.put("protein", 0.30);
        macroSplit.put("carbs", 0.45);
        macroSplit.put("fat", 0.25);
        result.setMacroSplit(macroSplit);
        
        result.setConstraints(Arrays.asList("moderate_deficit", "high_protein"));
        result.setRecommendations(Arrays.asList("increase_fiber", "hydrate_regularly", "track_progress"));
        
        logger.info("[DEBUG MOCK] Generated mock strategy: {} calories/day", dailyCalories);
        return result;
    }
    
    /**
     * DEBUG ONLY: Creates hardcoded meal structure response.
     * Used when app.debug.ai.mock=true to bypass Groq API.
     */
    private AiMealStructureResult createMockMealStructure(AiMealStructureRequest req) {
        AiMealStructureResult result = new AiMealStructureResult();
        
        Map<String, Integer> targetCals = req.getStrategyResult().getTargetCalories();
        int dailyTotal = targetCals.getOrDefault("daily", 2000);
        
        List<AiMealStructureResult.MealSlot> meals = new ArrayList<>();
        
        // Breakfast
        AiMealStructureResult.MealSlot breakfast = new AiMealStructureResult.MealSlot();
        breakfast.setMealType("breakfast");
        breakfast.setIndex(0);
        breakfast.setCalorieTarget(targetCals.getOrDefault("breakfast", 500));
        breakfast.setMacroFocus(Map.of("protein", 0.25, "carbs", 0.50, "fat", 0.25));
        breakfast.setTimingNote("Morning energy boost");
        meals.add(breakfast);
        
        // Lunch
        AiMealStructureResult.MealSlot lunch = new AiMealStructureResult.MealSlot();
        lunch.setMealType("lunch");
        lunch.setIndex(0);
        lunch.setCalorieTarget(targetCals.getOrDefault("lunch", 700));
        lunch.setMacroFocus(Map.of("protein", 0.35, "carbs", 0.40, "fat", 0.25));
        lunch.setTimingNote("Midday sustenance");
        meals.add(lunch);
        
        // Dinner
        AiMealStructureResult.MealSlot dinner = new AiMealStructureResult.MealSlot();
        dinner.setMealType("dinner");
        dinner.setIndex(0);
        dinner.setCalorieTarget(targetCals.getOrDefault("dinner", 600));
        dinner.setMacroFocus(Map.of("protein", 0.40, "carbs", 0.35, "fat", 0.25));
        dinner.setTimingNote("Evening meal");
        meals.add(dinner);
        
        // Snack
        AiMealStructureResult.MealSlot snack = new AiMealStructureResult.MealSlot();
        snack.setMealType("snack");
        snack.setIndex(0);
        snack.setCalorieTarget(targetCals.getOrDefault("snack", 200));
        snack.setMacroFocus(Map.of("protein", 0.30, "carbs", 0.40, "fat", 0.30));
        snack.setTimingNote("Post-workout or afternoon");
        meals.add(snack);
        
        result.setMeals(meals);
        result.setTotalCaloriesDistributed(dailyTotal);
        
        logger.info("[DEBUG MOCK] Generated mock meal structure: {} meals, {} total calories", 
                   meals.size(), dailyTotal);
        return result;
    }
    
    /**
     * DEBUG ONLY: Simple calorie calculation for mock data.
     */
    private int calculateMockCalories(AiStrategyRequest req) {
        // Simple BMR estimation (Mifflin-St Jeor)
        double bmr;
        if ("male".equalsIgnoreCase(req.getSex())) {
            bmr = (10 * req.getWeightKg()) + (6.25 * req.getHeightCm()) - (5 * req.getAge()) + 5;
        } else {
            bmr = (10 * req.getWeightKg()) + (6.25 * req.getHeightCm()) - (5 * req.getAge()) - 161;
        }
        
        // Apply activity multiplier (assume moderate activity)
        double tdee = bmr * 1.55;
        
        // Apply goal modifier
        if (req.getGoal() != null && req.getGoal().contains("loss")) {
            tdee *= 0.85; // 15% deficit
        } else if (req.getGoal() != null && req.getGoal().contains("gain")) {
            tdee *= 1.10; // 10% surplus
        }
        
        return (int) Math.round(tdee / 100) * 100; // Round to nearest 100
    }
}
