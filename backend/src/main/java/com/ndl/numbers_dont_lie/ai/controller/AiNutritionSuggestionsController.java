package com.ndl.numbers_dont_lie.ai.controller;

import com.ndl.numbers_dont_lie.ai.dto.AiNutritionSuggestionsRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiNutritionSuggestionsResponse;
import com.ndl.numbers_dont_lie.ai.service.AiNutritionSuggestionsService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/nutrition")
public class AiNutritionSuggestionsController {
    private static final Logger logger = LoggerFactory.getLogger(AiNutritionSuggestionsController.class);
    private final AiNutritionSuggestionsService suggestionsService;

    public AiNutritionSuggestionsController(AiNutritionSuggestionsService suggestionsService) {
        this.suggestionsService = suggestionsService;
    }

    @PostMapping("/suggestions")
    public ResponseEntity<?> generateSuggestions(@RequestBody AiNutritionSuggestionsRequest request) {
        if (request == null || request.getDate() == null || request.getNutritionSummary() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "date and nutritionSummary are required"
            ));
        }

        try {
            AiNutritionSuggestionsResponse response = suggestionsService.generateSuggestions(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[AI_NUTRITION] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("[AI_NUTRITION] Suggestions generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to generate nutrition suggestions")
            );
        }
    }
}
