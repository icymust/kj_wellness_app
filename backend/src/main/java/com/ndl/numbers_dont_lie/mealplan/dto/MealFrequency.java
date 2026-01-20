package com.ndl.numbers_dont_lie.mealplan.dto;

import com.ndl.numbers_dont_lie.mealplan.entity.MealType;

import java.util.HashMap;
import java.util.Map;

/**
 * Data transfer object representing user's meal frequency preferences.
 * Specifies how many meals of each type should be planned per day.
 * 
 * ARCHITECTURAL DESIGN:
 * - This is a DERIVED value object, not a persistent entity
 * - User Profile (Project 1) is the single source of truth for meal frequency
 * - This DTO avoids duplicate data entry by transforming User Profile data
 * - Production code must NOT create ad-hoc instances
 * - Must be constructed from User Profile data via service layer
 * 
 * Example:
 * - 1 breakfast, 1 lunch, 1 dinner, 2 snacks
 */
public class MealFrequency {
    
    private final Map<MealType, Integer> frequency;

    public MealFrequency() {
        this.frequency = new HashMap<>();
    }

    public MealFrequency(Map<MealType, Integer> frequency) {
        this.frequency = new HashMap<>(frequency);
    }

    public int getCount(MealType mealType) {
        return frequency.getOrDefault(mealType, 0);
    }

    public void setCount(MealType mealType, int count) {
        if (count > 0) {
            frequency.put(mealType, count);
        } else {
            frequency.remove(mealType);
        }
    }

    public Map<MealType, Integer> getFrequency() {
        return new HashMap<>(frequency);
    }

    /**
    * Factory method for default meal frequency.
    * Returns: 1 breakfast, 1 lunch, 1 dinner, 0 snacks
    * 
    * @deprecated For tests only. Must not be used in production flow.
    * Production code must derive meal frequency from User Profile (Project 1).
    */
    @Deprecated(since = "1.0", forRemoval = false)
    public static MealFrequency defaultFrequency() {
        Map<MealType, Integer> defaults = new HashMap<>();
        defaults.put(MealType.BREAKFAST, 1);
        defaults.put(MealType.LUNCH, 1);
        defaults.put(MealType.DINNER, 1);
        return new MealFrequency(defaults);
    }

    /**
     * Calculate total meals per day.
     */
    public int totalMealsPerDay() {
        return frequency.values().stream().mapToInt(Integer::intValue).sum();
    }
}
