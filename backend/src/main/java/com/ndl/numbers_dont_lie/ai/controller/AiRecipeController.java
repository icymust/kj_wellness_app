package com.ndl.numbers_dont_lie.ai.controller;

import com.ndl.numbers_dont_lie.ai.dto.AiRecipeGenerateRequest;
import com.ndl.numbers_dont_lie.ai.service.AiRecipeMvpService;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
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
@RequestMapping("/api/ai/recipes")
public class AiRecipeController {
    private static final Logger logger = LoggerFactory.getLogger(AiRecipeController.class);
    private final AiRecipeMvpService aiRecipeMvpService;

    public AiRecipeController(AiRecipeMvpService aiRecipeMvpService) {
        this.aiRecipeMvpService = aiRecipeMvpService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateRecipe(@RequestBody AiRecipeGenerateRequest request) {
        if (request == null || request.getUserId() == null || request.getMealType() == null) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "userId and mealType are required")
            );
        }

        try {
            Recipe recipe = aiRecipeMvpService.generateAiRecipeAndAttach(
                request.getUserId(),
                request.getMealType(),
                request.getMealId()
            );
            return ResponseEntity.ok(recipe);
        } catch (IllegalArgumentException e) {
            logger.warn("[AI_RECIPE] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("[AI_RECIPE] Generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to generate AI recipe")
            );
        }
    }
}
