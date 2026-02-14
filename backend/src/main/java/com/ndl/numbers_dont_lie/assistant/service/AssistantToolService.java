package com.ndl.numbers_dont_lie.assistant.service;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.health.GoalProgress;
import com.ndl.numbers_dont_lie.health.HealthCalc;
import com.ndl.numbers_dont_lie.mealplan.dto.DailyNutritionSummary;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.service.NutritionSummaryService;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.RecipeIngredient;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import com.ndl.numbers_dont_lie.weight.entity.WeightEntry;
import com.ndl.numbers_dont_lie.weight.repository.WeightEntryRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AssistantToolService {

    private final ProfileRepository profileRepository;
    private final WeightEntryRepository weightEntryRepository;
    private final DayPlanRepository dayPlanRepository;
    private final RecipeRepository recipeRepository;
    private final NutritionSummaryService nutritionSummaryService;
    private final NutritionalPreferencesRepository nutritionalPreferencesRepository;

    public AssistantToolService(
            ProfileRepository profileRepository,
            WeightEntryRepository weightEntryRepository,
            DayPlanRepository dayPlanRepository,
            RecipeRepository recipeRepository,
            NutritionSummaryService nutritionSummaryService,
            NutritionalPreferencesRepository nutritionalPreferencesRepository) {
        this.profileRepository = profileRepository;
        this.weightEntryRepository = weightEntryRepository;
        this.dayPlanRepository = dayPlanRepository;
        this.recipeRepository = recipeRepository;
        this.nutritionSummaryService = nutritionSummaryService;
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
    }

    public Map<String, Object> execute(String toolName, Map<String, Object> args, UserEntity user) {
        return switch (toolName) {
            case "get_health_profile" -> getHealthProfile(args, user);
            case "get_goal_progress" -> getGoalProgress(args, user);
            case "get_meal_plan" -> getMealPlan(args, user);
            case "get_recipe_details" -> getRecipeDetails(args, user);
            case "get_nutrition_analysis" -> getNutritionAnalysis(args, user);
            case "get_chart_trend" -> getChartTrend(args, user);
            default -> throw new IllegalArgumentException("Unsupported function: " + toolName);
        };
    }

    private Map<String, Object> getHealthProfile(Map<String, Object> args, UserEntity user) {
        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        String metricType = lower(args.get("metricType"), "all");
        String period = lower(args.get("period"), "current");
        double latestWeight = latestWeight(profile, user);
        HealthCalc.BmiResult bmi = null;
        if (profile.getHeightCm() != null && latestWeight > 0) {
            bmi = HealthCalc.bmi(latestWeight, profile.getHeightCm());
        }

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("weightKg", round1(latestWeight));
        metrics.put("heightCm", profile.getHeightCm());
        if (bmi != null) {
            metrics.put("bmi", bmi.bmi);
            metrics.put("bmiClassification", bmi.classification);
        }
        metrics.put("activityLevel", profile.getActivityLevel());
        metrics.put("goal", profile.getGoal());
        metrics.put("targetWeightKg", profile.getTargetWeightKg());
        metrics.put("wellnessScore", estimateWellnessScore(profile, latestWeight, bmi));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_health_profile");
        response.put("period", period);
        response.put("metricType", metricType);
        response.put("userName", userName(user));

        if (!"all".equals(metricType)) {
            response.put("metrics", Map.of(metricType, metricValue(metricType, metrics)));
        } else {
            response.put("metrics", metrics);
        }
        return response;
    }

    private Object metricValue(String metricType, Map<String, Object> metrics) {
        return switch (metricType) {
            case "bmi" -> Map.of(
                "bmi", metrics.get("bmi"),
                "classification", metrics.get("bmiClassification")
            );
            case "weight" -> metrics.get("weightKg");
            case "wellness_score" -> metrics.get("wellnessScore");
            case "activity_level" -> metrics.get("activityLevel");
            case "goals" -> Map.of(
                "goal", metrics.get("goal"),
                "targetWeightKg", metrics.get("targetWeightKg")
            );
            default -> metrics;
        };
    }

    private Map<String, Object> getGoalProgress(Map<String, Object> args, UserEntity user) {
        String period = lower(args.get("period"), "monthly");
        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        List<WeightEntry> weights = weightEntryRepository.findAllByUserOrderByAtAsc(user);
        Double initial = !weights.isEmpty() ? weights.get(0).getWeightKg() : profile.getWeightKg();
        Double current = !weights.isEmpty() ? weights.get(weights.size() - 1).getWeightKg() : profile.getWeightKg();
        Double target = profile.getTargetWeightKg();

        GoalProgress.Result progress = GoalProgress.progress(initial, target, current);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_goal_progress");
        response.put("period", period);
        response.put("goal", profile.getGoal());
        response.put("initialWeightKg", initial);
        response.put("currentWeightKg", current);
        response.put("targetWeightKg", target);
        response.put("progressPercent", progress.percent);
        response.put("coveredKg", progress.coveredKg);
        response.put("remainingKg", progress.remainingKg);
        response.put("milestones5Percent", progress.milestones5);
        return response;
    }

    private Map<String, Object> getMealPlan(Map<String, Object> args, UserEntity user) {
        String dateOrRange = lower(args.get("dateOrRange"), "today");

        LocalDate base = resolveDate(dateOrRange);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_meal_plan");

        if ("week".equals(dateOrRange) || "this_week".equals(dateOrRange)) {
            LocalDate start = base.with(DayOfWeek.MONDAY);
            LocalDate end = start.plusDays(6);
            response.put("startDate", start.toString());
            response.put("endDate", end.toString());

            List<Map<String, Object>> days = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                days.add(dayMeals(date, user.getId()));
            }
            response.put("days", days);
            return response;
        }

        response.put("date", base.toString());
        response.put("day", dayMeals(base, user.getId()));
        return response;
    }

    private Map<String, Object> dayMeals(LocalDate date, Long userId) {
        // Weekly data can contain historical versions for the same date.
        // Always use the newest day plan to avoid non-unique result failures.
        Optional<DayPlan> plan = dayPlanRepository.findByUserIdAndDateWithMealsOrderByIdDesc(userId, date)
            .stream()
            .findFirst();
        Map<String, Object> day = new LinkedHashMap<>();
        day.put("date", date.toString());

        List<Map<String, Object>> meals = new ArrayList<>();
        if (plan.isPresent()) {
            for (Meal meal : plan.get().getMeals()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("mealType", meal.getMealType() != null ? meal.getMealType().getJsonValue() : "unknown");
                item.put("time", meal.getPlannedTime() != null ? meal.getPlannedTime().toLocalTime().toString() : null);
                item.put("recipeId", meal.getRecipeId());
                item.put("name", mealName(meal));
                meals.add(item);
            }
            meals.sort(Comparator.comparing(m -> String.valueOf(m.get("time"))));
        }
        day.put("meals", meals);
        return day;
    }

    private String mealName(Meal meal) {
        if (meal.getCustomMealName() != null && !meal.getCustomMealName().isBlank()) {
            return meal.getCustomMealName();
        }
        if (meal.getRecipeId() != null && !meal.getRecipeId().isBlank()) {
            return recipeRepository.findByStableId(meal.getRecipeId())
                .map(Recipe::getTitle)
                .orElse(meal.getRecipeId());
        }
        return "Meal";
    }

    private Map<String, Object> getRecipeDetails(Map<String, Object> args, UserEntity user) {
        LocalDate date = resolveDate(lower(args.get("date"), "today"));
        String mealType = lower(args.get("mealType"), "dinner");

        DayPlan dayPlan = dayPlanRepository.findByUserIdAndDateWithMeals(user.getId(), date)
            .orElseThrow(() -> new IllegalArgumentException("No meal plan found for date " + date));

        Meal selectedMeal = dayPlan.getMeals().stream()
            .filter(m -> m.getMealType() != null && m.getMealType().getJsonValue().equals(mealType))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No " + mealType + " meal found for " + date));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_recipe_details");
        response.put("date", date.toString());
        response.put("mealType", mealType);
        response.put("mealName", mealName(selectedMeal));

        if (selectedMeal.getRecipeId() == null || selectedMeal.getRecipeId().isBlank()) {
            response.put("recipe", Map.of(
                "type", "custom",
                "name", mealName(selectedMeal),
                "note", "This is a custom meal without recipe steps."
            ));
            return response;
        }

        Recipe recipe = recipeRepository.findByStableId(selectedMeal.getRecipeId())
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + selectedMeal.getRecipeId()));

        List<Map<String, Object>> ingredients = new ArrayList<>();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", ingredient.getIngredient().getLabel());
            item.put("quantity", ingredient.getQuantity());
            item.put("unit", ingredient.getIngredient().getUnit());
            ingredients.add(item);
        }

        List<Map<String, Object>> preparation = new ArrayList<>();
        recipe.getPreparationSteps().stream()
            .sorted(Comparator.comparing(s -> s.getOrderNumber() == null ? 0 : s.getOrderNumber()))
            .forEach(step -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("step", step.getOrderNumber());
                row.put("description", step.getDescription());
                preparation.add(row);
            });

        Map<String, Object> recipeMap = new LinkedHashMap<>();
        recipeMap.put("id", recipe.getStableId());
        recipeMap.put("title", recipe.getTitle());
        recipeMap.put("cuisine", recipe.getCuisine());
        recipeMap.put("timeMinutes", recipe.getTimeMinutes());
        recipeMap.put("difficulty", recipe.getDifficultyLevel() != null ? recipe.getDifficultyLevel().name().toLowerCase(Locale.ROOT) : null);
        recipeMap.put("ingredients", ingredients);
        recipeMap.put("preparation", preparation);

        response.put("recipe", recipeMap);
        return response;
    }

    private Map<String, Object> getNutritionAnalysis(Map<String, Object> args, UserEntity user) {
        String period = lower(args.get("period"), "daily");
        String nutrientType = lower(args.get("nutrientType"), "all");

        if ("weekly".equals(period)) {
            return weeklyNutrition(user, nutrientType);
        }
        return dailyNutrition(user, LocalDate.now(ZoneId.systemDefault()), nutrientType);
    }

    private Map<String, Object> dailyNutrition(UserEntity user, LocalDate date, String nutrientType) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_nutrition_analysis");
        response.put("period", "daily");
        response.put("date", date.toString());

        DailyNutritionSummary summary = dayPlanRepository.findByUserIdAndDateWithMeals(user.getId(), date)
            .map(nutritionSummaryService::generateSummary)
            .orElseGet(() -> emptySummary(user.getId(), date));

        response.put("nutrition", selectedNutrition(summary, nutrientType));
        response.put("targets", Map.of(
            "calories", round1(summary.getTargetCalories()),
            "protein", round1(summary.getTargetProtein()),
            "carbs", round1(summary.getTargetCarbs()),
            "fats", round1(summary.getTargetFats())
        ));
        response.put("delta", Map.of(
            "calories", round1(summary.getTotalCalories() - summary.getTargetCalories()),
            "protein", round1(summary.getTotalProtein() - summary.getTargetProtein()),
            "carbs", round1(summary.getTotalCarbs() - summary.getTargetCarbs()),
            "fats", round1(summary.getTotalFats() - summary.getTargetFats())
        ));
        return response;
    }

    private Map<String, Object> weeklyNutrition(UserEntity user, String nutrientType) {
        LocalDate start = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate end = start.plusDays(6);

        double calories = 0.0;
        double protein = 0.0;
        double carbs = 0.0;
        double fats = 0.0;
        double targetCalories = 0.0;
        double targetProtein = 0.0;
        double targetCarbs = 0.0;
        double targetFats = 0.0;

        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate currentDate = date;
            DailyNutritionSummary summary = dayPlanRepository.findByUserIdAndDateWithMeals(user.getId(), currentDate)
                .map(nutritionSummaryService::generateSummary)
                .orElseGet(() -> emptySummary(user.getId(), currentDate));
            calories += summary.getTotalCalories();
            protein += summary.getTotalProtein();
            carbs += summary.getTotalCarbs();
            fats += summary.getTotalFats();
            targetCalories += summary.getTargetCalories();
            targetProtein += summary.getTargetProtein();
            targetCarbs += summary.getTargetCarbs();
            targetFats += summary.getTargetFats();
        }

        Map<String, Object> nutrition = new LinkedHashMap<>();
        nutrition.put("calories", round1(calories));
        nutrition.put("protein", round1(protein));
        nutrition.put("carbs", round1(carbs));
        nutrition.put("fats", round1(fats));

        Map<String, Object> targets = new LinkedHashMap<>();
        targets.put("calories", round1(targetCalories));
        targets.put("protein", round1(targetProtein));
        targets.put("carbs", round1(targetCarbs));
        targets.put("fats", round1(targetFats));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_nutrition_analysis");
        response.put("period", "weekly");
        response.put("startDate", start.toString());
        response.put("endDate", end.toString());
        response.put("nutrition", "all".equals(nutrientType) ? nutrition : Map.of(nutrientType, nutrition.get(nutrientType)));
        response.put("targets", targets);
        response.put("delta", Map.of(
            "calories", round1(calories - targetCalories),
            "protein", round1(protein - targetProtein),
            "carbs", round1(carbs - targetCarbs),
            "fats", round1(fats - targetFats)
        ));
        return response;
    }

    private Map<String, Object> selectedNutrition(DailyNutritionSummary summary, String nutrientType) {
        Map<String, Object> nutrition = new LinkedHashMap<>();
        nutrition.put("calories", round1(summary.getTotalCalories()));
        nutrition.put("protein", round1(summary.getTotalProtein()));
        nutrition.put("carbs", round1(summary.getTotalCarbs()));
        nutrition.put("fats", round1(summary.getTotalFats()));
        if ("all".equals(nutrientType)) {
            return nutrition;
        }
        return Map.of(nutrientType, nutrition.get(nutrientType));
    }

    private DailyNutritionSummary emptySummary(Long userId, LocalDate date) {
        DailyNutritionSummary summary = new DailyNutritionSummary();
        summary.setDate(date);
        summary.setTotalCalories(0.0);
        summary.setTotalProtein(0.0);
        summary.setTotalCarbs(0.0);
        summary.setTotalFats(0.0);
        NutritionalPreferences prefs = nutritionalPreferencesRepository.findByUserId(userId).orElse(null);
        summary.setTargetCalories(prefs != null && prefs.getCalorieTarget() != null ? prefs.getCalorieTarget() : 0.0);
        summary.setTargetProtein(prefs != null && prefs.getProteinTarget() != null ? prefs.getProteinTarget() : 0.0);
        summary.setTargetCarbs(prefs != null && prefs.getCarbsTarget() != null ? prefs.getCarbsTarget() : 0.0);
        summary.setTargetFats(prefs != null && prefs.getFatsTarget() != null ? prefs.getFatsTarget() : 0.0);
        summary.setNutritionEstimated(true);
        return summary;
    }

    private Map<String, Object> getChartTrend(Map<String, Object> args, UserEntity user) {
        String chartType = lower(args.get("chartType"), "weight");
        String period = lower(args.get("period"), "weekly");

        LocalDate end = LocalDate.now();
        LocalDate start = "monthly".equals(period) ? end.minusDays(29) : end.minusDays(6);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("function", "get_chart_trend");
        response.put("chartType", chartType);
        response.put("period", period);
        response.put("startDate", start.toString());
        response.put("endDate", end.toString());

        if ("calories".equals(chartType)) {
            List<Map<String, Object>> points = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                LocalDate currentDate = date;
                DailyNutritionSummary summary = dayPlanRepository.findByUserIdAndDateWithMeals(user.getId(), currentDate)
                    .map(nutritionSummaryService::generateSummary)
                    .orElseGet(() -> emptySummary(user.getId(), currentDate));
                points.add(Map.of(
                    "date", date.toString(),
                    "value", round1(summary.getTotalCalories()),
                    "target", round1(summary.getTargetCalories())
                ));
            }
            response.put("points", points);
            response.put("trend", trendLabel(points));
            return response;
        }

        if ("wellness".equals(chartType)) {
            ProfileEntity profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));
            double latestWeight = latestWeight(profile, user);
            HealthCalc.BmiResult bmi = profile.getHeightCm() != null && latestWeight > 0
                ? HealthCalc.bmi(latestWeight, profile.getHeightCm())
                : null;
            double currentWellness = estimateWellnessScore(profile, latestWeight, bmi);
            List<Map<String, Object>> points = new ArrayList<>();
            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                points.add(Map.of("date", date.toString(), "value", currentWellness));
            }
            response.put("points", points);
            response.put("trend", "stable");
            return response;
        }

        // Default weight trend
        List<WeightEntry> entries = weightEntryRepository.findAllByUserOrderByAtAsc(user);
        List<Map<String, Object>> points = new ArrayList<>();
        for (WeightEntry entry : entries) {
            LocalDate date = entry.getAt().atZone(ZoneId.systemDefault()).toLocalDate();
            if (date.isBefore(start) || date.isAfter(end)) {
                continue;
            }
            points.add(Map.of(
                "date", date.toString(),
                "value", round1(entry.getWeightKg())
            ));
        }
        response.put("points", points);
        response.put("trend", trendLabel(points));
        return response;
    }

    private String trendLabel(List<Map<String, Object>> points) {
        if (points.size() < 2) {
            return "insufficient_data";
        }
        double first = asDouble(points.get(0).get("value"));
        double last = asDouble(points.get(points.size() - 1).get("value"));
        double diff = round1(last - first);
        if (Math.abs(diff) < 0.3) {
            return "stable";
        }
        return diff > 0 ? "up" : "down";
    }

    private double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private LocalDate resolveDate(String token) {
        String value = token == null ? "today" : token.toLowerCase(Locale.ROOT);
        LocalDate now = LocalDate.now(ZoneId.systemDefault());
        return switch (value) {
            case "today" -> now;
            case "tomorrow" -> now.plusDays(1);
            case "yesterday" -> now.minusDays(1);
            case "week", "this_week" -> now.with(DayOfWeek.MONDAY);
            default -> {
                try {
                    yield LocalDate.parse(value);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Invalid date value: " + token);
                }
            }
        };
    }

    private double latestWeight(ProfileEntity profile, UserEntity user) {
        List<WeightEntry> entries = weightEntryRepository.findAllByUserOrderByAtAsc(user);
        if (!entries.isEmpty()) {
            return entries.get(entries.size() - 1).getWeightKg();
        }
        return profile.getWeightKg() != null ? profile.getWeightKg() : 0.0;
    }

    private double estimateWellnessScore(ProfileEntity profile, double latestWeight, HealthCalc.BmiResult bmi) {
        double bmiScore = bmi != null ? HealthCalc.bmiScoreComponent(bmi.classification) : 50.0;
        double activityScore = HealthCalc.activityScore(profile.getActivityLevel());
        double progressScore = HealthCalc.progressScore(latestWeight, profile.getTargetWeightKg());
        return HealthCalc.wellness(bmiScore, activityScore, progressScore, activityScore);
    }

    private String lower(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = value.toString().trim().toLowerCase(Locale.ROOT);
        return text.isEmpty() ? fallback : text;
    }

    private double round1(Double value) {
        if (value == null) return 0.0;
        return Math.round(value * 10.0) / 10.0;
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String userName(UserEntity user) {
        String email = user.getEmail() != null ? user.getEmail() : "User";
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }
}
