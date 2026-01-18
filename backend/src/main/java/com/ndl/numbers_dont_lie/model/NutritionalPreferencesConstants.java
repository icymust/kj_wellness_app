package com.ndl.numbers_dont_lie.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NutritionalPreferencesConstants {

    public static final Set<String> AVAILABLE_DIETARY_PREFERENCES = new HashSet<>(Arrays.asList(
            "vegetarian", "vegan", "keto", "paleo", "pescatarian", "low-carb", 
            "gluten-free", "dairy-free", "halal", "kosher", "organic", 
            "low-sodium", "high-protein", "mediterranean", "intermittent-fasting"
    ));

    public static final Set<String> AVAILABLE_ALLERGIES = new HashSet<>(Arrays.asList(
            "dairy", "eggs", "fish", "shellfish", "tree-nuts", "peanuts", 
            "wheat", "soy", "gluten", "sesame"
    ));

    public static boolean isValidDietaryPreference(String preference) {
        return AVAILABLE_DIETARY_PREFERENCES.contains(preference);
    }

    public static boolean isValidAllergy(String allergen) {
        return AVAILABLE_ALLERGIES.contains(allergen);
    }
}
