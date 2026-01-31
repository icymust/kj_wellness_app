package com.ndl.numbers_dont_lie.shoppinglist.dto;

import java.util.List;

public class MealShoppingListResponse {
    private Long mealId;
    private String mealType;
    private String mealName;
    private List<ShoppingListItemDto> items;

    public MealShoppingListResponse() {
    }

    public MealShoppingListResponse(Long mealId, String mealType, String mealName, List<ShoppingListItemDto> items) {
        this.mealId = mealId;
        this.mealType = mealType;
        this.mealName = mealName;
        this.items = items;
    }

    public Long getMealId() {
        return mealId;
    }

    public void setMealId(Long mealId) {
        this.mealId = mealId;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public List<ShoppingListItemDto> getItems() {
        return items;
    }

    public void setItems(List<ShoppingListItemDto> items) {
        this.items = items;
    }
}
