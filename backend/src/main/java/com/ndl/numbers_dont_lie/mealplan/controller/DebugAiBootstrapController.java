package com.ndl.numbers_dont_lie.mealplan.controller;

import com.ndl.numbers_dont_lie.ai.AiStrategyService;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureResult;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * DEBUG-ONLY Controller: Bootstrap AI Pipeline
 *
 * PURPOSE:
 * Manually execute STEP 4.1 (AI Strategy) and STEP 4.2 (Meal Structure) for a given user.
 * This populates the AI cache so that existing debug meal plan endpoints return real data
 * instead of fallback responses.
 *
 * USAGE:
 * POST /api/debug/ai/bootstrap?userId=1
 *
 * WORKFLOW:
 * 1. Fetch user profile from database
 * 2. Build AiStrategyRequest from profile
 * 3. Call AiStrategyService.analyzeStrategy() → caches STEP 4.1 result
 * 4. Build AiMealStructureRequest using strategy result
 * 5. Call AiStrategyService.analyzeMealStructure() → caches STEP 4.2 result
 * 6. Return success JSON with cached data summary
 *
 * CONSTRAINTS:
 * - Uses existing services (no new abstractions)
 * - DEBUG MODE ONLY (not for production use)
 * - Public endpoint (bypasses authentication via SecurityConfig)
 * - Comprehensive logging for debugging
 *
 * @author GitHub Copilot Chat
 */
@RestController
@RequestMapping("/api/debug/meal-plans")
public class DebugAiBootstrapController {

    private static final Logger logger = LoggerFactory.getLogger(DebugAiBootstrapController.class);

