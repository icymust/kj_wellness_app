package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for replacing individual meals in a day plan.
 * Respects user dietary preferences and ensures variety within the day.
 */
@Service
public class MealReplacementService {
    private static final Logger logger = LoggerFactory.getLogger(MealReplacementService.class);
    
    private final MealRepository mealRepository;
    private final DayPlanRepository dayPlanRepository;
    private final RecipeRepository recipeRepository;
    private final NutritionalPreferencesRepository nutritionalPreferencesRepository;
    
    public MealReplacementService(
            MealRepository mealRepository,
            DayPlanRepository dayPlanRepository,
            RecipeRepository recipeRepository,
            NutritionalPreferencesRepository nutritionalPreferencesRepository) {
        this.mealRepository = mealRepository;
        this.dayPlanRepository = dayPlanRepository;
        this.recipeRepository = recipeRepository;
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
    }
    
    /**
     * Replace a meal with an alternative recipe.
     * 
     * Process:
     * 1. Load meal and parent day plan
     * 2. Load user preferences
     * 3. Collect already used recipes in day
     * 4. Find alternative recipe from DB (respecting preferences, avoiding duplicates)
     * 5. Update meal in place
     * 6. Save and return updated meal
     * 
     * @param mealId ID of meal to replace
     * @return Updated Meal object
     * @throws IllegalArgumentException if meal not found
     * @throws IllegalStateException if no alternative recipe found
     */
    @Transactional
    public Meal replaceMeal(Long mealId) {
        logger.info("[MEAL_REPLACE] Requested mealId={}", mealId);
        
        // Load meal
        Meal meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new IllegalArgumentException("Meal not found: " + mealId));
        
        String originalTitle = meal.getCustomMealName();
        MealType mealType = meal.getMealType();
        Integer targetCalories = meal.getCalorieTarget();
        
        logger.info("[MEAL_REPLACE] Original meal: {}", originalTitle);
        
        // Load parent day plan
        DayPlan dayPlan = meal.getDayPlan();
        if (dayPlan == null) {
            throw new IllegalStateException("Meal has no parent day plan: " + mealId);
        }
        
        Long userId = dayPlan.getUserId();
        
        // Load user preferences
        NutritionalPreferences preferences = nutritionalPreferencesRepository.findByUserId(userId)
                .orElse(null);
        
        // Collect already used recipe titles in this day (for variety)
        Set<String> usedTitles = dayPlan.getMeals().stream()
                .map(Meal::getCustomMealName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        
        logger.info("[MEAL_REPLACE] Used recipes in day: {}", usedTitles);
        
        // Find alternative recipe
        Recipe alternativeRecipe = findAlternativeRecipe(
                mealType,
                targetCalories,
                usedTitles,
                preferences
        );
        
        if (alternativeRecipe == null) {
            throw new IllegalStateException("No alternative recipe found for meal type: " + mealType);
        }
        
        logger.info("[MEAL_REPLACE] Selected replacement: {} source=DB", alternativeRecipe.getTitle());
        
        // Update meal in place
        meal.setCustomMealName(alternativeRecipe.getTitle());
        meal.setRecipeId(alternativeRecipe.getStableId());
        // Keep same calorie target and planned calories
        if (meal.getPlannedCalories() == null) {
            meal.setPlannedCalories(targetCalories);
        }
        
        // Save updated meal
        Meal savedMeal = mealRepository.save(meal);
        
        logger.info("[MEAL_REPLACE] Replacement successful: {} → {}", originalTitle, alternativeRecipe.getTitle());
        
        return savedMeal;
    }
    
