package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Structured query for recipe retrieval via RAG.
 * 
 * Used in STEP 4.3: convert user preferences → embedding → retrieve relevant recipes.
 * Does NOT trigger recipe generation or nutrition calculation.
 */
public class RecipeQuery {
    private List<String> cuisinePreferences; // e.g. ["Italian", "Mexican"]
    private List<String> dietaryRestrictions; // e.g. ["vegan", "gluten_free", "no_peanuts"]
    private String mealType; // breakfast | lunch | dinner | snack
    private Map<String, Double> macroFocus; // e.g. { "protein": 0.4, "carbs": 0.35, "fat": 0.25 }
    private Integer maxTimeMinutes; // optional time constraint
    private String freeTextQuery; // optional additional search terms

    public List<String> getCuisinePreferences() { return cuisinePreferences; }
    public void setCuisinePreferences(List<String> cuisinePreferences) { 
        this.cuisinePreferences = cuisinePreferences; 
    }

    public List<String> getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(List<String> dietaryRestrictions) { 
        this.dietaryRestrictions = dietaryRestrictions; 
    }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public Map<String, Double> getMacroFocus() { return macroFocus; }
    public void setMacroFocus(Map<String, Double> macroFocus) { this.macroFocus = macroFocus; }

    public Integer getMaxTimeMinutes() { return maxTimeMinutes; }
    public void setMaxTimeMinutes(Integer maxTimeMinutes) { this.maxTimeMinutes = maxTimeMinutes; }

    public String getFreeTextQuery() { return freeTextQuery; }
    public void setFreeTextQuery(String freeTextQuery) { this.freeTextQuery = freeTextQuery; }
}