    private final AiStrategyService aiStrategyService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public DebugAiBootstrapController(
            AiStrategyService aiStrategyService,
            UserRepository userRepository,
            ProfileRepository profileRepository
    ) {
        this.aiStrategyService = aiStrategyService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    /**
     * POST /api/debug/ai/bootstrap?userId=1
     *
     * Executes the full AI pipeline for a user:
     * - STEP 4.1: AI Strategy Analysis
     * - STEP 4.2: Meal Structure Distribution
     *
     * Caches results via AiSessionCache.
     *
     * @param userId User ID to bootstrap AI for (defaults to 1)
     * @return JSON response with execution status and cached data summary
     */
    @PostMapping("/ai-bootstrap")
    public ResponseEntity<Map<String, Object>> bootstrap(
            @RequestParam(defaultValue = "1") Long userId
    ) {
        System.out.println("========================================");
        System.out.println("AI BOOTSTRAP - START");
        System.out.println("========================================");
        System.out.println("User ID: " + userId);
        System.out.println();

        logger.info("[AI BOOTSTRAP] Starting bootstrap for userId={}", userId);

        try {
            // ========== STEP 1: Fetch User & Profile ==========
            logger.info("[AI BOOTSTRAP] STEP 1: Fetching user profile from database...");
            System.out.println("STEP 1: Fetching user profile from database...");

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            System.out.println("✓ User found: " + user.getEmail());

            ProfileEntity profile = profileRepository.findByUser(user)
                    .orElseThrow(() -> new IllegalArgumentException("Profile not found for user: " + userId));
            System.out.println("✓ Profile found");
            System.out.println("  - Age: " + profile.getAge());
            System.out.println("  - Gender: " + profile.getGender());
            System.out.println("  - Height: " + profile.getHeightCm() + " cm");
            System.out.println("  - Weight: " + profile.getWeightKg() + " kg");
            System.out.println("  - Goal: " + profile.getGoal());
            System.out.println("  - Meal Frequency: " + profile.getMealFrequency());
            System.out.println();

            // ========== STEP 2: Build AiStrategyRequest ==========
            logger.info("[AI BOOTSTRAP] STEP 2: Building AiStrategyRequest...");
            System.out.println("STEP 2: Building AiStrategyRequest...");

            AiStrategyRequest strategyRequest = new AiStrategyRequest();
            strategyRequest.setUserId(String.valueOf(userId));
            strategyRequest.setTimezone(profile.getTimezone() != null ? ZoneId.of(profile.getTimezone()) : ZoneId.systemDefault());
            strategyRequest.setAge(profile.getAge() != null ? profile.getAge() : 30);
            strategyRequest.setSex(profile.getGender() != null ? profile.getGender() : "other");
            strategyRequest.setHeightCm(profile.getHeightCm() != null ? profile.getHeightCm() : 170);
            strategyRequest.setWeightKg(profile.getWeightKg() != null ? profile.getWeightKg() : 70);
            strategyRequest.setGoal(profile.getGoal() != null ? profile.getGoal() : "general_fitness");
            
            // Dietary preferences and allergies not in ProfileEntity - use empty defaults
            strategyRequest.setDietaryPreferences(Collections.emptyMap());
            strategyRequest.setAllergies(Collections.emptyList());
            
            // Parse meal frequency from profile (e.g., "THREE_MEALS" → 3)
            int mealCount = parseMealFrequency(profile.getMealFrequency());
            Map<String, Integer> mealFreq = new HashMap<>();
            mealFreq.put("breakfast", 1);
            mealFreq.put("lunch", 1);
            mealFreq.put("dinner", 1);
            mealFreq.put("snacks", Math.max(0, mealCount - 3));
            strategyRequest.setMealFrequency(mealFreq);

            System.out.println("✓ AiStrategyRequest built");
            System.out.println("  - Timezone: " + strategyRequest.getTimezone());
            System.out.println("  - Meal count: " + mealCount + " meals/day");
            System.out.println();

            // ========== STEP 3: Execute STEP 4.1 - AI Strategy Analysis ==========
            logger.info("[AI BOOTSTRAP] STEP 3: Executing STEP 4.1 - AI Strategy Analysis...");
            System.out.println("STEP 3: Executing STEP 4.1 - AI Strategy Analysis...");
            System.out.println("Calling AiStrategyService.analyzeStrategy()...");
            System.out.println();

            AiStrategyResult strategyResult = aiStrategyService.analyzeStrategy(strategyRequest);

            if (strategyResult == null) {
                System.out.println("✗ FAILED: AI Strategy returned null");
                logger.error("[AI BOOTSTRAP] AI Strategy returned null for userId={}", userId);
                throw new IllegalStateException("AI Strategy returned null - check API key and service availability");
            }

            Integer dailyCalories = strategyResult.getTargetCalories() != null 
                    ? strategyResult.getTargetCalories().get("daily") 
                    : null;

            System.out.println("✓ AI Strategy generated successfully");
            System.out.println("  - Strategy Name: " + strategyResult.getStrategyName());
            System.out.println("  - Daily Calories: " + dailyCalories);
            System.out.println("  - Cached in: AiSessionCache.userStrategies");
            System.out.println();

            logger.info("[AI BOOTSTRAP] STEP 4.1 completed: strategy={}, dailyCalories={}",
                    strategyResult.getStrategyName(), dailyCalories);

            // ========== STEP 4: Execute STEP 4.2 - Meal Structure Distribution ==========
            logger.info("[AI BOOTSTRAP] STEP 4: Executing STEP 4.2 - Meal Structure Distribution...");
            System.out.println("STEP 4: Executing STEP 4.2 - Meal Structure Distribution...");
            System.out.println("Calling AiStrategyService.analyzeMealStructure()...");
            System.out.println();

            AiMealStructureRequest structureRequest = new AiMealStructureRequest();
            structureRequest.setUserId(String.valueOf(userId));
            structureRequest.setStrategyResult(strategyResult);
            if (dailyCalories != null) {
                structureRequest.setDailyCalorieTarget(dailyCalories);
            }

            AiMealStructureResult structureResult = aiStrategyService.analyzeMealStructure(structureRequest);

            if (structureResult == null) {
                System.out.println("✗ FAILED: Meal Structure returned null");
                logger.error("[AI BOOTSTRAP] Meal Structure returned null for userId={}", userId);
                throw new IllegalStateException("Meal Structure returned null - check AI service");
            }

            int mealSlots = structureResult.getMeals() != null ? structureResult.getMeals().size() : 0;

            System.out.println("✓ Meal Structure generated successfully");
            System.out.println("  - Meal Slots: " + mealSlots);
            System.out.println("  - Total Calories Distributed: " + structureResult.getTotalCaloriesDistributed());
            System.out.println("  - Cached in: AiSessionCache.userMealStructures");
            System.out.println();

            logger.info("[AI BOOTSTRAP] STEP 4.2 completed: mealSlots={}, totalCalories={}",
                    mealSlots, structureResult.getTotalCaloriesDistributed());

            // ========== STEP 5: Build Success Response ==========
            System.out.println("========================================");
            System.out.println("AI BOOTSTRAP - SUCCESS");
            System.out.println("========================================");
            System.out.println();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "OK");
            response.put("userId", userId);
            response.put("aiStrategyGenerated", true);
            response.put("mealStructureGenerated", true);
            response.put("dailyCalories", dailyCalories);
            response.put("mealSlots", mealSlots);
            response.put("message", "AI pipeline executed successfully. Cache populated for userId=" + userId);

            logger.info("[AI BOOTSTRAP] Bootstrap completed successfully for userId={}", userId);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // User or profile not found
            System.out.println("✗ ERROR: " + e.getMessage());
            System.out.println();
            logger.error("[AI BOOTSTRAP] Bad request for userId={}: {}", userId, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("userId", userId);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            // Unexpected error
            System.out.println("✗ UNEXPECTED ERROR: " + e.getMessage());
            e.printStackTrace();
            System.out.println();
            logger.error("[AI BOOTSTRAP] Unexpected error for userId=" + userId, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("userId", userId);
            errorResponse.put("error", "Unexpected error: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Parse meal frequency string to integer count.
     * Examples: "THREE_MEALS" → 3, "FOUR_MEALS" → 4, "FIVE_MEALS" → 5
     * Defaults to 3 if parsing fails.
     */
    private int parseMealFrequency(String mealFrequency) {
        if (mealFrequency == null || mealFrequency.isBlank()) {
            return 3; // default
        }
        String upper = mealFrequency.toUpperCase();
        if (upper.contains("THREE")) return 3;
        if (upper.contains("FOUR")) return 4;
        if (upper.contains("FIVE")) return 5;
        // Try to extract number directly
        try {
            return Integer.parseInt(mealFrequency.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 3; // fallback
        }
    }
}
