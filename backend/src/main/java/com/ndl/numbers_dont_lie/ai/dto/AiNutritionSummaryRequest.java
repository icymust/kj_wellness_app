package com.ndl.numbers_dont_lie.ai.dto;

public class AiNutritionSummaryRequest {
    private Long userId;
    private String date;
    private NutritionSummary nutritionSummary;
    private String userGoal;

    public AiNutritionSummaryRequest() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public NutritionSummary getNutritionSummary() {
        return nutritionSummary;
    }

    public void setNutritionSummary(NutritionSummary nutritionSummary) {
        this.nutritionSummary = nutritionSummary;
    }

    public String getUserGoal() {
        return userGoal;
    }

    public void setUserGoal(String userGoal) {
        this.userGoal = userGoal;
    }

    public static class NutritionSummary {
        private Double calories;
        private Double targetCalories;
        private Double protein;
        private Double targetProtein;
        private Double carbs;
        private Double targetCarbs;
        private Double fats;
        private Double targetFats;
        private Boolean nutritionEstimated;

        public NutritionSummary() {}

        public Double getCalories() {
            return calories;
        }

        public void setCalories(Double calories) {
            this.calories = calories;
        }

        public Double getTargetCalories() {
            return targetCalories;
        }

        public void setTargetCalories(Double targetCalories) {
            this.targetCalories = targetCalories;
        }

        public Double getProtein() {
            return protein;
        }

        public void setProtein(Double protein) {
            this.protein = protein;
        }

        public Double getTargetProtein() {
            return targetProtein;
        }

        public void setTargetProtein(Double targetProtein) {
            this.targetProtein = targetProtein;
        }

        public Double getCarbs() {
            return carbs;
        }

        public void setCarbs(Double carbs) {
            this.carbs = carbs;
        }

        public Double getTargetCarbs() {
            return targetCarbs;
        }

        public void setTargetCarbs(Double targetCarbs) {
            this.targetCarbs = targetCarbs;
        }

        public Double getFats() {
            return fats;
        }

        public void setFats(Double fats) {
            this.fats = fats;
        }

        public Double getTargetFats() {
            return targetFats;
        }

        public void setTargetFats(Double targetFats) {
            this.targetFats = targetFats;
        }

        public Boolean getNutritionEstimated() {
            return nutritionEstimated;
        }

        public void setNutritionEstimated(Boolean nutritionEstimated) {
            this.nutritionEstimated = nutritionEstimated;
        }
    }
}
