package com.ndl.numbers_dont_lie.assistant.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AssistantToolValidator {

    public void validate(String toolName, Map<String, Object> args) {
        switch (toolName) {
            case "get_health_profile" -> {
                String metricType = stringArg(args, "metricType", "all");
                requireEnum("metricType", metricType, "bmi", "weight", "wellness_score", "activity_level", "goals", "all");
                String period = stringArg(args, "period", "current");
                requireEnum("period", period, "current", "weekly", "monthly");
            }
            case "get_goal_progress" -> {
                String goalType = stringArg(args, "goalType", "weight");
                requireEnum("goalType", goalType, "weight", "wellness", "activity", "all");
                String period = stringArg(args, "period", "monthly");
                requireEnum("period", period, "weekly", "monthly", "current");
            }
            case "get_meal_plan" -> {
                String dateOrRange = stringArg(args, "dateOrRange", "today");
                validateDateOrRange(dateOrRange);
            }
            case "get_recipe_details" -> {
                String date = stringArg(args, "date", "today");
                validateDateOrRange(date);
                String mealType = stringArg(args, "mealType", "dinner");
                requireEnum("mealType", mealType, "breakfast", "lunch", "dinner", "snack");
            }
            case "get_nutrition_analysis" -> {
                String period = stringArg(args, "period", "daily");
                requireEnum("period", period, "daily", "weekly");
                String nutrientType = stringArg(args, "nutrientType", "all");
                requireEnum("nutrientType", nutrientType, "calories", "protein", "carbs", "fats", "all");
            }
            case "get_chart_trend" -> {
                String chartType = stringArg(args, "chartType", "weight");
                requireEnum("chartType", chartType, "weight", "calories", "wellness");
                String period = stringArg(args, "period", "weekly");
                requireEnum("period", period, "weekly", "monthly");
            }
            default -> throw new IllegalArgumentException("Unsupported function: " + toolName);
        }
    }

    private String stringArg(Map<String, Object> args, String key, String defaultValue) {
        Object raw = args.get(key);
        if (raw == null) {
            return defaultValue;
        }
        String value = raw.toString().trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    private void requireEnum(String field, String value, String... allowed) {
        for (String item : allowed) {
            if (item.equals(value)) {
                return;
            }
        }
        throw new IllegalArgumentException("Invalid " + field + ": " + value);
    }

    private void validateDateOrRange(String raw) {
        String value = raw.toLowerCase(Locale.ROOT).trim();
        if (value.equals("today") || value.equals("tomorrow") || value.equals("yesterday") || value.equals("week") || value.equals("this_week")) {
            return;
        }
        try {
            LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid dateOrRange: " + raw + ". Use YYYY-MM-DD, today, tomorrow, or week.");
        }
    }
}
