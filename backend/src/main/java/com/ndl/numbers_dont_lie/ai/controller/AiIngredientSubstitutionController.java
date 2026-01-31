package com.ndl.numbers_dont_lie.ai.controller;

import com.ndl.numbers_dont_lie.ai.dto.AiIngredientSubstituteRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiIngredientSubstituteResponse;
import com.ndl.numbers_dont_lie.ai.service.AiIngredientSubstitutionService;
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
@RequestMapping("/api/ai/ingredients")
public class AiIngredientSubstitutionController {
    private static final Logger logger = LoggerFactory.getLogger(AiIngredientSubstitutionController.class);
    private final AiIngredientSubstitutionService substitutionService;

    public AiIngredientSubstitutionController(AiIngredientSubstitutionService substitutionService) {
        this.substitutionService = substitutionService;
    }

    @PostMapping("/substitute")
    public ResponseEntity<?> substituteIngredient(@RequestBody AiIngredientSubstituteRequest request) {
        if (request == null || request.getRecipeId() == null || request.getIngredientName() == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "recipeId and ingredientName are required"
            ));
        }

        try {
            AiIngredientSubstituteResponse response = substitutionService.suggestSubstitutes(
                request.getRecipeId(),
                request.getIngredientName(),
                request.getAvailableIngredients()
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("[AI_SUBSTITUTE] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("[AI_SUBSTITUTE] Substitute failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to generate ingredient substitutes")
            );
        }
    }
}
