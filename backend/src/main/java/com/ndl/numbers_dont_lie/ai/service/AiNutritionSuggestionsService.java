package com.ndl.numbers_dont_lie.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.GroqClient;
import com.ndl.numbers_dont_lie.ai.dto.AiNutritionSuggestionsRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiNutritionSuggestionsResponse;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiNutritionSuggestionsService {
    private static final Logger logger = LoggerFactory.getLogger(AiNutritionSuggestionsService.class);
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiNutritionSuggestionsService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public AiNutritionSuggestionsResponse generateSuggestions(AiNutritionSuggestionsRequest request) {
        if (request == null || request.getDate() == null || request.getNutritionSummary() == null) {
            throw new IllegalArgumentException("date and nutritionSummary are required");
        }

        AiNutritionSuggestionsRequest.NutritionSummary n = request.getNutritionSummary();
        double calories = safe(n.getCalories());
        double targetCalories = safe(n.getTargetCalories());
        double protein = safe(n.getProtein());
        double targetProtein = safe(n.getTargetProtein());
        double carbs = safe(n.getCarbs());
        double targetCarbs = safe(n.getTargetCarbs());
        double fats = safe(n.getFats());
        double targetFats = safe(n.getTargetFats());
        boolean estimated = n.getNutritionEstimated() != null && n.getNutritionEstimated();

        String prompt = buildPrompt(
            request,
            calories,
            targetCalories,
            protein,
            targetProtein,
            carbs,
            targetCarbs,
            fats,
            targetFats,
            estimated
        );

        JsonNode response = groqClient.callForJson(prompt, 0.3);
        AiNutritionSuggestionsResponse parsed = parseResponse(response);

        logger.info(
            "[AI_NUTRITION] Generated improvement suggestions for userId={} date={}",
            request.getUserId(),
            request.getDate()
        );

        return parsed;
    }

    private String buildPrompt(
            AiNutritionSuggestionsRequest request,
            double calories,
            double targetCalories,
            double protein,
            double targetProtein,
            double carbs,
            double targetCarbs,
            double fats,
            double targetFats,
            boolean estimated) {
        double calorieDelta = calories - targetCalories;
        String goal = (request.getUserGoal() == null || request.getUserGoal().isBlank())
            ? "general_fitness"
            : request.getUserGoal();

        List<String> mealLines = new ArrayList<>();
        if (request.getMeals() != null) {
            for (var meal : request.getMeals()) {
                String type = meal.getMealType() != null ? meal.getMealType() : "MEAL";
                String name = meal.getName() != null ? meal.getName() : "Meal";
                mealLines.add(type + ": " + name);
            }
        }

        List<String> prefs = request.getDietaryPreferences() != null
            ? request.getDietaryPreferences()
            : List.of();

        StringBuilder sb = new StringBuilder();
        sb.append("Generate 5-6 bullet-point nutrition improvement suggestions for today.\n");
        sb.append("Return STRICT JSON only: { \"suggestions\": [string] }\n");
        sb.append("Rules:\n");
        sb.append("- Use ONLY provided data; do not invent numbers.\n");
        sb.append("- Do NOT mention exact calories for individual meals.\n");
        sb.append("- Do NOT give medical advice or suggest supplements.\n");
        sb.append("- Be supportive and neutral.\n");
        sb.append("- Must include at least one suggestion of EACH type:\n");
        sb.append("  1) food recommendation\n");
        sb.append("  2) meal timing adjustment\n");
        sb.append("  3) portion size modification\n");
        sb.append("  4) alternative ingredient\n");
        sb.append("  5) meal plan optimization\n");
        if (estimated) {
            sb.append("- Include a short disclaimer that values are estimated.\n");
        }
        sb.append("\nExample:\n");
        sb.append("{\"suggestions\":[\n");
        sb.append("  \"Add a colorful vegetable to lunch to boost fiber and micronutrients.\",\n");
        sb.append("  \"If you feel hungry later, shift a small snack closer to the afternoon.\",\n");
        sb.append("  \"Reduce dinner portions slightly to better match your calorie target.\",\n");
        sb.append("  \"Swap mayonnaise for Greek yogurt to lower saturated fat.\",\n");
        sb.append("  \"Consider a lighter evening meal to balance higher calories earlier in the day.\"\n");
        sb.append("]}\n");
        sb.append("\nInputs:\n");
        sb.append("- date: ").append(request.getDate()).append("\n");
        sb.append("- goal: ").append(goal).append("\n");
        sb.append("- calories: ").append(calories).append("\n");
        sb.append("- targetCalories: ").append(targetCalories).append("\n");
        sb.append("- calorieDelta: ").append(calorieDelta).append("\n");
        sb.append("- protein: ").append(protein).append("\n");
        sb.append("- targetProtein: ").append(targetProtein).append("\n");
        sb.append("- carbs: ").append(carbs).append("\n");
        sb.append("- targetCarbs: ").append(targetCarbs).append("\n");
        sb.append("- fats: ").append(fats).append("\n");
        sb.append("- targetFats: ").append(targetFats).append("\n");
        sb.append("- dietaryPreferences: ").append(prefs).append("\n");
        sb.append("- meals: ").append(mealLines).append("\n");
        return sb.toString();
    }

    private AiNutritionSuggestionsResponse parseResponse(JsonNode response) {
        try {
            AiNutritionSuggestionsResponse parsed = objectMapper.convertValue(response, AiNutritionSuggestionsResponse.class);
            if (parsed.getSuggestions() == null || parsed.getSuggestions().isEmpty()) {
                throw new IllegalArgumentException("AI response missing suggestions");
            }
            List<String> cleaned = parsed.getSuggestions().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .limit(6)
                .collect(Collectors.toList());
            return new AiNutritionSuggestionsResponse(cleaned);
        } catch (IllegalArgumentException e) {
            throw new AiClientException("Invalid AI nutrition suggestions payload: " + e.getMessage(), e);
        }
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
