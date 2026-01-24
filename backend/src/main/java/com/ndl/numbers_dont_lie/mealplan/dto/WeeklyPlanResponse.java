package com.ndl.numbers_dont_lie.mealplan.dto;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import java.util.List;

public class WeeklyPlanResponse {
    private List<DayPlan> dayPlans;
    private WeeklyNutritionSummary weeklyNutrition;

    public WeeklyPlanResponse() {}

    public WeeklyPlanResponse(List<DayPlan> dayPlans, WeeklyNutritionSummary weeklyNutrition) {
        this.dayPlans = dayPlans;
        this.weeklyNutrition = weeklyNutrition;
    }

    public List<DayPlan> getDayPlans() { return dayPlans; }
    public void setDayPlans(List<DayPlan> dayPlans) { this.dayPlans = dayPlans; }

    public WeeklyNutritionSummary getWeeklyNutrition() { return weeklyNutrition; }
    public void setWeeklyNutrition(WeeklyNutritionSummary weeklyNutrition) { this.weeklyNutrition = weeklyNutrition; }
}
