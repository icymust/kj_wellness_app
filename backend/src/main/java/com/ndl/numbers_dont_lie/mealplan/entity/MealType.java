package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines the type/slot of a meal within a day plan.
 * Used to categorize meals by time of day.
 */
public enum MealType {
    BREAKFAST("breakfast"),
    LUNCH("lunch"),
    DINNER("dinner"),
    SNACK("snack");

    private final String jsonValue;

    MealType(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
