package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Tracks the reason for creating a new meal plan version.
 * Used to maintain audit trail and understand plan evolution.
 */
public enum VersionReason {
    INITIAL_GENERATION("initial_generation"),
    REGENERATED("regenerated"),
    MANUAL_CHANGE("manual_change");

    private final String jsonValue;

    VersionReason(String jsonValue) {
        this.jsonValue = jsonValue;
    }

    @JsonValue
    public String getJsonValue() {
        return jsonValue;
    }
}
