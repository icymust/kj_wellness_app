package com.ndl.numbers_dont_lie.ai.dto;

public class AiRecipeGenerateRequest {
    private Long userId;
    private String mealType;
    private Long mealId;

    public AiRecipeGenerateRequest() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public Long getMealId() {
        return mealId;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }
}
