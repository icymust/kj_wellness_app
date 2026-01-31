package com.ndl.numbers_dont_lie.shoppinglist.dto;

public class ShoppingListItemDto {
    private String ingredient;
    private Double totalQuantity;
    private String unit;
    private String category;

    public ShoppingListItemDto() {}

    public ShoppingListItemDto(String ingredient, Double totalQuantity, String unit) {
        this(ingredient, totalQuantity, unit, null);
    }

    public ShoppingListItemDto(String ingredient, Double totalQuantity, String unit, String category) {
        this.ingredient = ingredient;
        this.totalQuantity = totalQuantity;
        this.unit = unit;
        this.category = category;
    }

    public String getIngredient() {
        return ingredient;
    }

    public void setIngredient(String ingredient) {
        this.ingredient = ingredient;
    }

    public Double getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Double totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
