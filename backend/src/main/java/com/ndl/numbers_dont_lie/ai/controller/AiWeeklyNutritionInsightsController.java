package com.ndl.numbers_dont_lie.ai.controller;

import com.ndl.numbers_dont_lie.ai.dto.AiWeeklyNutritionInsightsRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiWeeklyNutritionInsightsResponse;
import com.ndl.numbers_dont_lie.ai.service.AiWeeklyNutritionInsightsService;
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
public class AiWeeklyNutritionInsightsController {
    private static final Logger logger = LoggerFactory.getLogger(AiWeeklyNutritionInsightsController.class);
    private final AiWeeklyNutritionInsightsService insightsService;

    public AiWeeklyNutritionInsightsController(AiWeeklyNutritionInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @PostMapping("/weekly-insights")
    public ResponseEntity<?> generateInsights(@RequestBody AiWeeklyNutritionInsightsRequest request) {
        if (request == null || request.getStartDate() == null || request.getWeeklyNutrition() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "startDate and weeklyNutrition are required"
            ));
        }

        try {
            AiWeeklyNutritionInsightsResponse response = insightsService.generateInsights(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[AI_NUTRITION] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("[AI_NUTRITION] Weekly insights generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to generate weekly nutrition insights")
            );
        }
    }
}
