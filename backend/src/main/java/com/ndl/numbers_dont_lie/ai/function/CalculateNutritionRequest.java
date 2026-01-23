package com.ndl.numbers_dont_lie.ai.function;

import java.util.List;

/**
 * STEP 6.1: Function Calling Contract
 * 
 * Explicit separation between AI generation and nutrition calculation.
 * 
 * CONTRACT:
 * - Function name: calculateNutrition
 * - AI MUST call this function to get nutrition
 * - AI MUST NOT include calorie/macro numbers in free text
 * - Backend MUST execute this via DatabaseNutritionCalculator
 * - AI embeds function result into final recipe JSON
 * 
 * INVARIANT:
 * All nutrition data in final recipe MUST come from function output.
 * Never from AI hallucination or estimation.
 * 
 * This enforces: AI generates structure, Database provides nutrition.
 */
public interface CalculateNutritionRequest {
    
    /**
     * Ingredient for nutrition calculation.
     */
    record IngredientInput(
        String name,           // Required: ingredient name
        double quantity,       // Required: amount (in standard units)
        String unit            // Required: g, ml, cup, tbsp, tsp, piece, etc
    ) {
        public void validate() {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Ingredient name cannot be blank");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("Ingredient quantity must be positive, got: " + quantity);
            }
            if (unit == null || unit.isBlank()) {
                throw new IllegalArgumentException("Ingredient unit cannot be blank");
            }
        }
    }
    
    /**
     * Function call input from AI.
     */
    record Input(
        List<IngredientInput> ingredients,  // Required: list of ingredients with quantities
        int servings                        // Required: number of servings (must be > 0)
    ) {
        public void validate() {
            if (ingredients == null || ingredients.isEmpty()) {
                throw new IllegalArgumentException("Ingredients list cannot be empty");
            }
            if (servings <= 0) {
                throw new IllegalArgumentException("Servings must be positive, got: " + servings);
            }
            for (IngredientInput ing : ingredients) {
                ing.validate();
            }
        }
    }
    
    /**
     * Function call output with calculated nutrition.
     */
    record Output(
        double calories,
        double protein,        // grams
        double carbohydrates,  // grams
        double fats,           // grams
        double caloriesPerServing,
        double proteinPerServing,
        double carbsPerServing,
        double fatsPerServing,
        String calculationMethod,  // "database_lookup", "interpolation", etc
        long executionTimeMs
    ) {}
}
