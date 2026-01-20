package com.ndl.numbers_dont_lie.recipe.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing meal types.
 * Maps to lowercase JSON values for API serialization.
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

    /**
     * Convert a string value to MealType enum.
     * Handles both uppercase enum names and lowercase JSON values.
     */
    public static MealType fromString(String value) {
        if (value == null || value.isEmpty()) {
            return BREAKFAST; // default fallback
        }
        
        String lowerValue = value.toLowerCase().trim();
        for (MealType type : MealType.values()) {
            if (type.jsonValue.equals(lowerValue) || type.name().equalsIgnoreCase(lowerValue)) {
                return type;
            }
        }
        return BREAKFAST; // default fallback for unrecognized values
    }
}
