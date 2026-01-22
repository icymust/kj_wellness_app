package com.ndl.numbers_dont_lie.ai.function;

import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;

/**
 * Function calling interface for nutrition calculation.
 * 
 * CRITICAL: This is the ONLY way to calculate nutrition in the AI pipeline.
 * AI must NEVER guess or hardcode nutritional values.
 * 
 * Flow:
 * 1. AI generates recipe with ingredients (name, quantity, unit)
 * 2. AI calls calculate_nutrition function
 * 3. System matches ingredients to database
 * 4. System calculates nutrition based on actual ingredient data
 * 5. Function returns NutritionInfo
 * 6. AI embeds result into final recipe JSON
 * 
 * Why function calling:
 * - Accuracy: Uses verified ingredient database, not AI estimates
 * - Consistency: Same calculation logic for all recipes
 * - Traceability: Clear audit trail for nutritional claims
 * - Safety: No hallucinated nutrition data
 */
public interface NutritionCalculator {
    
    /**
     * Calculate nutrition for a recipe based on its ingredients.
     * 
     * Process:
     * - Match ingredient names to database (fuzzy matching if needed)
     * - Scale nutrition by quantity
     * - Sum all ingredients
     * - Calculate per-serving values
     * 
     * @param ingredients List of ingredients with quantities
     * @param servings Number of servings
     * @return Calculated nutrition info (total + per serving)
     * @throws IngredientNotFoundException if ingredient not in database
     * @throws InvalidQuantityException if quantity is invalid
     */
    GeneratedRecipe.NutritionInfo calculate(
        java.util.List<GeneratedRecipe.GeneratedIngredient> ingredients,
        int servings
    );
}
