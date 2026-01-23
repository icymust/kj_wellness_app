package com.ndl.numbers_dont_lie.mealplan.controller;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.service.DayPlanAssemblerService;
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
    
    /**
     * Mock userId for debug endpoints.
     * In production, this would be extracted from JWT or authentication context.
     * For debugging, we use a fixed value to avoid authentication requirements.
     */
    private static final Long DEBUG_USER_ID = 1L;
    
    private final DayPlanAssemblerService dayPlanAssemblerService;
    private final WeeklyMealPlanService weeklyMealPlanService;
    
    public DebugMealPlanController(
            DayPlanAssemblerService dayPlanAssemblerService,
            WeeklyMealPlanService weeklyMealPlanService) {
        this.dayPlanAssemblerService = dayPlanAssemblerService;
        this.weeklyMealPlanService = weeklyMealPlanService;
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
     * @return DayPlan containing all meals for the specified date
     * @throws IllegalStateException if user not found, strategy/structure not cached, or generation fails
     */
    @GetMapping("/day")
    public ResponseEntity<?> generateDay(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        try {
            logger.info("Debug: Generating meal plan for date={}", date);
            
            // Create temporary MealPlanVersion for assembly
            // In production, this would be part of an existing MealPlan
            // For debug, we create a temporary version
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(java.time.LocalDateTime.now());
            tempVersion.setReason(com.ndl.numbers_dont_lie.mealplan.entity.VersionReason.INITIAL_GENERATION);
            
            // Call service to generate DayPlan
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    DEBUG_USER_ID,
                    date,
                    tempVersion
            );
            
            logger.info("Debug: Successfully generated meal plan for date={}", date);
            return ResponseEntity.ok(dayPlan);
            
        } catch (IllegalStateException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            logger.error("Debug: Failed to generate meal plan for date={}: {}", date, errorMsg);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Generation failed",
                    "message", errorMsg,
                    "date", date.toString()
            ));
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("Debug: Unexpected error generating meal plan for date={}", date, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Unexpected error",
                    "message", errorMsg,
                    "date", date.toString()
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
     * @return MealPlan containing 7 DayPlans (one per day)
     * @throws IllegalStateException if user not found or generation fails
     */
    @GetMapping("/week")
    public ResponseEntity<?> generateWeek(
            @RequestParam(name = "startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {
        try {
            logger.info("Debug: Generating weekly meal plan for startDate={}", startDate);
            
            // Call service to generate 7-day MealPlan
            MealPlan mealPlan = weeklyMealPlanService.generateWeeklyPlan(
                    DEBUG_USER_ID,
                    startDate
            );
            
            logger.info("Debug: Successfully generated weekly meal plan starting from={}", startDate);
            return ResponseEntity.ok(mealPlan);
            
        } catch (IllegalStateException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            logger.error("Debug: Failed to generate weekly meal plan for startDate={}: {}", startDate, errorMsg);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Generation failed",
                    "message", errorMsg,
                    "startDate", startDate.toString()
            ));
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("Debug: Unexpected error generating weekly meal plan for startDate={}", startDate, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Unexpected error",
                    "message", errorMsg,
                    "startDate", startDate.toString()
            ));
        }
    }
}
