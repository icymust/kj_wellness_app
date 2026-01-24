package com.ndl.numbers_dont_lie.mealplan.dto;

public class WeeklyNutritionSummary {
    private double totalCalories;
    private double totalProtein;
    private double totalCarbs;
    private double totalFats;
    private double targetCalories;
    private double targetProtein;
    private double targetCarbs;
    private double targetFats;
    private double caloriesPercentage;
    private double proteinPercentage;
    private double carbsPercentage;
    private double fatsPercentage;
    private boolean nutritionEstimated;

    public double getTotalCalories() { return totalCalories; }
    public void setTotalCalories(double totalCalories) { this.totalCalories = totalCalories; }

    public double getTotalProtein() { return totalProtein; }
    public void setTotalProtein(double totalProtein) { this.totalProtein = totalProtein; }

    public double getTotalCarbs() { return totalCarbs; }
    public void setTotalCarbs(double totalCarbs) { this.totalCarbs = totalCarbs; }

    public double getTotalFats() { return totalFats; }
    public void setTotalFats(double totalFats) { this.totalFats = totalFats; }

    public double getTargetCalories() { return targetCalories; }
    public void setTargetCalories(double targetCalories) { this.targetCalories = targetCalories; }

    public double getTargetProtein() { return targetProtein; }
    public void setTargetProtein(double targetProtein) { this.targetProtein = targetProtein; }

    public double getTargetCarbs() { return targetCarbs; }
    public void setTargetCarbs(double targetCarbs) { this.targetCarbs = targetCarbs; }

    public double getTargetFats() { return targetFats; }
    public void setTargetFats(double targetFats) { this.targetFats = targetFats; }

    public double getCaloriesPercentage() { return caloriesPercentage; }
    public void setCaloriesPercentage(double caloriesPercentage) { this.caloriesPercentage = caloriesPercentage; }

    public double getProteinPercentage() { return proteinPercentage; }
    public void setProteinPercentage(double proteinPercentage) { this.proteinPercentage = proteinPercentage; }

    public double getCarbsPercentage() { return carbsPercentage; }
    public void setCarbsPercentage(double carbsPercentage) { this.carbsPercentage = carbsPercentage; }

    public double getFatsPercentage() { return fatsPercentage; }
    public void setFatsPercentage(double fatsPercentage) { this.fatsPercentage = fatsPercentage; }

    public boolean isNutritionEstimated() { return nutritionEstimated; }
    public void setNutritionEstimated(boolean nutritionEstimated) { this.nutritionEstimated = nutritionEstimated; }
}
