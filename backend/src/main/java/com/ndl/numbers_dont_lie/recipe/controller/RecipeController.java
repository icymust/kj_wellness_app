package com.ndl.numbers_dont_lie.recipe.controller;

import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Recipe Controller
 * 
 * READ-ONLY API for fetching recipe details by ID.
 * No modifications, no generation, no caching - just fetch and return.
 * 
 * Endpoint:
 * - GET /api/recipes/{id} - Fetch recipe by stable ID (string ID like "r00001") or database ID (long)
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
            
            // Convert to simple map to avoid circular reference issues
            var recipeMap = new java.util.HashMap<String, Object>();
            recipeMap.put("id", recipeEntity.getId());
            recipeMap.put("stable_id", recipeEntity.getStableId());
            recipeMap.put("title", recipeEntity.getTitle());
            recipeMap.put("cuisine", recipeEntity.getCuisine());
            recipeMap.put("meal", recipeEntity.getMeal());
            recipeMap.put("servings", recipeEntity.getServings());
            recipeMap.put("summary", recipeEntity.getSummary());
            recipeMap.put("time", recipeEntity.getTimeMinutes());
            recipeMap.put("difficulty_level", recipeEntity.getDifficultyLevel());
            recipeMap.put("source", recipeEntity.getSource());
            recipeMap.put("img", recipeEntity.getImageUrl());
            recipeMap.put("dietary_tags", recipeEntity.getDietaryTags());
            
            // Add ingredients as simple objects
            var ingredientsList = new java.util.ArrayList<java.util.Map<String, Object>>();
            for (var ri : recipeEntity.getIngredients()) {
                var ingMap = new java.util.HashMap<String, Object>();
                ingMap.put("label", ri.getIngredient().getLabel());
                ingMap.put("quantity", ri.getQuantity());
                ingMap.put("unit", ri.getIngredient().getUnit());
                ingredientsList.add(ingMap);
            }
            recipeMap.put("ingredients", ingredientsList);
            
            // Add preparation steps
            var stepsList = new java.util.ArrayList<String>();
            for (var step : recipeEntity.getPreparationSteps()) {
                stepsList.add(step.getDescription());
            }
            recipeMap.put("preparation", stepsList);
            
            return ResponseEntity.ok(recipeMap);
            
        } catch (Exception e) {
            logger.error("[RECIPE_API] Error fetching recipe ID {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
