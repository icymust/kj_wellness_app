package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI-generated recipe output for STEP 4.3.2.
 * 
 * Structure matches Recipe entity schema but uses simpler types
 * suitable for AI generation and JSON serialization.
 * 
 * IMPORTANT:
 * - Initial generation does NOT include nutrition
 * - Nutrition is calculated via function calling (calculate_nutrition)
 * - AI embeds nutrition into final output after function returns
 */
public class GeneratedRecipe {
    
    private String title;
    private String cuisine;
    private String meal; // breakfast | lunch | dinner | snack
    private Integer servings;
    private String summary;
    private Integer timeMinutes;
    private String difficultyLevel; // easy | medium | hard
    private List<String> dietaryTags; // e.g. ["vegan", "gluten_free", "high_protein"]
    
    private List<GeneratedIngredient> ingredients;
    private List<PreparationStep> preparationSteps;
    
    // Calculated via function calling
    private NutritionInfo nutrition;

    public static class GeneratedIngredient {
        private String ingredientId; // optional: match to ingredient DB
        private String name;
        private Double quantity; // grams or ml
        private String unit; // g | ml | piece | cup | tbsp, etc.

        public String getIngredientId() { return ingredientId; }
        public void setIngredientId(String ingredientId) { this.ingredientId = ingredientId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class PreparationStep {
        private Integer stepNumber;
        private String stepTitle;
        private String instruction;

        public Integer getStepNumber() { return stepNumber; }
        public void setStepNumber(Integer stepNumber) { this.stepNumber = stepNumber; }

        public String getStepTitle() { return stepTitle; }
        public void setStepTitle(String stepTitle) { this.stepTitle = stepTitle; }

        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }
    }

    public static class NutritionInfo {
        private Double calories;
        private Double protein;
        private Double carbohydrates;
        private Double fat;
        private Double fiber;
        private Double sugar;
        private Double sodium;
        
        // Per serving
        private Double caloriesPerServing;
        private Double proteinPerServing;
        private Double carbsPerServing;
        private Double fatPerServing;

        public Double getCalories() { return calories; }
        public void setCalories(Double calories) { this.calories = calories; }

        public Double getProtein() { return protein; }
        public void setProtein(Double protein) { this.protein = protein; }

        public Double getCarbohydrates() { return carbohydrates; }
        public void setCarbohydrates(Double carbohydrates) { this.carbohydrates = carbohydrates; }

        public Double getFat() { return fat; }
        public void setFat(Double fat) { this.fat = fat; }

        public Double getFiber() { return fiber; }
        public void setFiber(Double fiber) { this.fiber = fiber; }

        public Double getSugar() { return sugar; }
        public void setSugar(Double sugar) { this.sugar = sugar; }

        public Double getSodium() { return sodium; }
        public void setSodium(Double sodium) { this.sodium = sodium; }

        public Double getCaloriesPerServing() { return caloriesPerServing; }
        public void setCaloriesPerServing(Double caloriesPerServing) { 
            this.caloriesPerServing = caloriesPerServing; 
        }

        public Double getProteinPerServing() { return proteinPerServing; }
        public void setProteinPerServing(Double proteinPerServing) { 
            this.proteinPerServing = proteinPerServing; 
        }

        public Double getCarbsPerServing() { return carbsPerServing; }
        public void setCarbsPerServing(Double carbsPerServing) { 
            this.carbsPerServing = carbsPerServing; 
        }

        public Double getFatPerServing() { return fatPerServing; }
        public void setFatPerServing(Double fatPerServing) { this.fatPerServing = fatPerServing; }
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public String getMeal() { return meal; }
    public void setMeal(String meal) { this.meal = meal; }

    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Integer getTimeMinutes() { return timeMinutes; }
    public void setTimeMinutes(Integer timeMinutes) { this.timeMinutes = timeMinutes; }

    public String getDifficultyLevel() { return difficultyLevel; }
    public void setDifficultyLevel(String difficultyLevel) { this.difficultyLevel = difficultyLevel; }

    public List<String> getDietaryTags() { return dietaryTags; }
    public void setDietaryTags(List<String> dietaryTags) { this.dietaryTags = dietaryTags; }

    public List<GeneratedIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<GeneratedIngredient> ingredients) { this.ingredients = ingredients; }

    public List<PreparationStep> getPreparationSteps() { return preparationSteps; }
    public void setPreparationSteps(List<PreparationStep> preparationSteps) { 
        this.preparationSteps = preparationSteps; 
    }

    public NutritionInfo getNutrition() { return nutrition; }
    public void setNutrition(NutritionInfo nutrition) { this.nutrition = nutrition; }
}
