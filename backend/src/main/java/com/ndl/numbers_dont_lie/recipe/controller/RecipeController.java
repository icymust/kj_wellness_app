package com.ndl.numbers_dont_lie.recipe.controller;

import com.ndl.numbers_dont_lie.recipe.dto.RecipeIngredientReplaceRequest;
import com.ndl.numbers_dont_lie.recipe.dto.RecipeServingsRequest;
import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;
import com.ndl.numbers_dont_lie.ai.function.DatabaseNutritionCalculator;
import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
import com.ndl.numbers_dont_lie.recipe.entity.MealType;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.RecipeIngredient;
import com.ndl.numbers_dont_lie.recipe.repository.IngredientRepository;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Recipe Controller
 * 
 * READ-ONLY API for fetching recipe details by ID.
 * No modifications, no generation, no caching - just fetch and return.
 * 
 * Endpoint:
 * - GET /api/recipes/{id} - Fetch recipe by stable ID (string ID like "r00001") or database ID (long)
 * - GET /api/recipes - List all recipes (supports filtering by meal type)
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);
    
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final DatabaseNutritionCalculator nutritionCalculator;
    
    public RecipeController(
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            DatabaseNutritionCalculator nutritionCalculator) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.nutritionCalculator = nutritionCalculator;
    }
    
    /**
     * List all recipes (optionally filtered by meal type).
     * 
     * GET /api/recipes?meal=breakfast
     * 
     * @param meal Optional meal type filter (breakfast, lunch, dinner, snack)
     * @return List of recipes
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> listRecipes(@RequestParam(required = false) String meal) {
        logger.info("[RECIPE_API] GET /api/recipes - Listing recipes, meal={}", meal);
        
        try {
            List<Recipe> recipes;
            
            if (meal != null && !meal.isEmpty()) {
                try {
                    MealType mealType = MealType.valueOf(meal.toUpperCase());
                    recipes = recipeRepository.findByMeal(mealType);
                    logger.info("[RECIPE_API] Found {} recipes for meal type: {}", recipes.size(), mealType);
                } catch (IllegalArgumentException e) {
                    logger.warn("[RECIPE_API] Invalid meal type: {}", meal);
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid meal type",
                        "message", "Must be one of: breakfast, lunch, dinner, snack"
                    ));
                }
            } else {
                recipes = recipeRepository.findAll();
                logger.info("[RECIPE_API] Found {} total recipes", recipes.size());
            }
            
            List<Map<String, Object>> recipeMaps = new ArrayList<>();
            for (Recipe r : recipes) {
                recipeMaps.add(recipeToMap(r));
            }
            
            return ResponseEntity.ok(recipeMaps);
            
        } catch (Exception e) {
            logger.error("[RECIPE_API] Error listing recipes: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to list recipes",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Fetch a recipe by ID.
     * Supports both stable ID (String, e.g., "r00001") and database ID (Long).
     * Returns all fields from the Recipe entity as-is (raw data for debugging).
     * 
     * @param id Recipe ID (stable string ID or database ID)
     * @return Recipe entity with all fields, or 404 if not found
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getRecipeById(@PathVariable String id) {
        logger.info("[RECIPE_API] GET /api/recipes/{} - Fetching recipe", id);
        
        try {
            var recipe = recipeRepository.findByStableId(id);
            
            if (recipe.isEmpty()) {
                logger.warn("[RECIPE_API] Recipe not found for stable ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Recipe recipeEntity = recipe.get();
            logger.info("[RECIPE_API] Found recipe: {} (stable ID: {})", recipeEntity.getTitle(), recipeEntity.getStableId());
            
            return ResponseEntity.ok(recipeToMap(recipeEntity));
            
        } catch (Exception e) {
            logger.error("[RECIPE_API] Error fetching recipe ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Adjust recipe servings (read-only) and recalculate ingredient quantities + nutrition.
     *
     * POST /api/recipes/{recipeId}/servings
     */
    @PostMapping("/{recipeId}/servings")
    @Transactional(readOnly = true)
    public ResponseEntity<?> adjustServings(
            @PathVariable Long recipeId,
            @RequestBody RecipeServingsRequest request) {
        if (request == null || request.getServings() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "servings is required"
            ));
        }

        int newServings = request.getServings();
        if (newServings <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "servings must be positive"
            ));
        }

        var recipeOpt = recipeRepository.findById(recipeId);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Recipe recipe = recipeOpt.get();
        int baseServings = recipe.getServings() != null && recipe.getServings() > 0
            ? recipe.getServings()
            : 1;
        double ratio = (double) newServings / baseServings;

        Map<String, Object> response = recipeToMapScaled(recipe, newServings, ratio);
        return ResponseEntity.ok(response);
    }

    /**
     * Replace a single ingredient in a recipe.
     *
     * POST /api/recipes/{recipeId}/ingredients/replace
     */
    @PostMapping("/{recipeId}/ingredients/replace")
    @Transactional
    public ResponseEntity<?> replaceIngredient(
            @PathVariable Long recipeId,
            @RequestBody RecipeIngredientReplaceRequest request) {
        if (request == null || request.getIngredientName() == null || request.getNewIngredientName() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "ingredientName and newIngredientName are required"
            ));
        }

        var recipeOpt = recipeRepository.findById(recipeId);
        if (recipeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Recipe recipe = recipeOpt.get();
        String oldName = normalize(request.getIngredientName());
        String newName = normalize(request.getNewIngredientName());

        RecipeIngredient target = null;
        for (var ri : recipe.getIngredients()) {
            if (ri.getIngredient() != null && normalize(ri.getIngredient().getLabel()).equals(oldName)) {
                target = ri;
                break;
            }
        }

        if (target == null) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "Ingredient not found in recipe"
            ));
        }

        String fallbackUnit = target.getIngredient() != null ? target.getIngredient().getUnit() : "gram";
        Ingredient replacement = ingredientRepository.findByLabel(newName)
            .orElseGet(() -> createIngredient(newName, fallbackUnit));

        target.setIngredient(replacement);
        Recipe saved = recipeRepository.save(recipe);
        logger.info("[RECIPE_API] Replaced ingredient '{}' with '{}' for recipeId={}", oldName, newName, recipeId);

        return ResponseEntity.ok(recipeToMap(saved));
    }
    
    /**
     * Convert Recipe entity to a simple Map (avoiding circular references).
     */
    private Map<String, Object> recipeToMap(Recipe recipe) {
        var recipeMap = new HashMap<String, Object>();
        recipeMap.put("id", recipe.getId());
        recipeMap.put("stable_id", recipe.getStableId());
        recipeMap.put("title", recipe.getTitle());
        recipeMap.put("cuisine", recipe.getCuisine());
        recipeMap.put("meal", recipe.getMeal());
        recipeMap.put("servings", recipe.getServings());
        recipeMap.put("summary", recipe.getSummary());
        recipeMap.put("time", recipe.getTimeMinutes());
        recipeMap.put("difficulty_level", recipe.getDifficultyLevel());
        recipeMap.put("source", recipe.getSource());
        recipeMap.put("img", recipe.getImageUrl());
        recipeMap.put("dietary_tags", recipe.getDietaryTags());
        
        // Add ingredients as simple objects
        var ingredientsList = new ArrayList<Map<String, Object>>();
        for (var ri : recipe.getIngredients()) {
            var ingMap = new HashMap<String, Object>();
            var ingredient = ri.getIngredient();
            ingMap.put("label", ingredient.getLabel());
            ingMap.put("quantity", ri.getQuantity());
            ingMap.put("unit", ingredient.getUnit());
            ingMap.put("nutrition", ingredient.getNutrition());
            ingredientsList.add(ingMap);
        }
        recipeMap.put("ingredients", ingredientsList);
        
        // Add preparation steps
        var stepsList = new ArrayList<String>();
        for (var step : recipe.getPreparationSteps()) {
            stepsList.add(step.getDescription());
        }
        recipeMap.put("preparation", stepsList);
        
        return recipeMap;
    }

    private Map<String, Object> recipeToMapScaled(Recipe recipe, int newServings, double ratio) {
        var recipeMap = new HashMap<String, Object>();
        recipeMap.put("id", recipe.getId());
        recipeMap.put("stable_id", recipe.getStableId());
        recipeMap.put("title", recipe.getTitle());
        recipeMap.put("cuisine", recipe.getCuisine());
        recipeMap.put("meal", recipe.getMeal());
        recipeMap.put("servings", newServings);
        recipeMap.put("summary", recipe.getSummary());
        recipeMap.put("time", recipe.getTimeMinutes());
        recipeMap.put("difficulty_level", recipe.getDifficultyLevel());
        recipeMap.put("source", recipe.getSource());
        recipeMap.put("img", recipe.getImageUrl());
        recipeMap.put("dietary_tags", recipe.getDietaryTags());

        var ingredientsList = new ArrayList<Map<String, Object>>();
        List<GeneratedRecipe.GeneratedIngredient> calcIngredients = new ArrayList<>();

        for (var ri : recipe.getIngredients()) {
            var ingredient = ri.getIngredient();
            var ingMap = new HashMap<String, Object>();
            ingMap.put("label", ingredient.getLabel());
            double scaledQty = ri.getQuantity() * ratio;
            ingMap.put("quantity", scaledQty);
            ingMap.put("unit", ingredient.getUnit());
            ingMap.put("nutrition", ingredient.getNutrition());
            ingredientsList.add(ingMap);

            GeneratedRecipe.GeneratedIngredient calcIng = new GeneratedRecipe.GeneratedIngredient();
            calcIng.setName(ingredient.getLabel());
            calcIng.setQuantity(scaledQty);
            calcIng.setUnit(ingredient.getUnit());
            if (ingredient.getStableId() != null) {
                calcIng.setIngredientId(ingredient.getStableId());
            }
            calcIngredients.add(calcIng);
        }
        recipeMap.put("ingredients", ingredientsList);

        var stepsList = new ArrayList<String>();
        for (var step : recipe.getPreparationSteps()) {
            stepsList.add(step.getDescription());
        }
        recipeMap.put("preparation", stepsList);

        GeneratedRecipe.NutritionInfo nutrition = nutritionCalculator.calculate(calcIngredients, newServings);
        Map<String, Object> nutritionSummary = new HashMap<>();
        nutritionSummary.put("calories", nutrition.getCalories());
        nutritionSummary.put("protein", nutrition.getProtein());
        nutritionSummary.put("carbs", nutrition.getCarbohydrates());
        nutritionSummary.put("fats", nutrition.getFat());
        nutritionSummary.put("caloriesPerServing", nutrition.getCaloriesPerServing());
        nutritionSummary.put("proteinPerServing", nutrition.getProteinPerServing());
        nutritionSummary.put("carbsPerServing", nutrition.getCarbsPerServing());
        nutritionSummary.put("fatsPerServing", nutrition.getFatPerServing());
        recipeMap.put("nutrition_summary", nutritionSummary);

        return recipeMap;
    }

    private Ingredient createIngredient(String label, String fallbackUnit) {
        String stableId = generateNextIngredientStableId();
        String unit = fallbackUnit != null ? fallbackUnit : "gram";
        Ingredient ingredient = new Ingredient(stableId, label, unit, 100.0, 0.0, 0.0, 0.0, 0.0);
        return ingredientRepository.save(ingredient);
    }

    private String generateNextIngredientStableId() {
        long nextId = ingredientRepository.findTopByOrderByIdDesc()
            .map(i -> i.getId() + 1)
            .orElse(1L);
        return String.format("ing%013d", nextId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
