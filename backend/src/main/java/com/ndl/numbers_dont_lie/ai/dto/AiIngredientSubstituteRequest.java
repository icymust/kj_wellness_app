package com.ndl.numbers_dont_lie.ai.dto;

public class AiIngredientSubstituteRequest {
    private Long recipeId;
    private String ingredientName;
    private String availableIngredients;

    public AiIngredientSubstituteRequest() {}

    public Long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Long recipeId) {
        this.recipeId = recipeId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getAvailableIngredients() {
        return availableIngredients;
    }

    public void setAvailableIngredients(String availableIngredients) {
        this.availableIngredients = availableIngredients;
    }
}
