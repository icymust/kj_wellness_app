package com.ndl.numbers_dont_lie.recipe.controller;

import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.MealType;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
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
    
    public RecipeController(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
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
            ingMap.put("label", ri.getIngredient().getLabel());
            ingMap.put("quantity", ri.getQuantity());
            ingMap.put("unit", ri.getIngredient().getUnit());
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
}
