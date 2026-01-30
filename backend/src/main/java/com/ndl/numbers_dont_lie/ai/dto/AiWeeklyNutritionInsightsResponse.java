package com.ndl.numbers_dont_lie.ai.dto;

public class AiWeeklyNutritionInsightsResponse {
    private String summary;

    public AiWeeklyNutritionInsightsResponse() {}

    public AiWeeklyNutritionInsightsResponse(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
