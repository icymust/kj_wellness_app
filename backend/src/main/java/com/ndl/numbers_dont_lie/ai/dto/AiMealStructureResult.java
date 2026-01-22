package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * Output for STEP 4.2: AI Meal Structure Prompt.
 * Contains meal slots with calorie targets and macro focus.
 * Does NOT include recipes, ingredient names, or dish names.
 * 
 * This result will be cached for STEP 4.3 (recipe selection/generation).
 */
public class AiMealStructureResult {
    
    public static class MealSlot {
        private String mealType; // breakfast, lunch, dinner, snack
        private int index; // 0 for main meals, 0-N for snacks
        private int calorieTarget;
        private Map<String, Double> macroFocus; // e.g. { "protein": 0.4, "carbs": 0.35, "fat": 0.25 }
        private String timingNote; // e.g. "Pre-workout", "Post-dinner snack"

        public String getMealType() { return mealType; }
        public void setMealType(String mealType) { this.mealType = mealType; }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }

        public int getCalorieTarget() { return calorieTarget; }
        public void setCalorieTarget(int calorieTarget) { this.calorieTarget = calorieTarget; }

        public Map<String, Double> getMacroFocus() { return macroFocus; }
        public void setMacroFocus(Map<String, Double> macroFocus) { this.macroFocus = macroFocus; }

        public String getTimingNote() { return timingNote; }
        public void setTimingNote(String timingNote) { this.timingNote = timingNote; }
    }

    private List<MealSlot> meals;
    private int totalCaloriesDistributed;

    public List<MealSlot> getMeals() { return meals; }
    public void setMeals(List<MealSlot> meals) { this.meals = meals; }

    public int getTotalCaloriesDistributed() { return totalCaloriesDistributed; }
    public void setTotalCaloriesDistributed(int totalCaloriesDistributed) { 
        this.totalCaloriesDistributed = totalCaloriesDistributed; 
    }
}
