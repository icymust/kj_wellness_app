package com.ndl.numbers_dont_lie.mealplan.controller;

import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;
import com.ndl.numbers_dont_lie.ai.function.DatabaseNutritionCalculator;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * STEP 6.2: Debug Meal Portion Adjustment Controller
 * 
 * TEMPORARY controller for testing portion adjustment and nutrition recalculation.
 * 
 * This controller is for debugging purposes only and NOT intended for production use.
 * It allows developers and testers to:
 * - Adjust serving size of a meal
 * - Recalculate nutrition via DatabaseNutritionCalculator (function calling)
 * - Test nutrition calculation without AI involvement
 * - Verify that nutrition is NEVER calculated by AI
 * 
 * IMPORTANT NOTES:
 * - No authentication required - debug only
 * - No UI logic - pure backend endpoint
 * - Uses DatabaseNutritionCalculator (same as STEP 6.1)
 * - All nutrition recalculated via function calling
 * - AI is NOT involved in this endpoint
 * 
 * DESIGN INTENT:
 * This endpoint demonstrates that nutrition calculation can be done
 * independently of AI. The same DatabaseNutritionCalculator used by
 * AI function calling (STEP 6.1) is used here directly.
 * 
 * @see DatabaseNutritionCalculator - calculates nutrition from ingredients
 */
@RestController
@RequestMapping("/api/debug/meals")
public class DebugMealPortionController {
    private static final Logger logger = LoggerFactory.getLogger(DebugMealPortionController.class);
    
    private final MealRepository mealRepository;
    private final DatabaseNutritionCalculator nutritionCalculator;
    
    public DebugMealPortionController(
            MealRepository mealRepository,
            DatabaseNutritionCalculator nutritionCalculator) {
        this.mealRepository = mealRepository;
        this.nutritionCalculator = nutritionCalculator;
    }
    
    /**
     * Adjust meal portion and recalculate nutrition.
     * 
     * REQUEST BODY:
     * {
     *   "servings": number (must be > 0),
     *   "ingredients": [
     *     { "name": "chicken breast", "quantity": 200, "unit": "g" },
     *     { "name": "rice", "quantity": 150, "unit": "g" },
     *     ...
     *   ]
     * }
     * 
     * PROCESS:
     * 1. Validate servings > 0
     * 2. Validate meal exists
     * 3. Build ingredient list from request
     * 4. Call DatabaseNutritionCalculator.calculate()
     * 5. Return updated nutrition (no persistence)
     * 
     * WHY FUNCTION CALLING:
     * - Ensures nutrition comes from database only
     * - Consistent with STEP 6.1 contract
     * - No AI involvement (pure calculation)
     * - Reuses validated nutrition logic
     * 
     * NOTE: This endpoint does NOT persist changes. It only returns
     * recalculated nutrition. In production, you would persist updated
     * meal data to database.
     * 
     * @param mealId Meal ID (must exist, but not used for data - this is debug only)
     * @param request Portion adjustment request with servings and ingredients
     * @return Recalculated nutrition information
     */
    @PostMapping("/{mealId}/portion")
    public ResponseEntity<?> adjustPortion(
            @PathVariable Long mealId,
            @RequestBody PortionAdjustmentRequest request) {
        try {
            logger.info("[DEBUG-PORTION] Adjusting portion for meal={}, servings={}", 
                mealId, request.getServings());
            
            // Validate servings
            if (request.getServings() == null || request.getServings() <= 0) {
                logger.warn("[DEBUG-PORTION] Invalid servings: {}", request.getServings());
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "error", "Invalid servings",
                        "message", "Servings must be greater than 0",
                        "servings", request.getServings() != null ? request.getServings() : "null"
                ));
            }
            
