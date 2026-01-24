package com.ndl.numbers_dont_lie.nutrition.controller;

import com.ndl.numbers_dont_lie.dto.nutrition.NutritionalPreferencesDto;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.service.nutrition.NutritionalPreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * DEBUG-ONLY: User Food Preferences API
 * 
 * Exposes backend source-of-truth for nutritional/food preferences without
 * touching AI pipeline or frontend. Permitted via SecurityConfig for debug.
 * 
 * Endpoints:
 * - GET  /api/debug/user/{userId}/food-preferences
 * - POST /api/debug/user/{userId}/food-preferences
 */
@RestController
@RequestMapping("/api/debug/user/{userId}/food-preferences")
public class DebugFoodPreferencesController {
    private static final Logger logger = LoggerFactory.getLogger(DebugFoodPreferencesController.class);

    private final NutritionalPreferencesService preferencesService;
    private final UserRepository userRepository;

    public DebugFoodPreferencesController(NutritionalPreferencesService preferencesService,
                                          UserRepository userRepository) {
        this.preferencesService = preferencesService;
        this.userRepository = userRepository;
    }

    /**
     * Returns the user's nutritional preferences. If none exist, returns safe defaults
     * backed by an in-memory DTO (no persistence side effects).
     */
    @GetMapping
    public ResponseEntity<?> get(@PathVariable("userId") Long userId) {
        logger.info("[DEBUG] GET food-preferences for userId={}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        NutritionalPreferencesDto dto = preferencesService.get(user.getEmail());
        return ResponseEntity.ok(dto);
    }

    /**
     * Upserts the user's nutritional preferences with validation handled by service.
     */
    @PostMapping
    public ResponseEntity<?> upsert(@PathVariable("userId") Long userId,
                                    @RequestBody NutritionalPreferencesDto request) {
        logger.info("[DEBUG] POST food-preferences for userId={}", userId);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        NutritionalPreferencesDto updated = preferencesService.upsert(user.getEmail(), request);
        return ResponseEntity.ok(updated);
    }
}
