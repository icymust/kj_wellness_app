package com.ndl.numbers_dont_lie.ai.function;

import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;
import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
import com.ndl.numbers_dont_lie.recipe.repository.IngredientRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of nutrition calculation via ingredient database lookup.
 * 
 * Uses actual ingredient data, not AI estimates.
 * Supports fuzzy matching for ingredient names.
 */
@Service
public class DatabaseNutritionCalculator implements NutritionCalculator {
    private final IngredientRepository ingredientRepository;

    public DatabaseNutritionCalculator(IngredientRepository ingredientRepository) {
        this.ingredientRepository = ingredientRepository;
    }

    @Override
    public GeneratedRecipe.NutritionInfo calculate(
            List<GeneratedRecipe.GeneratedIngredient> ingredients,
            int servings) {
        
        if (servings <= 0) {
            throw new IllegalArgumentException("Servings must be positive");
        }

        double totalCalories = 0.0;
        double totalProtein = 0.0;
        double totalCarbs = 0.0;
        double totalFat = 0.0;
        double totalFiber = 0.0;
        double totalSugar = 0.0;
        double totalSodium = 0.0;

        for (GeneratedRecipe.GeneratedIngredient genIng : ingredients) {
            // Try to find ingredient in database
            Ingredient dbIngredient = findIngredient(genIng);
            
            if (dbIngredient == null) {
                // Log warning but continue (use zeros for this ingredient)
                System.err.println("Warning: Ingredient not found in database: " + genIng.getName());
                continue;
            }

            // Convert quantity to grams/ml (standardize to 100g/ml base)
            double quantityInStandardUnit = convertToStandardUnit(
                genIng.getQuantity(), 
                genIng.getUnit(), 
                dbIngredient.getUnit()
            );

            // Scale nutrition by quantity (ingredient nutrition is per 100g/ml)
            double scaleFactor = quantityInStandardUnit / 100.0;
            
            if (dbIngredient.getNutrition() != null) {
                totalCalories += (dbIngredient.getNutrition().getCalories() != null ? 
                    dbIngredient.getNutrition().getCalories() : 0.0) * scaleFactor;
                totalProtein += (dbIngredient.getNutrition().getProtein() != null ? 
                    dbIngredient.getNutrition().getProtein() : 0.0) * scaleFactor;
                totalCarbs += (dbIngredient.getNutrition().getCarbs() != null ? 
                    dbIngredient.getNutrition().getCarbs() : 0.0) * scaleFactor;
                totalFat += (dbIngredient.getNutrition().getFats() != null ? 
                    dbIngredient.getNutrition().getFats() : 0.0) * scaleFactor;
                // Note: Nutrition entity only has calories, protein, carbs, fats
                // Fiber, sugar, sodium not available in current schema
            }
        }

        // Build result
        GeneratedRecipe.NutritionInfo nutrition = new GeneratedRecipe.NutritionInfo();
        nutrition.setCalories(totalCalories);
        nutrition.setProtein(totalProtein);
        nutrition.setCarbohydrates(totalCarbs);
        nutrition.setFat(totalFat);
        nutrition.setFiber(totalFiber);
        nutrition.setSugar(totalSugar);
        nutrition.setSodium(totalSodium);

        // Per serving
        nutrition.setCaloriesPerServing(totalCalories / servings);
        nutrition.setProteinPerServing(totalProtein / servings);
        nutrition.setCarbsPerServing(totalCarbs / servings);
        nutrition.setFatPerServing(totalFat / servings);

        return nutrition;
    }

    /**
     * Find ingredient in database with fuzzy matching.
     */
    private Ingredient findIngredient(GeneratedRecipe.GeneratedIngredient genIng) {
        // Try exact ID match first
        if (genIng.getIngredientId() != null) {
            Optional<Ingredient> byId = ingredientRepository.findByStableId(genIng.getIngredientId());
            if (byId.isPresent()) {
                return byId.get();
            }
        }

        // Try exact label match
        String normalizedName = genIng.getName().toLowerCase().trim();
        Optional<Ingredient> byLabel = ingredientRepository.findByLabel(normalizedName);
        if (byLabel.isPresent()) {
            return byLabel.get();
        }

        // Try partial match (simple contains)
        List<Ingredient> all = ingredientRepository.findAll();
        for (Ingredient ing : all) {
            if (ing.getLabel().contains(normalizedName) || 
                normalizedName.contains(ing.getLabel())) {
                return ing;
            }
        }

        return null;
    }

    /**
     * Convert quantity to standard unit (grams or milliliters).
     */
    private double convertToStandardUnit(Double quantity, String unit, String dbUnit) {
        if (quantity == null) return 0.0;
        if (unit == null) return quantity; // assume already in standard unit

        String normalizedUnit = unit.toLowerCase().trim();
        
        // Direct matches
        if (normalizedUnit.equals("g") || normalizedUnit.equals("gram") || 
            normalizedUnit.equals("grams")) {
            return quantity;
        }
        if (normalizedUnit.equals("ml") || normalizedUnit.equals("milliliter") || 
            normalizedUnit.equals("milliliters")) {
            return quantity;
        }

        // Common conversions
        Map<String, Double> conversions = new HashMap<>();
        conversions.put("kg", 1000.0);
        conversions.put("kilogram", 1000.0);
        conversions.put("l", 1000.0);
        conversions.put("liter", 1000.0);
        conversions.put("cup", 240.0); // ml
        conversions.put("tbsp", 15.0);
        conversions.put("tablespoon", 15.0);
        conversions.put("tsp", 5.0);
        conversions.put("teaspoon", 5.0);
        conversions.put("oz", 28.35); // grams
        conversions.put("ounce", 28.35);

        Double factor = conversions.get(normalizedUnit);
        if (factor != null) {
            return quantity * factor;
        }

        // Default: return as-is
        return quantity;
    }
}
