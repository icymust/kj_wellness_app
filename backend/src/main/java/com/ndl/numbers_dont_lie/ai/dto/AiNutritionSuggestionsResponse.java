package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;

public class AiNutritionSuggestionsResponse {
    private List<String> suggestions;

    public AiNutritionSuggestionsResponse() {}

    public AiNutritionSuggestionsResponse(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}
