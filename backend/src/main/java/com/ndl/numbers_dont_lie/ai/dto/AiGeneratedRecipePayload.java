package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;

public class AiGeneratedRecipePayload {
    private String name;
    private String description;
    private String mealType;
    private String cuisine;
    private List<IngredientItem> ingredients;
    private List<String> steps;
    private Nutrition nutrition;

    public AiGeneratedRecipePayload() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public List<IngredientItem> getIngredients() { return ingredients; }
    public void setIngredients(List<IngredientItem> ingredients) { this.ingredients = ingredients; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public Nutrition getNutrition() { return nutrition; }
    public void setNutrition(Nutrition nutrition) { this.nutrition = nutrition; }

    public static class IngredientItem {
        private String name;
        private Double quantity;
        private String unit;

        public IngredientItem() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public static class Nutrition {
        private Double calories;
        private Double protein;
        private Double carbs;
        private Double fats;

        public Nutrition() {}

        public Double getCalories() { return calories; }
        public void setCalories(Double calories) { this.calories = calories; }

        public Double getProtein() { return protein; }
        public void setProtein(Double protein) { this.protein = protein; }

        public Double getCarbs() { return carbs; }
        public void setCarbs(Double carbs) { this.carbs = carbs; }

        public Double getFats() { return fats; }
        public void setFats(Double fats) { this.fats = fats; }
    }
}