            // Validate meal exists (for debug purposes)
            Meal meal = mealRepository.findById(mealId).orElse(null);
            if (meal == null) {
                logger.warn("[DEBUG-PORTION] Meal not found: {}", mealId);
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "error", "Meal not found",
                        "message", "No meal found with ID: " + mealId,
                        "mealId", mealId
                ));
            }
            
            // Validate ingredients
            if (request.getIngredients() == null || request.getIngredients().isEmpty()) {
                logger.warn("[DEBUG-PORTION] No ingredients provided");
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "error", "Invalid ingredients",
                        "message", "Ingredients list cannot be empty"
                ));
            }
            
            logger.info("[DEBUG-PORTION] Recalculating nutrition for {} ingredients",
                request.getIngredients().size());
            
            // Convert request ingredients to GeneratedRecipe.GeneratedIngredient format
            List<GeneratedRecipe.GeneratedIngredient> ingredients = new ArrayList<>();
            for (PortionAdjustmentRequest.IngredientInput input : request.getIngredients()) {
                GeneratedRecipe.GeneratedIngredient ingredient = new GeneratedRecipe.GeneratedIngredient();
                ingredient.setName(input.getName());
                ingredient.setQuantity(input.getQuantity());
                ingredient.setUnit(input.getUnit());
                ingredients.add(ingredient);
            }
            
            // Call DatabaseNutritionCalculator (same as STEP 6.1 function calling)
            // This is the ONLY way to calculate nutrition - no AI involved
            GeneratedRecipe.NutritionInfo nutrition = nutritionCalculator.calculate(
                    ingredients,
                    request.getServings()
            );
            
            logger.info("[DEBUG-PORTION] Nutrition recalculated successfully:");
            logger.info("  - Total: {} cal, {}g protein, {}g carbs, {}g fat",
                nutrition.getCalories(), nutrition.getProtein(),
                nutrition.getCarbohydrates(), nutrition.getFat());
            logger.info("  - Per serving: {} cal, {}g protein, {}g carbs, {}g fat",
                nutrition.getCaloriesPerServing(), nutrition.getProteinPerServing(),
                nutrition.getCarbsPerServing(), nutrition.getFatPerServing());
            
            // Build response with recalculated nutrition
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Nutrition recalculated successfully",
                    "mealId", mealId,
                    "servings", request.getServings(),
                    "ingredientCount", ingredients.size(),
                    "nutrition", Map.of(
                        "total", Map.of(
                            "calories", nutrition.getCalories(),
                            "protein", nutrition.getProtein(),
                            "carbohydrates", nutrition.getCarbohydrates(),
                            "fat", nutrition.getFat()
                        ),
                        "perServing", Map.of(
                            "calories", nutrition.getCaloriesPerServing(),
                            "protein", nutrition.getProteinPerServing(),
                            "carbohydrates", nutrition.getCarbsPerServing(),
                            "fat", nutrition.getFatPerServing()
                        )
                    ),
                    "note", "Nutrition calculated via DatabaseNutritionCalculator (function calling, no AI)"
            ));
            
        } catch (IllegalArgumentException e) {
            // Validation errors from DatabaseNutritionCalculator
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Validation failed";
            logger.error("[DEBUG-PORTION] Nutrition calculation validation failed: {}", errorMsg);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", "Nutrition calculation failed",
                    "message", errorMsg,
                    "mealId", mealId
            ));
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("[DEBUG-PORTION] Unexpected error adjusting portion for meal={}", mealId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "error", "Unexpected error",
                    "message", errorMsg,
                    "mealId", mealId,
                    "exceptionType", e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * Request body for portion adjustment.
     */
    public static class PortionAdjustmentRequest {
        private Integer servings;
        private List<IngredientInput> ingredients;
        
        public Integer getServings() {
            return servings;
        }
        
        public void setServings(Integer servings) {
            this.servings = servings;
        }
        
        public List<IngredientInput> getIngredients() {
            return ingredients;
        }
        
        public void setIngredients(List<IngredientInput> ingredients) {
            this.ingredients = ingredients;
        }
        
        /**
         * Ingredient with quantity for portion adjustment.
         */
        public static class IngredientInput {
            private String name;
            private Double quantity;
            private String unit;
            
            public String getName() {
                return name;
            }
            
            public void setName(String name) {
                this.name = name;
            }
            
            public Double getQuantity() {
                return quantity;
            }
            
            public void setQuantity(Double quantity) {
                this.quantity = quantity;
            }
            
            public String getUnit() {
                return unit;
            }
            
            public void setUnit(String unit) {
                this.unit = unit;
            }
        }
    }
}
