package com.ndl.numbers_dont_lie.dto.nutrition;

import java.time.LocalDateTime;
import java.util.Set;

public class NutritionalPreferencesDto {
    public Set<String> dietaryPreferences;
    public Set<String> allergies;
    public LocalDateTime updatedAt;

    public NutritionalPreferencesDto() {}

    public NutritionalPreferencesDto(Set<String> dietaryPreferences, Set<String> allergies, LocalDateTime updatedAt) {
        this.dietaryPreferences = dietaryPreferences;
        this.allergies = allergies;
        this.updatedAt = updatedAt;
    }
}
