package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines the duration scope of a meal plan.
 */
public enum PlanDuration {
    DAILY("daily"),
    WEEKLY("weekly");

    private final String jsonValue;

    PlanDuration(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
