package com.ndl.numbers_dont_lie.mealplan.controller;

import com.ndl.numbers_dont_lie.mealplan.dto.DailyNutritionSummary;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.service.DayPlanAssemblerService;
import com.ndl.numbers_dont_lie.mealplan.service.NutritionSummaryService;
import com.ndl.numbers_dont_lie.mealplan.service.WeeklyMealPlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * STEP 5.5.1: Debug Viewer API
 * 
 * TEMPORARY AND READ-ONLY controller for visually inspecting AI-generated meal plans.
 * 
 * This controller is for debugging purposes only and NOT intended for production use.
 * It allows developers and testers to:
 * - Generate single-day meal plans and inspect the output
 * - Generate weekly meal plans and review the structure
 * - Verify AI recipe generation and assembly
 * - Test nutritional preference handling
 * 
 * IMPORTANT NOTES:
 * - No business logic - only delegates to existing services
 * - No authentication required - debug only
 * - Uses mock userId (hardcoded to 1L) - not production-ready
 * - No new data persisted by these endpoints
 * - Errors are returned as-is for debugging visibility
 * 
 * @see DayPlanAssemblerService - generates single-day meal plans
 * @see WeeklyMealPlanService - generates 7-day meal plans
 */
@RestController
@RequestMapping("/api/debug/meal-plans")
public class DebugMealPlanController {
    private static final Logger logger = LoggerFactory.getLogger(DebugMealPlanController.class);
    
    private final DayPlanAssemblerService dayPlanAssemblerService;
    private final WeeklyMealPlanService weeklyMealPlanService;
    private final NutritionSummaryService nutritionSummaryService;
    
    public DebugMealPlanController(
            DayPlanAssemblerService dayPlanAssemblerService,
            WeeklyMealPlanService weeklyMealPlanService,
            NutritionSummaryService nutritionSummaryService) {
        this.dayPlanAssemblerService = dayPlanAssemblerService;
        this.weeklyMealPlanService = weeklyMealPlanService;
        this.nutritionSummaryService = nutritionSummaryService;
    }
    
