package com.ndl.numbers_dont_lie.assistant.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AssistantToolPlanner {

    public static class ToolCallSpec {
        private final String name;
        private final Map<String, Object> args;

        public ToolCallSpec(String name, Map<String, Object> args) {
            this.name = name;
            this.args = args;
        }

        public String getName() {
            return name;
        }

        public Map<String, Object> getArgs() {
            return args;
        }
    }

    public List<ToolCallSpec> plan(String message, String lastTopic) {
        String q = message == null ? "" : message.toLowerCase(Locale.ROOT);
        List<ToolCallSpec> planned = new ArrayList<>();
        Set<String> dedup = new LinkedHashSet<>();

        boolean asksHealth = containsAny(q, "bmi", "weight", "wellness", "activity level", "goal", "fitness goal");
        boolean asksProgress = containsAny(q, "how close", "progress", "on track", "improving");
        boolean asksMealPlan = containsAny(q, "meal plan", "what's for", "what is for", "today meals", "tomorrow", "lunch", "dinner", "breakfast", "snack");
        boolean asksRecipe = containsAny(q, "recipe", "prepare", "cook", "ingredients", "steps", "how do i make");
        boolean asksNutrition = containsAny(q, "protein", "calories", "carbs", "fats", "nutrition", "macro");
        boolean asksTrend = containsAny(q, "trend", "chart", "changed", "this month", "this week");
        boolean followUp = containsAny(q, "tell me more", "why is that", "why is it", "is that enough", "can you expand", "more about that");

        if (asksHealth || (followUp && "health".equals(lastTopic))) {
            String metricType = metricTypeFromQuestion(q);
            put(planned, dedup, new ToolCallSpec("get_health_profile", Map.of(
                "metricType", metricType,
                "period", periodFromQuestion(q, "current")
            )));
        }

        if (asksProgress || (followUp && "progress".equals(lastTopic))) {
            put(planned, dedup, new ToolCallSpec("get_goal_progress", Map.of(
                "goalType", "weight",
                "period", periodFromQuestion(q, "monthly")
            )));
        }

        if (asksMealPlan || (followUp && "meal_plan".equals(lastTopic))) {
            put(planned, dedup, new ToolCallSpec("get_meal_plan", Map.of(
                "dateOrRange", dateOrRangeFromQuestion(q)
            )));
        }

        if (asksRecipe || (followUp && "recipe".equals(lastTopic))) {
            put(planned, dedup, new ToolCallSpec("get_recipe_details", Map.of(
                "date", dateFromQuestion(q),
                "mealType", mealTypeFromQuestion(q)
            )));
        }

        if (asksNutrition || (followUp && "nutrition".equals(lastTopic))) {
            put(planned, dedup, new ToolCallSpec("get_nutrition_analysis", Map.of(
                "period", nutritionPeriodFromQuestion(q),
                "nutrientType", nutrientFromQuestion(q)
            )));
        }

        if (asksTrend || (followUp && "trend".equals(lastTopic))) {
            put(planned, dedup, new ToolCallSpec("get_chart_trend", Map.of(
                "chartType", chartTypeFromQuestion(q),
                "period", periodFromQuestion(q, "weekly")
            )));
        }

        if (planned.isEmpty()) {
            put(planned, dedup, new ToolCallSpec("get_health_profile", Map.of(
                "metricType", "all",
                "period", "current"
            )));
        }

        if (planned.size() > 4) {
            return planned.subList(0, 4);
        }
        return planned;
    }

    private void put(List<ToolCallSpec> list, Set<String> dedup, ToolCallSpec spec) {
        String key = spec.getName() + "|" + spec.getArgs().toString();
        if (dedup.add(key)) {
            list.add(spec);
        }
    }

    private boolean containsAny(String text, String... values) {
        for (String value : values) {
            if (text.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String metricTypeFromQuestion(String q) {
        boolean bmi = q.contains("bmi");
        boolean weight = q.contains("weight");
        boolean wellness = q.contains("wellness");
        boolean activity = q.contains("activity");
        boolean goal = q.contains("goal");
        int count = (bmi ? 1 : 0) + (weight ? 1 : 0) + (wellness ? 1 : 0) + (activity ? 1 : 0) + (goal ? 1 : 0);
        if (count > 1) {
            return "all";
        }
        if (bmi) return "bmi";
        if (weight) return "weight";
        if (wellness) return "wellness_score";
        if (activity) return "activity_level";
        if (goal) return "goals";
        return "all";
    }

    private String periodFromQuestion(String q, String fallback) {
        if (q.contains("this week") || q.contains("weekly") || q.contains("week")) {
            return "weekly";
        }
        if (q.contains("this month") || q.contains("monthly") || q.contains("month")) {
            return "monthly";
        }
        if (q.contains("today") || q.contains("current") || q.contains("now")) {
            return "current";
        }
        return fallback;
    }

    private String dateOrRangeFromQuestion(String q) {
        if (q.contains("tomorrow")) return "tomorrow";
        if (q.contains("yesterday")) return "yesterday";
        if (q.contains("this week") || q.contains("week")) return "week";
        if (q.contains("today")) return "today";
        return "today";
    }

    private String dateFromQuestion(String q) {
        if (q.contains("tomorrow")) return "tomorrow";
        if (q.contains("yesterday")) return "yesterday";
        if (q.contains("today") || q.contains("tonight")) return "today";
        return LocalDate.now().toString();
    }

    private String mealTypeFromQuestion(String q) {
        if (q.contains("breakfast")) return "breakfast";
        if (q.contains("lunch")) return "lunch";
        if (q.contains("snack")) return "snack";
        return "dinner";
    }

    private String nutrientFromQuestion(String q) {
        if (q.contains("protein")) return "protein";
        if (q.contains("carb")) return "carbs";
        if (q.contains("fat")) return "fats";
        if (q.contains("calorie")) return "calories";
        return "all";
    }

    private String nutritionPeriodFromQuestion(String q) {
        if (q.contains("week") || q.contains("weekly")) return "weekly";
        return "daily";
    }

    private String chartTypeFromQuestion(String q) {
        if (q.contains("calorie")) return "calories";
        if (q.contains("wellness")) return "wellness";
        return "weight";
    }
}
