package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * AI strategy output with strictly JSON-only fields. No free-form text beyond rationale strings.
 */
public class AiStrategyResult {
    private String strategyName; // concise name, e.g. "Moderate Deficit High-Protein"
    private String rationale; // brief explanation from the model
    private Map<String, Integer> targetCalories; // e.g. { "daily": 2200, "breakfast": 500 }
    private Map<String, Double> macroSplit; // e.g. { "protein": 0.3, "carbs": 0.45, "fat": 0.25 }
    private List<String> constraints; // e.g. ["no_peanuts", "limit_sugar"]
    private List<String> recommendations; // e.g. ["increase_fiber", "hydrate"]

    public String getStrategyName() { return strategyName; }
    public void setStrategyName(String strategyName) { this.strategyName = strategyName; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public Map<String, Integer> getTargetCalories() { return targetCalories; }
    public void setTargetCalories(Map<String, Integer> targetCalories) { this.targetCalories = targetCalories; }

    public Map<String, Double> getMacroSplit() { return macroSplit; }
    public void setMacroSplit(Map<String, Double> macroSplit) { this.macroSplit = macroSplit; }

    public List<String> getConstraints() { return constraints; }
    public void setConstraints(List<String> constraints) { this.constraints = constraints; }

    public List<String> getRecommendations() { return recommendations; }
    public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
}
