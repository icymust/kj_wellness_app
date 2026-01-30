package com.ndl.numbers_dont_lie.ai.dto;

public class AiNutritionSummaryResponse {
    private String summary;

    public AiNutritionSummaryResponse() {}

    public AiNutritionSummaryResponse(String summary) {
        this.summary = summary;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