    /**
     * Generate and return a single-day meal plan.
     * 
     * Calls DayPlanAssemblerService which orchestrates the AI pipeline:
     * - STEP 4.1: Reuse cached AI strategy
     * - STEP 4.2: Reuse cached meal structure
     * - STEP 4.3.1: Retrieve similar recipes via RAG
     * - STEP 4.3.2: Generate specific recipe per meal slot
     * - STEP 5.1: Assemble meals into DayPlan
     * 
     * @param date the target date for meal plan generation (ISO 8601 format: YYYY-MM-DD)
     * @param userId required user ID for meal plan generation
     * @return DayPlan containing all meals for the specified date
     * @throws IllegalStateException if user not found, strategy/structure not cached, or generation fails
     */
    @GetMapping("/day")
    public ResponseEntity<?> generateDay(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(name = "userId")
            Long userId) {
        // Guard: userId is required
        if (userId == null || userId <= 0) {
            logger.warn("[USER_CONTEXT] MISSING: userId is required for /day endpoint");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing required parameter: userId",
                "message", "userId must be a positive integer",
                "example", "/api/debug/meal-plans/day?date=2026-01-25&userId=2"
            ));
        }
        
        logger.info("[USER_CONTEXT] Using userId = {}", userId);
        logger.info("DEBUG endpoint HIT: /day for userId={}, date={}", userId, date);
        try {
            logger.info("Debug: Generating meal plan for userId={}, date={}", userId, date);
            
            // Create temporary MealPlanVersion for assembly
            // In production, this would be part of an existing MealPlan
            // For debug, we create a temporary version
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(java.time.LocalDateTime.now());
            tempVersion.setReason(com.ndl.numbers_dont_lie.mealplan.entity.VersionReason.INITIAL_GENERATION);
            
            // Call service to generate DayPlan
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    userId,
                    date,
                    tempVersion
            );
            
            logger.info("Debug: Successfully generated meal plan for userId={}, date={}", userId, date);
                return ResponseEntity.ok(dayPlan);
            
        } catch (IllegalStateException e) {
                logger.error("Debug: Failed to generate meal plan for userId={}, date={} (IllegalStateException)", userId, date, e);
                return ResponseEntity.ok(Map.of(
                    "debug", true,
                    "fallback", true,
                    "error", e.getMessage() != null ? e.getMessage() : "Generation failed",
                    "date", date.toString(),
                    "meals", java.util.List.of(Map.of("name", "Fallback Meal", "calories", 500))
                ));
        } catch (Exception e) {
                logger.error("Debug: Unexpected error generating meal plan for userId={}, date={}", userId, date, e);
                return ResponseEntity.ok(Map.of(
                    "debug", true,
                    "fallback", true,
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    "date", date.toString(),
                    "meals", java.util.List.of(Map.of("name", "Fallback Meal", "calories", 500))
                ));
        }
    }
    
    /**
     * Generate and return a weekly (7-day) meal plan.
     * 
     * Calls WeeklyMealPlanService which:
     * - Generates 7 consecutive days of meal plans
     * - Calls DayPlanAssemblerService for each day
     * - Handles partial failures gracefully (failed days get placeholder meals)
     * - Returns complete MealPlan with all DayPlans
     * 
     * @param startDate the start date for week (Monday, ISO 8601 format: YYYY-MM-DD)
     * @param userId required user ID for meal plan generation
     * @return MealPlan containing 7 DayPlans (one per day)
     * @throws IllegalStateException if user not found or generation fails
     */
    @GetMapping("/week")
    public ResponseEntity<?> generateWeek(
            @RequestParam(name = "startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam(name = "userId")
            Long userId) {
        // Guard: userId is required
        if (userId == null || userId <= 0) {
            logger.warn("[USER_CONTEXT] MISSING: userId is required for /week endpoint");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing required parameter: userId",
                "message", "userId must be a positive integer",
                "example", "/api/debug/meal-plans/week?startDate=2026-01-20&userId=2"
            ));
        }
        
        logger.info("[USER_CONTEXT] Using userId = {}", userId);
        logger.info("DEBUG endpoint HIT: /week for userId={}, startDate={}", userId, startDate);
        try {
            logger.info("Debug: Generating weekly meal plan for userId={}, startDate={}", userId, startDate);
            
            // Call service to generate 7-day MealPlan
            MealPlan mealPlan = weeklyMealPlanService.generateWeeklyPlan(
                    userId,
                    startDate
            );
            
            logger.info("Debug: Successfully generated weekly meal plan starting from userId={}, startDate={}", userId, startDate);
                return ResponseEntity.ok(mealPlan);
            
        } catch (IllegalStateException e) {
                logger.error("Debug: Failed to generate weekly meal plan for userId={}, startDate={} (IllegalStateException)", userId, startDate, e);
                return ResponseEntity.ok(Map.of(
                    "debug", true,
                    "fallback", true,
                    "error", e.getMessage() != null ? e.getMessage() : "Generation failed",
                    "startDate", startDate.toString(),
                    "meals", java.util.List.of(Map.of("name", "Fallback Meal", "calories", 500))
                ));
        } catch (Exception e) {
                logger.error("Debug: Unexpected error generating weekly meal plan for userId={}, startDate={}", userId, startDate, e);
                return ResponseEntity.ok(Map.of(
                    "debug", true,
                    "fallback", true,
                    "error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                    "startDate", startDate.toString(),
                    "meals", java.util.List.of(Map.of("name", "Fallback Meal", "calories", 500))
                ));
        }
    }
    
    /**
     * Get nutrition summary for a specific day (STEP 6.3 - Nutrition Visualization).
     * 
     * Aggregates nutrition from all meals in the day and compares to user targets.
     * Returns daily totals, percentages of targets, and progress visualization data.
     * 
     * CURRENT MVP LIMITATION:
     * - Meal entity doesn't store GeneratedRecipe nutrition yet
     * - Returns placeholder data for UI development and testing
     * - See NutritionSummaryService for future enhancement plan
     * 
     * @param date the target date for nutrition summary (ISO 8601 format: YYYY-MM-DD)
     * @param userId required user ID for meal plan generation
     * @return DailyNutritionSummary with aggregated nutrition and progress percentages
     * @throws IllegalStateException if day plan not found or user has no nutritional targets
     */
    @GetMapping("/day/nutrition")
    public ResponseEntity<?> getDayNutrition(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam(name = "userId")
            Long userId) {
        // Guard: userId is required
        if (userId == null || userId <= 0) {
            logger.warn("[USER_CONTEXT] MISSING: userId is required for /day/nutrition endpoint");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing required parameter: userId",
                "message", "userId must be a positive integer",
                "example", "/api/debug/meal-plans/day/nutrition?date=2026-01-25&userId=2"
            ));
        }
        
        logger.info("[USER_CONTEXT] Using userId = {}", userId);
        try {
            logger.info("Debug: Fetching nutrition summary for userId={}, date={}", userId, date);
            
            // For debug purposes, we need to generate a day first or fetch existing
            // Since this is MVP, we'll generate day on-demand
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(java.time.LocalDateTime.now());
            tempVersion.setReason(com.ndl.numbers_dont_lie.mealplan.entity.VersionReason.INITIAL_GENERATION);
            
            // Generate DayPlan with meals
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    userId,
                    date,
                    tempVersion
            );
            
            // Generate nutrition summary
            DailyNutritionSummary summary = nutritionSummaryService.generateSummary(dayPlan, userId);
            
            logger.info("Debug: Nutrition summary generated for userId={}, date={}, meals={}", 
                userId, date, dayPlan.getMeals().size());
            return ResponseEntity.ok(summary);
            
        } catch (IllegalStateException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            logger.error("Debug: Failed to fetch nutrition summary for userId={}, date={}: {}", userId, date, errorMsg);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Summary generation failed",
                    "message", errorMsg,
                    "date", date.toString()
            ));
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("Debug: Unexpected error fetching nutrition summary for userId={}, date={}", userId, date, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Unexpected error",
                    "message", errorMsg,
                    "date", date.toString()
            ));
        }
    }
}
