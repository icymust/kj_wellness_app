package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;

public class AiWeeklyNutritionInsightsRequest {
    private Long userId;
    private String startDate;
    private String endDate;
    private WeeklyNutrition weeklyNutrition;
    private List<DailySummary> dailySummaries;
    private String userGoal;

    public AiWeeklyNutritionInsightsRequest() {}

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public WeeklyNutrition getWeeklyNutrition() {
        return weeklyNutrition;
    }

    public void setWeeklyNutrition(WeeklyNutrition weeklyNutrition) {
        this.weeklyNutrition = weeklyNutrition;
    }

    public List<DailySummary> getDailySummaries() {
        return dailySummaries;
    }

    public void setDailySummaries(List<DailySummary> dailySummaries) {
        this.dailySummaries = dailySummaries;
    }

    public String getUserGoal() {
        return userGoal;
    }

    public void setUserGoal(String userGoal) {
        this.userGoal = userGoal;
    }

    public static class WeeklyNutrition {
        private Double totalCalories;
        private Double targetCalories;
        private Double totalProtein;
        private Double targetProtein;
        private Double totalCarbs;
        private Double targetCarbs;
        private Double totalFats;
        private Double targetFats;
        private Boolean nutritionEstimated;

        public WeeklyNutrition() {}

        public Double getTotalCalories() {
            return totalCalories;
        }

        public void setTotalCalories(Double totalCalories) {
            this.totalCalories = totalCalories;
        }

        public Double getTargetCalories() {
            return targetCalories;
        }

        public void setTargetCalories(Double targetCalories) {
            this.targetCalories = targetCalories;
        }

        public Double getTotalProtein() {
            return totalProtein;
        }

        public void setTotalProtein(Double totalProtein) {
            this.totalProtein = totalProtein;
        }

        public Double getTargetProtein() {
            return targetProtein;
        }

        public void setTargetProtein(Double targetProtein) {
            this.targetProtein = targetProtein;
        }

        public Double getTotalCarbs() {
            return totalCarbs;
        }

        public void setTotalCarbs(Double totalCarbs) {
            this.totalCarbs = totalCarbs;
        }

        public Double getTargetCarbs() {
            return targetCarbs;
        }

        public void setTargetCarbs(Double targetCarbs) {
            this.targetCarbs = targetCarbs;
        }

        public Double getTotalFats() {
            return totalFats;
        }

        public void setTotalFats(Double totalFats) {
            this.totalFats = totalFats;
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

    public static class DailySummary {
        private String date;
        private Double calories;
        private Double targetCalories;

        public DailySummary() {}

        public DailySummary(String date, Double calories, Double targetCalories) {
            this.date = date;
            this.calories = calories;
            this.targetCalories = targetCalories;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

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
    }
}
