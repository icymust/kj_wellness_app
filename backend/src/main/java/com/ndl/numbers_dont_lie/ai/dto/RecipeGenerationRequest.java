package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Input for STEP 4.3.2: AI Recipe Generation with RAG + Function Calling.
 * 
 * Bundles all context needed to generate a recipe:
 * - Strategy from STEP 4.1
 * - Meal slot from STEP 4.2
 * - Retrieved recipes from STEP 4.3.1 (RAG context)
 * - User dietary restrictions and preferences
 * 
 * This context enables:
 * - RAG: AI adapts real recipes rather than inventing
 * - Function calling: Nutrition is calculated, not guessed
 */
public class RecipeGenerationRequest {
    private String userId;
    
    // Context from previous steps
    private AiStrategyResult strategy; // STEP 4.1
    private AiMealStructureResult.MealSlot mealSlot; // STEP 4.2: specific meal to generate
    private List<RetrievedRecipe> retrievedRecipes; // STEP 4.3.1: RAG context
    
    // User constraints
    private List<String> dietaryRestrictions; // e.g. ["vegan", "gluten_free", "no_peanuts"]
    private List<String> allergies; // e.g. ["peanut", "shellfish"]
    private Map<String, Boolean> dietaryPreferences; // e.g. { "high_protein": true }
    
    // Optional overrides
    private Integer targetCalories; // override from meal slot if needed
    private Integer servings; // default servings

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public AiStrategyResult getStrategy() { return strategy; }
    public void setStrategy(AiStrategyResult strategy) { this.strategy = strategy; }

    public AiMealStructureResult.MealSlot getMealSlot() { return mealSlot; }
    public void setMealSlot(AiMealStructureResult.MealSlot mealSlot) { this.mealSlot = mealSlot; }

    public List<RetrievedRecipe> getRetrievedRecipes() { return retrievedRecipes; }
    public void setRetrievedRecipes(List<RetrievedRecipe> retrievedRecipes) { 
        this.retrievedRecipes = retrievedRecipes; 
    }

    public List<String> getDietaryRestrictions() { return dietaryRestrictions; }
    public void setDietaryRestrictions(List<String> dietaryRestrictions) { 
        this.dietaryRestrictions = dietaryRestrictions; 
    }

    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }

    public Map<String, Boolean> getDietaryPreferences() { return dietaryPreferences; }
    public void setDietaryPreferences(Map<String, Boolean> dietaryPreferences) { 
        this.dietaryPreferences = dietaryPreferences; 
    }

    public Integer getTargetCalories() { return targetCalories; }
    public void setTargetCalories(Integer targetCalories) { this.targetCalories = targetCalories; }

    public Integer getServings() { return servings; }
    public void setServings(Integer servings) { this.servings = servings; }
}