    /**
     * Find an alternative recipe from the database.
     * 
     * Criteria:
     * - Matches meal type (breakfast/lunch/dinner/snack)
     * - Excludes already used recipes in the day
     * - Respects dietary preferences (vegetarian, allergies, dislikes)
     * - Prefers recipes with similar calorie count
     * 
     * @param mealType Target meal type
     * @param targetCalories Target calorie amount (used for preference, not filter)
     * @param excludeTitles Recipe titles to exclude
     * @param preferences User dietary preferences (can be null)
     * @return Alternative Recipe or null if none found
     */
    private Recipe findAlternativeRecipe(
            MealType mealType,
            Integer targetCalories,
            Set<String> excludeTitles,
            NutritionalPreferences preferences) {
        
        logger.info("[MEAL_REPLACE] Searching alternative: type={}, calories={}, exclude={}", 
                mealType, targetCalories, excludeTitles.size());
        
        // Map MealType to Recipe's MealType enum
        com.ndl.numbers_dont_lie.recipe.entity.MealType recipeMealType = mapToRecipeMealType(mealType);
        
        // Query recipes by meal type
        List<Recipe> candidates = recipeRepository.findByMeal(recipeMealType);
        
        logger.info("[MEAL_REPLACE] Found {} candidate recipes for meal type: {}", 
                candidates.size(), recipeMealType);
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Filter out excluded recipes
        candidates = candidates.stream()
                .filter(r -> !excludeTitles.contains(r.getTitle()))
                .collect(Collectors.toList());
        
        logger.info("[MEAL_REPLACE] {} recipes after exclusion filter", candidates.size());
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Apply dietary filters if preferences exist
        if (preferences != null) {
            candidates = applyDietaryFilters(candidates, preferences);
            logger.info("[MEAL_REPLACE] {} recipes after dietary filters", candidates.size());
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // Sort by calorie proximity (closest to target) and pick first
        if (targetCalories != null && targetCalories > 0) {
            candidates.sort(Comparator.comparingInt(r -> 
                    Math.abs(estimateRecipeCalories(r) - targetCalories)
            ));
        }
        
        // Return first candidate (random if no calorie target, closest if target exists)
        Recipe selected = candidates.get(0);
        logger.info("[MEAL_REPLACE] Selected recipe: {} (calories ~{})", 
                selected.getTitle(), estimateRecipeCalories(selected));
        
        return selected;
    }
    
    /**
     * Map meal plan MealType to Recipe MealType.
     */
    private com.ndl.numbers_dont_lie.recipe.entity.MealType mapToRecipeMealType(MealType mealType) {
        switch (mealType) {
            case BREAKFAST:
                return com.ndl.numbers_dont_lie.recipe.entity.MealType.BREAKFAST;
            case LUNCH:
                return com.ndl.numbers_dont_lie.recipe.entity.MealType.LUNCH;
            case DINNER:
                return com.ndl.numbers_dont_lie.recipe.entity.MealType.DINNER;
            case SNACK:
                return com.ndl.numbers_dont_lie.recipe.entity.MealType.SNACK;
            default:
                return com.ndl.numbers_dont_lie.recipe.entity.MealType.LUNCH;
        }
    }
    
    /**
     * Apply dietary preference filters to recipe candidates.
     * 
     * Filters:
     * - Vegetarian: Exclude recipes with meat/poultry/fish ingredients
     * - Allergies: Exclude recipes containing allergens
     * - Disliked ingredients: Exclude recipes with disliked items
     * 
     * Note: This is basic text matching. Production version would use structured ingredient data.
     */
    private List<Recipe> applyDietaryFilters(List<Recipe> recipes, NutritionalPreferences preferences) {
        List<Recipe> filtered = new ArrayList<>(recipes);
        
        // Vegetarian filter
        if (preferences.getDietaryPreferences() != null && 
            preferences.getDietaryPreferences().stream()
                    .anyMatch(p -> p.toLowerCase().contains("vegetarian"))) {
            
            List<String> meatKeywords = Arrays.asList(
                    "chicken", "beef", "pork", "lamb", "turkey", "duck", 
                    "fish", "salmon", "tuna", "shrimp", "bacon", "sausage"
            );
            
            filtered = filtered.stream()
                    .filter(r -> {
                        String ingredientsStr = r.getIngredients().stream()
                                .map(ri -> ri.getIngredient() != null && ri.getIngredient().getLabel() != null 
                                        ? ri.getIngredient().getLabel().toLowerCase() 
                                        : "")
                                .reduce("", (a, b) -> a + " " + b);
                        return meatKeywords.stream().noneMatch(ingredientsStr::contains);
                    })
                    .collect(Collectors.toList());
            
            logger.info("[MEAL_REPLACE] Applied vegetarian filter");
        }
        
        // Allergy filter
        if (preferences.getAllergies() != null && !preferences.getAllergies().isEmpty()) {
            List<String> allergens = new ArrayList<>(preferences.getAllergies());
            
            filtered = filtered.stream()
                    .filter(r -> {
                        String ingredientsStr = r.getIngredients().stream()
                                .map(ri -> ri.getIngredient() != null && ri.getIngredient().getLabel() != null 
                                        ? ri.getIngredient().getLabel().toLowerCase() 
                                        : "")
                                .reduce("", (a, b) -> a + " " + b);
                        return allergens.stream()
                                .map(String::toLowerCase)
                                .noneMatch(ingredientsStr::contains);
                    })
                    .collect(Collectors.toList());
            
            logger.info("[MEAL_REPLACE] Applied allergy filter: {}", preferences.getAllergies());
        }
        
        // Disliked ingredients filter
        if (preferences.getDislikedIngredients() != null && !preferences.getDislikedIngredients().isEmpty()) {
            List<String> disliked = preferences.getDislikedIngredients();
            
            filtered = filtered.stream()
                    .filter(r -> {
                        String ingredientsStr = r.getIngredients().stream()
                                .map(ri -> ri.getIngredient() != null && ri.getIngredient().getLabel() != null 
                                        ? ri.getIngredient().getLabel().toLowerCase() 
                                        : "")
                                .reduce("", (a, b) -> a + " " + b);
                        return disliked.stream()
                                .map(String::toLowerCase)
                                .noneMatch(ingredientsStr::contains);
                    })
                    .collect(Collectors.toList());
            
            logger.info("[MEAL_REPLACE] Applied disliked ingredients filter: {}", preferences.getDislikedIngredients());
        }
        
        return filtered;
    }
    
    /**
     * Estimate recipe calories from title/ingredients.
     * This is a rough heuristic - production would use nutrition API.
     * 
     * Returns:
     * - Breakfast: 400-500 cal average
     * - Lunch/Dinner: 600-700 cal average
     * - Snack: 200-250 cal average
     */
    private int estimateRecipeCalories(Recipe recipe) {
        String meal = recipe.getMeal() != null ? recipe.getMeal().name().toLowerCase() : "";
        
        if (meal.contains("breakfast")) {
            return 450;
        } else if (meal.contains("lunch") || meal.contains("dinner")) {
            return 650;
        } else if (meal.contains("snack")) {
            return 225;
        } else {
            return 500; // Default
        }
    }
}
