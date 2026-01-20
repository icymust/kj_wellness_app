package com.ndl.numbers_dont_lie.recipe.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing difficulty levels for recipes.
 * Maps to lowercase JSON values for API serialization.
 */
public enum DifficultyLevel {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String jsonValue;

    DifficultyLevel(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }

    /**
     * Convert a string value to DifficultyLevel enum.
     * Handles both uppercase enum names and lowercase JSON values.
     */
    public static DifficultyLevel fromString(String value) {
        if (value == null || value.isEmpty()) {
            return MEDIUM; // default fallback
        }
        
        String lowerValue = value.toLowerCase().trim();
        for (DifficultyLevel level : DifficultyLevel.values()) {
            if (level.jsonValue.equals(lowerValue) || level.name().equalsIgnoreCase(lowerValue)) {
                return level;
            }
        }
        return MEDIUM; // default fallback for unrecognized values
    }
}
