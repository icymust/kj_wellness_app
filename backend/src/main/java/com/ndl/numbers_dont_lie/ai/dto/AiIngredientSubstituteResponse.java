package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;

public class AiIngredientSubstituteResponse {
    private List<Alternative> alternatives;

    public AiIngredientSubstituteResponse() {}

    public AiIngredientSubstituteResponse(List<Alternative> alternatives) {
        this.alternatives = alternatives;
    }

    public List<Alternative> getAlternatives() {
        return alternatives;
    }

    public void setAlternatives(List<Alternative> alternatives) {
        this.alternatives = alternatives;
    }

    public static class Alternative {
        private String name;
        private String reason;

        public Alternative() {}

        public Alternative(String name, String reason) {
            this.name = name;
            this.reason = reason;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
