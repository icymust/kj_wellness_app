package com.ndl.numbers_dont_lie.mealplan.dto;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import java.time.LocalDate;
import java.util.List;

public class WeeklyPlanResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DayPlan> days;
    private WeeklyNutritionSummary weeklyNutrition;

    public WeeklyPlanResponse() {}

    public WeeklyPlanResponse(LocalDate startDate, LocalDate endDate, List<DayPlan> days, WeeklyNutritionSummary weeklyNutrition) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
        this.weeklyNutrition = weeklyNutrition;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<DayPlan> getDays() { return days; }
    public void setDays(List<DayPlan> days) { this.days = days; }

    public WeeklyNutritionSummary getWeeklyNutrition() { return weeklyNutrition; }
    public void setWeeklyNutrition(WeeklyNutritionSummary weeklyNutrition) { this.weeklyNutrition = weeklyNutrition; }
}
