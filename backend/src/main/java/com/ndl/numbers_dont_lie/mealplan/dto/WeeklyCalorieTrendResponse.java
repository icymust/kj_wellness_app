package com.ndl.numbers_dont_lie.mealplan.dto;

import java.time.LocalDate;
import java.util.List;

public class WeeklyCalorieTrendResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private List<DayTrend> days;

    public WeeklyCalorieTrendResponse() {}

    public WeeklyCalorieTrendResponse(LocalDate startDate, LocalDate endDate, List<DayTrend> days) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.days = days;
    }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public List<DayTrend> getDays() { return days; }
    public void setDays(List<DayTrend> days) { this.days = days; }

    public static class DayTrend {
        private LocalDate date;
        private int actualCalories;
        private int targetCalories;
        private int delta;

        public DayTrend() {}

        public DayTrend(LocalDate date, int actualCalories, int targetCalories, int delta) {
            this.date = date;
            this.actualCalories = actualCalories;
            this.targetCalories = targetCalories;
            this.delta = delta;
        }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public int getActualCalories() { return actualCalories; }
        public void setActualCalories(int actualCalories) { this.actualCalories = actualCalories; }

        public int getTargetCalories() { return targetCalories; }
        public void setTargetCalories(int targetCalories) { this.targetCalories = targetCalories; }

        public int getDelta() { return delta; }
        public void setDelta(int delta) { this.delta = delta; }
    }
}
