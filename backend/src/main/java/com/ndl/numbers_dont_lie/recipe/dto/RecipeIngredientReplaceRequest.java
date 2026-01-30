package com.ndl.numbers_dont_lie.recipe.dto;

public class RecipeIngredientReplaceRequest {
    private String ingredientName;
    private String newIngredientName;

    public RecipeIngredientReplaceRequest() {}

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getNewIngredientName() {
        return newIngredientName;
    }

    public void setNewIngredientName(String newIngredientName) {
        this.newIngredientName = newIngredientName;
    }
}
