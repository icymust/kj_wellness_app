package com.ndl.numbers_dont_lie.shoppinglist.service;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.RecipeIngredient;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.shoppinglist.dto.ShoppingListItemDto;
import com.ndl.numbers_dont_lie.shoppinglist.dto.DailyShoppingListResponse;
import com.ndl.numbers_dont_lie.shoppinglist.dto.MealShoppingListResponse;
import com.ndl.numbers_dont_lie.shoppinglist.dto.WeeklyShoppingListResponse;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ShoppingListService {
    private static final Logger logger = LoggerFactory.getLogger(ShoppingListService.class);
    private final DayPlanRepository dayPlanRepository;
    private final RecipeRepository recipeRepository;
    private final MealRepository mealRepository;

    public ShoppingListService(DayPlanRepository dayPlanRepository, RecipeRepository recipeRepository, MealRepository mealRepository) {
        this.dayPlanRepository = dayPlanRepository;
        this.recipeRepository = recipeRepository;
        this.mealRepository = mealRepository;
    }

    public DailyShoppingListResponse buildDailyShoppingList(Long userId, LocalDate date) {
        logger.info("[SHOPPING_LIST] Generating daily shopping list for userId={} date={}", userId, date);

        AggregationResult aggregation = aggregateDay(userId, date, PlanDuration.DAILY);
        logger.info("[SHOPPING_LIST] Meals processed: {}", aggregation.mealsProcessed);
        logger.info("[SHOPPING_LIST] Unique ingredients: {}", aggregation.aggregated.size());

        List<ShoppingListItemDto> items = new ArrayList<>(aggregation.aggregated.values());
        return new DailyShoppingListResponse(date.toString(), items);
    }

    public WeeklyShoppingListResponse buildWeeklyShoppingList(Long userId, LocalDate startDate) {
        logger.info("[SHOPPING_LIST] Generating weekly shopping list for userId={} startDate={}", userId, startDate);

        Map<String, ShoppingListItemDto> aggregated = new LinkedHashMap<>();
        int totalMealsProcessed = 0;
        int daysProcessed = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            AggregationResult dayResult = aggregateDay(userId, date, PlanDuration.WEEKLY);
            if (dayResult.mealsProcessed > 0) {
                daysProcessed++;
            }
            totalMealsProcessed += dayResult.mealsProcessed;
            mergeAggregates(aggregated, dayResult.aggregated);
        }

        logger.info("[SHOPPING_LIST] Days processed: {}", daysProcessed);
        logger.info("[SHOPPING_LIST] Total meals processed: {}", totalMealsProcessed);
        logger.info("[SHOPPING_LIST] Unique ingredients: {}", aggregated.size());

        List<ShoppingListItemDto> items = new ArrayList<>(aggregated.values());
        LocalDate endDate = startDate.plusDays(6);
        return new WeeklyShoppingListResponse(startDate.toString(), endDate.toString(), items);
    }

    public MealShoppingListResponse buildMealShoppingList(Long mealId) {
        logger.info("[SHOPPING_LIST] Generating meal shopping list for mealId={}", mealId);

        if (mealId == null) {
            return new MealShoppingListResponse(null, null, null, new ArrayList<>());
        }

        Meal meal = mealRepository.findById(mealId).orElse(null);
        if (meal == null) {
            return new MealShoppingListResponse(mealId, null, null, new ArrayList<>());
        }

        AggregationResult aggregation = aggregateMeal(meal);
        logger.info("[SHOPPING_LIST] Meals processed: {}", aggregation.mealsProcessed);
        logger.info("[SHOPPING_LIST] Unique ingredients: {}", aggregation.aggregated.size());

        List<ShoppingListItemDto> items = new ArrayList<>(aggregation.aggregated.values());
        String mealType = meal.getMealType() != null ? meal.getMealType().toString() : null;
        String mealName = meal.getCustomMealName();
        return new MealShoppingListResponse(mealId, mealType, mealName, items);
    }

    public AggregationResult aggregateDay(Long userId, LocalDate date, PlanDuration duration) {
        Map<String, ShoppingListItemDto> aggregated = new LinkedHashMap<>();
        int mealsProcessed = 0;

        DayPlan dayPlan = resolveDayPlan(userId, date, duration);
        if (dayPlan != null && dayPlan.getMeals() != null) {
            for (Meal meal : dayPlan.getMeals()) {
                mealsProcessed++;
                String recipeId = meal.getRecipeId();
                if (recipeId == null || recipeId.isBlank()) {
                    continue;
                }
                Recipe recipe = recipeRepository.findByStableId(recipeId).orElse(null);
                if (recipe == null || recipe.getIngredients() == null) {
                    continue;
                }
                for (RecipeIngredient ri : recipe.getIngredients()) {
                    if (ri.getIngredient() == null || ri.getIngredient().getLabel() == null) {
                        continue;
                    }
                    String name = normalize(ri.getIngredient().getLabel());
                    if (name.isEmpty()) {
                        continue;
                    }
                    String unit = normalizeUnit(ri.getIngredient().getUnit());
                    String key = name + "|" + unit;
                    double qty = ri.getQuantity() != null ? ri.getQuantity() : 0.0;

                    ShoppingListItemDto existing = aggregated.get(key);
                    if (existing == null) {
                        aggregated.put(key, new ShoppingListItemDto(name, qty, unit, categorizeIngredient(name)));
                    } else {
                        existing.setTotalQuantity(existing.getTotalQuantity() + qty);
                    }
                }
            }
        }

        return new AggregationResult(aggregated, mealsProcessed);
    }

    private AggregationResult aggregateMeal(Meal meal) {
        Map<String, ShoppingListItemDto> aggregated = new LinkedHashMap<>();
        int mealsProcessed = 0;

        if (meal != null) {
            mealsProcessed = 1;
            String recipeId = meal.getRecipeId();
            if (recipeId != null && !recipeId.isBlank()) {
                Recipe recipe = recipeRepository.findByStableId(recipeId).orElse(null);
                if (recipe != null && recipe.getIngredients() != null) {
                    for (RecipeIngredient ri : recipe.getIngredients()) {
                        if (ri.getIngredient() == null || ri.getIngredient().getLabel() == null) {
                            continue;
                        }
                        String name = normalize(ri.getIngredient().getLabel());
                        if (name.isEmpty()) {
                            continue;
                        }
                        String unit = normalizeUnit(ri.getIngredient().getUnit());
                        String key = name + "|" + unit;
                        double qty = ri.getQuantity() != null ? ri.getQuantity() : 0.0;

                        ShoppingListItemDto existing = aggregated.get(key);
                        if (existing == null) {
                            aggregated.put(key, new ShoppingListItemDto(name, qty, unit, categorizeIngredient(name)));
                        } else {
                            existing.setTotalQuantity(existing.getTotalQuantity() + qty);
                        }
                    }
                }
            }
        }

        return new AggregationResult(aggregated, mealsProcessed);
    }

    private DayPlan resolveDayPlan(Long userId, LocalDate date, PlanDuration duration) {
        List<DayPlan> candidates = dayPlanRepository.findByUserIdAndDateWithMealsAndDurationOrderByIdDesc(
            userId,
            date,
            duration
        );
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void mergeAggregates(Map<String, ShoppingListItemDto> target, Map<String, ShoppingListItemDto> source) {
        for (Map.Entry<String, ShoppingListItemDto> entry : source.entrySet()) {
            ShoppingListItemDto existing = target.get(entry.getKey());
            if (existing == null) {
                ShoppingListItemDto item = entry.getValue();
                target.put(entry.getKey(), new ShoppingListItemDto(
                    item.getIngredient(),
                    item.getTotalQuantity(),
                    item.getUnit(),
                    item.getCategory()
                ));
            } else {
                existing.setTotalQuantity(existing.getTotalQuantity() + entry.getValue().getTotalQuantity());
            }
        }
    }

    public static class AggregationResult {
        private final Map<String, ShoppingListItemDto> aggregated;
        private final int mealsProcessed;

        public AggregationResult(Map<String, ShoppingListItemDto> aggregated, int mealsProcessed) {
            this.aggregated = aggregated;
            this.mealsProcessed = mealsProcessed;
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUnit(String unit) {
        String normalized = normalize(unit);
        return normalized.isEmpty() ? "unit" : normalized;
    }

    private String categorizeIngredient(String name) {
        String n = normalize(name);
        if (n.isEmpty()) return "Other";

        if (containsAny(n, "milk", "cheese", "yogurt", "butter", "cream", "mozzarella", "parmesan", "cheddar", "feta", "ricotta")) {
            return "Dairy";
        }
        if (containsAny(n, "chicken", "beef", "pork", "turkey", "ham", "bacon", "sausage", "fish", "salmon", "tuna", "shrimp", "egg", "tofu", "lentil", "bean", "chickpea")) {
            return "Protein";
        }
        if (containsAny(n, "apple", "banana", "orange", "berry", "berry", "grape", "lemon", "lime", "avocado", "tomato", "onion", "garlic", "lettuce", "spinach", "kale", "pepper", "cucumber", "carrot", "broccoli", "cauliflower", "mushroom", "potato", "sweet potato", "zucchini", "corn", "pea")) {
            return "Produce";
        }
        if (containsAny(n, "rice", "pasta", "noodle", "bread", "tortilla", "oat", "flour", "cereal", "quinoa", "barley", "couscous")) {
            return "Grains";
        }
        if (containsAny(n, "oil", "vinegar", "sauce", "ketchup", "mustard", "mayo", "sugar", "salt", "pepper", "spice", "herb", "basil", "oregano", "cumin", "paprika", "chili", "cocoa", "vanilla")) {
            return "Pantry";
        }
        if (containsAny(n, "water", "juice", "soda", "tea", "coffee", "milkshake")) {
            return "Beverages";
        }
        return "Other";
    }

    private boolean containsAny(String text, String... needles) {
        for (String n : needles) {
            if (text.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
