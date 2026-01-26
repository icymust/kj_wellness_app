package com.ndl.numbers_dont_lie.mealplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

/**
 * Request DTO for adding a custom meal to a day plan.
 * 
 * Design intent:
 * - Minimal input: just the meal name, type, and date
 * - No recipeId, no nutrition data required
 * - Custom meals are isolated and don't affect generation logic
 */
public class AddCustomMealRequest {

    @JsonProperty("date")
    private LocalDate date; // YYYY-MM-DD

    @JsonProperty("meal_type")
    private String mealType; // breakfast|lunch|dinner|snack

    @JsonProperty("name")
    private String name; // Custom meal name (e.g., "Homemade Pizza")

    public AddCustomMealRequest() {
    }

    public AddCustomMealRequest(LocalDate date, String mealType, String name) {
        this.date = date;
        this.mealType = mealType;
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AddCustomMealRequest{" +
                "date=" + date +
                ", mealType='" + mealType + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
