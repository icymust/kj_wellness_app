package com.ndl.numbers_dont_lie.recipe.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;

@Embeddable
public class Nutrition {

    @JsonProperty("calories")
    private Double calories; // kcal per 100g/ml

    @JsonProperty("protein")
    private Double protein; // grams per 100g/ml

    @JsonProperty("carbs")
    private Double carbs; // grams per 100g/ml

    @JsonProperty("fats")
    private Double fats; // grams per 100g/ml

    public Nutrition() {
    }

    public Nutrition(Double calories, Double protein, Double carbs, Double fats) {
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    public Double getCalories() {
        return calories;
    }

    public void setCalories(Double calories) {
        this.calories = calories;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

    public Double getCarbs() {
        return carbs;
    }

    public void setCarbs(Double carbs) {
        this.carbs = carbs;
    }

    public Double getFats() {
        return fats;
    }

    public void setFats(Double fats) {
        this.fats = fats;
    }
}
