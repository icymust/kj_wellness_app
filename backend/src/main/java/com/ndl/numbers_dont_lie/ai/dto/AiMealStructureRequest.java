package com.ndl.numbers_dont_lie.ai.dto;

import java.time.LocalTime;
import java.util.Map;

/**
 * Input for STEP 4.2: AI Meal Structure Prompt.
 * Depends on STEP 4.1 AiStrategyResult (cached).
 * 
 * This DTO provides context for distributing daily calories across meal slots
 * without generating recipes or naming dishes.
 */
public class AiMealStructureRequest {
    private String userId;
    private AiStrategyResult strategyResult; // from STEP 4.1
    private Map<String, Integer> mealFrequency; // e.g. { "breakfast": 1, "lunch": 1, "dinner": 1, "snacks": 2 }
    private Map<String, LocalTime> mealTimingPreferences; // e.g. { "breakfast": 07:30, "lunch": 12:30, "dinner": 19:00 }
    private int dailyCalorieTarget; // from strategy result or override

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public AiStrategyResult getStrategyResult() { return strategyResult; }
    public void setStrategyResult(AiStrategyResult strategyResult) { this.strategyResult = strategyResult; }

    public Map<String, Integer> getMealFrequency() { return mealFrequency; }
    public void setMealFrequency(Map<String, Integer> mealFrequency) { this.mealFrequency = mealFrequency; }

    public Map<String, LocalTime> getMealTimingPreferences() { return mealTimingPreferences; }
    public void setMealTimingPreferences(Map<String, LocalTime> mealTimingPreferences) { 
        this.mealTimingPreferences = mealTimingPreferences; 
    }

    public int getDailyCalorieTarget() { return dailyCalorieTarget; }
    public void setDailyCalorieTarget(int dailyCalorieTarget) { this.dailyCalorieTarget = dailyCalorieTarget; }
}
