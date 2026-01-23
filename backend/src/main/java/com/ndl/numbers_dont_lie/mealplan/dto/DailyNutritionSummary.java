package com.ndl.numbers_dont_lie.mealplan.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DEBUG visualization DTO for STEP 6.3.1 - Nutrition Summary Backend.
 * 
 * Purpose: Present aggregated daily nutrition data with progress towards user targets.
 * 
 * Design Intent:
 * - Aggregates nutrition from all meals in a DayPlan
 * - Compares totals against user-defined nutritional targets
 * - Provides percentage calculations for visualization
 * - NO AI involvement - pure data aggregation
 * 
 * Usage Context:
 * - Debug endpoints for nutritional validation
 * - Visual inspection of daily intake
 * - Progress tracking against goals
 * 
 * IMPORTANT: Debug-only DTO for visualization; not production logic.
 * Production implementation would include:
 * - Historical tracking
 * - Trend analysis
 * - Recommendations
 * - More granular meal breakdown
 */
public class DailyNutritionSummary {

    @JsonProperty("date")
    private java.time.LocalDate date;

    @JsonProperty("total_calories")
    private double totalCalories;

    @JsonProperty("total_protein")
    private double totalProtein;

    @JsonProperty("total_carbs")
    private double totalCarbs;

    @JsonProperty("total_fats")
    private double totalFats;

    @JsonProperty("target_calories")
    private double targetCalories;

    @JsonProperty("target_protein")
    private double targetProtein;

    @JsonProperty("target_carbs")
    private double targetCarbs;

    @JsonProperty("target_fats")
    private double targetFats;

    @JsonProperty("calories_percentage")
    private double caloriesPercentage;

    @JsonProperty("protein_percentage")
    private double proteinPercentage;

    @JsonProperty("carbs_percentage")
    private double carbsPercentage;

    @JsonProperty("fats_percentage")
    private double fatsPercentage;

    public DailyNutritionSummary() {
    }

    public java.time.LocalDate getDate() {
        return date;
    }

    public void setDate(java.time.LocalDate date) {
        this.date = date;
    }

    public double getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(double totalCalories) {
        this.totalCalories = totalCalories;
    }

    public double getTotalProtein() {
        return totalProtein;
    }

    public void setTotalProtein(double totalProtein) {
        this.totalProtein = totalProtein;
    }

    public double getTotalCarbs() {
        return totalCarbs;
    }

    public void setTotalCarbs(double totalCarbs) {
        this.totalCarbs = totalCarbs;
    }

    public double getTotalFats() {
        return totalFats;
    }

    public void setTotalFats(double totalFats) {
        this.totalFats = totalFats;
    }

    public double getTargetCalories() {
        return targetCalories;
    }

    public void setTargetCalories(double targetCalories) {
        this.targetCalories = targetCalories;
    }

    public double getTargetProtein() {
        return targetProtein;
    }

    public void setTargetProtein(double targetProtein) {
        this.targetProtein = targetProtein;
    }

    public double getTargetCarbs() {
        return targetCarbs;
    }

    public void setTargetCarbs(double targetCarbs) {
        this.targetCarbs = targetCarbs;
    }

    public double getTargetFats() {
        return targetFats;
    }

    public void setTargetFats(double targetFats) {
        this.targetFats = targetFats;
    }

    public double getCaloriesPercentage() {
        return caloriesPercentage;
    }

    public void setCaloriesPercentage(double caloriesPercentage) {
        this.caloriesPercentage = caloriesPercentage;
    }

    public double getProteinPercentage() {
        return proteinPercentage;
    }

    public void setProteinPercentage(double proteinPercentage) {
        this.proteinPercentage = proteinPercentage;
    }

    public double getCarbsPercentage() {
        return carbsPercentage;
    }

    public void setCarbsPercentage(double carbsPercentage) {
        this.carbsPercentage = carbsPercentage;
    }

    public double getFatsPercentage() {
        return fatsPercentage;
    }

    public void setFatsPercentage(double fatsPercentage) {
        this.fatsPercentage = fatsPercentage;
    }
}
