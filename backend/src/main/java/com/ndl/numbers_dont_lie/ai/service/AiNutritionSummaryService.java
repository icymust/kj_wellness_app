package com.ndl.numbers_dont_lie.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.GroqClient;
import com.ndl.numbers_dont_lie.ai.dto.AiNutritionSummaryRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiNutritionSummaryResponse;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiNutritionSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(AiNutritionSummaryService.class);
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiNutritionSummaryService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public AiNutritionSummaryResponse generateSummary(AiNutritionSummaryRequest request) {
        if (request == null || request.getDate() == null || request.getNutritionSummary() == null) {
            throw new IllegalArgumentException("date and nutritionSummary are required");
        }

        AiNutritionSummaryRequest.NutritionSummary n = request.getNutritionSummary();
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
            request.getDate(),
            request.getUserGoal(),
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

        JsonNode response = groqClient.callForJson(prompt);
        AiNutritionSummaryResponse parsed = parseResponse(response);

        logger.info(
            "[AI_NUTRITION] Generated daily nutrition summary for userId={} date={}",
            request.getUserId(),
            request.getDate()
        );

        return parsed;
    }

    private String buildPrompt(
            String date,
            String userGoal,
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
        String goal = (userGoal == null || userGoal.isBlank()) ? "general_fitness" : userGoal;

        StringBuilder sb = new StringBuilder();
        sb.append("Create a 3-5 sentence nutrition insight summary in neutral, supportive tone.\n");
        sb.append("Return STRICT JSON only with schema: { \"summary\": string }\n");
        sb.append("Use ONLY the provided numbers. Do NOT invent values.\n");
        sb.append("Do NOT give medical advice or suggest supplements.\n\n");
        sb.append("Inputs:\n");
        sb.append("- date: ").append(date).append("\n");
        sb.append("- user goal: ").append(goal).append("\n");
        sb.append("- calories: ").append(calories).append("\n");
        sb.append("- targetCalories: ").append(targetCalories).append("\n");
        sb.append("- calorieDelta (actual - target): ").append(calorieDelta).append("\n");
        sb.append("- protein: ").append(protein).append("\n");
        sb.append("- targetProtein: ").append(targetProtein).append("\n");
        sb.append("- carbs: ").append(carbs).append("\n");
        sb.append("- targetCarbs: ").append(targetCarbs).append("\n");
        sb.append("- fats: ").append(fats).append("\n");
        sb.append("- targetFats: ").append(targetFats).append("\n");
        sb.append("- nutritionEstimated: ").append(estimated).append("\n\n");
        sb.append("Requirements:\n");
        sb.append("- Mention calorie surplus or deficit relative to target.\n");
        sb.append("- Comment on macro balance vs targets (good or needs attention).\n");
        sb.append("- If nutritionEstimated is true, mention values are estimated.\n");
        return sb.toString();
    }

    private AiNutritionSummaryResponse parseResponse(JsonNode response) {
        try {
            AiNutritionSummaryResponse parsed = objectMapper.convertValue(response, AiNutritionSummaryResponse.class);
            if (parsed.getSummary() == null || parsed.getSummary().isBlank()) {
                throw new IllegalArgumentException("AI response missing summary");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new AiClientException("Invalid AI nutrition summary payload: " + e.getMessage(), e);
        }
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
