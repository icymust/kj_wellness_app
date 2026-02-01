package com.ndl.numbers_dont_lie.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.GroqClient;
import com.ndl.numbers_dont_lie.ai.dto.AiWeeklyNutritionInsightsRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiWeeklyNutritionInsightsResponse;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiWeeklyNutritionInsightsService {
    private static final Logger logger = LoggerFactory.getLogger(AiWeeklyNutritionInsightsService.class);
    private final GroqClient groqClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiWeeklyNutritionInsightsService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    public AiWeeklyNutritionInsightsResponse generateInsights(AiWeeklyNutritionInsightsRequest request) {
        if (request == null || request.getStartDate() == null || request.getWeeklyNutrition() == null) {
            throw new IllegalArgumentException("startDate and weeklyNutrition are required");
        }

        AiWeeklyNutritionInsightsRequest.WeeklyNutrition w = request.getWeeklyNutrition();
        double totalCalories = safe(w.getTotalCalories());
        double targetCalories = safe(w.getTargetCalories());
        double totalProtein = safe(w.getTotalProtein());
        double targetProtein = safe(w.getTargetProtein());
        double totalCarbs = safe(w.getTotalCarbs());
        double targetCarbs = safe(w.getTargetCarbs());
        double totalFats = safe(w.getTotalFats());
        double targetFats = safe(w.getTargetFats());
        boolean estimated = w.getNutritionEstimated() != null && w.getNutritionEstimated();

        String prompt = buildPrompt(
            request,
            totalCalories,
            targetCalories,
            totalProtein,
            targetProtein,
            totalCarbs,
            targetCarbs,
            totalFats,
            targetFats,
            estimated
        );

        JsonNode response = groqClient.callForJson(prompt, 0.2);
        AiWeeklyNutritionInsightsResponse parsed = parseResponse(response);

        logger.info(
            "[AI_NUTRITION] Generated weekly nutrition insights for userId={} startDate={}",
            request.getUserId(),
            request.getStartDate()
        );

        return parsed;
    }

    private String buildPrompt(
            AiWeeklyNutritionInsightsRequest request,
            double totalCalories,
            double targetCalories,
            double totalProtein,
            double targetProtein,
            double totalCarbs,
            double targetCarbs,
            double totalFats,
            double targetFats,
            boolean estimated) {
        String goal = (request.getUserGoal() == null || request.getUserGoal().isBlank())
            ? "maintenance"
            : request.getUserGoal();

        List<String> dailyLines = new ArrayList<>();
        if (request.getDailySummaries() != null) {
            for (var day : request.getDailySummaries()) {
                String date = day.getDate() != null ? day.getDate() : "unknown";
                double cal = safe(day.getCalories());
                double target = safe(day.getTargetCalories());
                dailyLines.add(date + ": " + cal + " / " + target);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Write a 3-6 sentence weekly nutrition insight summary.\n");
        sb.append("Return STRICT JSON only with schema: { \"summary\": string }\n");
        sb.append("Rules:\n");
        sb.append("- Use ONLY provided data; do not invent numbers.\n");
        sb.append("- Do NOT give medical advice or suggest supplements.\n");
        sb.append("- Tone: supportive and analytical.\n");
        if (estimated) {
            sb.append("- Mention that insights are based on estimated values.\n");
        }
        sb.append("\nInputs:\n");
        sb.append("- startDate: ").append(request.getStartDate()).append("\n");
        sb.append("- endDate: ").append(request.getEndDate()).append("\n");
        sb.append("- goal: ").append(goal).append("\n");
        sb.append("- totalCalories: ").append(totalCalories).append("\n");
        sb.append("- targetCalories: ").append(targetCalories).append("\n");
        sb.append("- totalProtein: ").append(totalProtein).append("\n");
        sb.append("- targetProtein: ").append(targetProtein).append("\n");
        sb.append("- totalCarbs: ").append(totalCarbs).append("\n");
        sb.append("- targetCarbs: ").append(targetCarbs).append("\n");
        sb.append("- totalFats: ").append(totalFats).append("\n");
        sb.append("- targetFats: ").append(targetFats).append("\n");
        sb.append("- dailySummaries (date: actual / target): ").append(dailyLines).append("\n");
        return sb.toString();
    }

    private AiWeeklyNutritionInsightsResponse parseResponse(JsonNode response) {
        try {
            AiWeeklyNutritionInsightsResponse parsed = objectMapper.convertValue(response, AiWeeklyNutritionInsightsResponse.class);
            if (parsed.getSummary() == null || parsed.getSummary().isBlank()) {
                throw new IllegalArgumentException("AI response missing summary");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new AiClientException("Invalid AI weekly insights payload: " + e.getMessage(), e);
        }
    }

    private double safe(Double value) {
        return value == null ? 0.0 : value;
    }
}
