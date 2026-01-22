package com.ndl.numbers_dont_lie.ai.dto;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Normalized user profile inputs for AI strategy analysis.
 * No PII beyond minimal identifiers; suitable for prompt injection.
 */
public class AiStrategyRequest {
    private String userId;
    private ZoneId timezone;
    private int age;
    private String sex; // "male" | "female" | "other"
    private double heightCm;
    private double weightKg;
    private String goal; // e.g. "lose_weight", "maintain", "gain_muscle"
    private Map<String, Boolean> dietaryPreferences; // e.g. { "vegan": true, "gluten_free": false }
    private List<String> allergies; // e.g. ["peanut", "shellfish"]
    private Map<String, Integer> mealFrequency; // keys: breakfast, lunch, dinner, snacks

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public ZoneId getTimezone() { return timezone; }
    public void setTimezone(ZoneId timezone) { this.timezone = timezone; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getSex() { return sex; }
    public void setSex(String sex) { this.sex = sex; }

    public double getHeightCm() { return heightCm; }
    public void setHeightCm(double heightCm) { this.heightCm = heightCm; }

    public double getWeightKg() { return weightKg; }
    public void setWeightKg(double weightKg) { this.weightKg = weightKg; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public Map<String, Boolean> getDietaryPreferences() { return dietaryPreferences; }
    public void setDietaryPreferences(Map<String, Boolean> dietaryPreferences) { this.dietaryPreferences = dietaryPreferences; }

    public List<String> getAllergies() { return allergies; }
    public void setAllergies(List<String> allergies) { this.allergies = allergies; }

    public Map<String, Integer> getMealFrequency() { return mealFrequency; }
    public void setMealFrequency(Map<String, Integer> mealFrequency) { this.mealFrequency = mealFrequency; }
}
