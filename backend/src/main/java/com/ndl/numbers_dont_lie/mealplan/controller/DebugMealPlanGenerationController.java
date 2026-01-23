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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * STEP 6.1.5: Debug Meal Plan Generation Endpoints
 * 
 * TEMPORARY controller for triggering AI meal plan generation during development.
 * 
 * This controller is for debugging purposes only and NOT intended for production use.
 * It allows developers and testers to:
 * - Trigger single-day meal plan generation with AI
 * - Trigger weekly meal plan generation with AI
 * - Test STEP 6.1 function calling contract (calculateNutrition)
 * - Visually inspect AI-generated plans in debug frontend
 * 
 * IMPORTANT NOTES:
 * - No business logic - only delegates to existing services
 * - No authentication required - debug only
 * - Uses mock userId (hardcoded to 1L) - not production-ready
 * - Persists generated plans if persistence is available
 * - Errors are returned with debug-friendly messages
 * - POST methods (not idempotent) since they trigger AI generation
 * 
 * FUNCTION CALLING CONTRACT:
 * These endpoints will exercise the STEP 6.1 contract enforcement:
 * - AI must call calculateNutrition function for each recipe
 * - All nutrition data must come from DatabaseNutritionCalculator
 * - No nutrition hallucination allowed
 * - Logs will show [STEP 6.1] and [FC] prefixes for auditability
 * 
 * @see DayPlanAssemblerService - generates single-day meal plans
 * @see WeeklyMealPlanService - generates 7-day meal plans with persistence
 */
@RestController
@RequestMapping("/api/debug/meal-plans/generate")
public class DebugMealPlanGenerationController {
    private static final Logger logger = LoggerFactory.getLogger(DebugMealPlanGenerationController.class);
    
    /**
     * Mock userId for debug endpoints.
     * In production, this would be extracted from JWT or authentication context.
     * For debugging, we use a fixed value to avoid authentication requirements.
     */
    private static final Long DEBUG_USER_ID = 1L;
    
    private final DayPlanAssemblerService dayPlanAssemblerService;
    private final WeeklyMealPlanService weeklyMealPlanService;
    
    public DebugMealPlanGenerationController(
            DayPlanAssemblerService dayPlanAssemblerService,
            WeeklyMealPlanService weeklyMealPlanService) {
        this.dayPlanAssemblerService = dayPlanAssemblerService;
        this.weeklyMealPlanService = weeklyMealPlanService;
    }
    
    /**
     * TRIGGER single-day meal plan generation with AI.
     * 
     * This endpoint GENERATES a new DayPlan by calling the AI pipeline:
     * - STEP 4.1: Reuse cached AI strategy
     * - STEP 4.2: Reuse cached meal structure
     * - STEP 4.3.1: Retrieve similar recipes via RAG
     * - STEP 4.3.2: Generate specific recipe per meal slot (calls AI)
     * - STEP 6.1: AI calls calculateNutrition function (enforced)
     * - STEP 5.1: Assemble meals into DayPlan
     * 
     * FUNCTION CALLING CONTRACT ENFORCEMENT:
     * - Each AI-generated recipe MUST call calculateNutrition
     * - Nutrition values are calculated from database only
     * - Logs will show function calling details
     * 
     * NOTE: This creates a temporary DayPlan for inspection but does NOT
     * persist it as part of a weekly MealPlan. For full persistence,
     * use POST /week instead.
     * 
     * @param date the target date for meal plan generation (ISO 8601 format: YYYY-MM-DD)
     * @return Success message with generation details
     * @throws IllegalStateException if user not found, strategy/structure not cached, or generation fails
     */
    @PostMapping("/day")
    public ResponseEntity<?> triggerDayGeneration(
            @RequestParam(name = "date")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        try {
            logger.info("[DEBUG-GENERATE] Triggering AI meal plan generation for date={}", date);
            logger.info("[DEBUG-GENERATE] This will exercise STEP 6.1 function calling contract");
            
            // Create temporary MealPlanVersion for assembly
            // For debug day generation, we create a non-persisted temp version
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(java.time.LocalDateTime.now());
            tempVersion.setReason(com.ndl.numbers_dont_lie.mealplan.entity.VersionReason.INITIAL_GENERATION);
            
            // Call service to generate DayPlan
            // This will trigger AI generation with function calling
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    DEBUG_USER_ID,
                    date,
                    tempVersion
            );
            
            int mealCount = dayPlan.getMeals() != null ? dayPlan.getMeals().size() : 0;
            
            logger.info("[DEBUG-GENERATE] Successfully generated meal plan for date={} with {} meals", 
                date, mealCount);
            logger.info("[DEBUG-GENERATE] Check logs for [STEP 6.1] and [FC] prefixes to verify function calling");
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "AI meal plan generated successfully",
                    "date", date.toString(),
                    "mealCount", mealCount,
                    "note", "Check backend logs for function calling details ([STEP 6.1], [FC] prefixes)"
            ));
            
        } catch (IllegalStateException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            logger.error("[DEBUG-GENERATE] Failed to generate meal plan for date={}: {}", date, errorMsg);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", "Generation failed",
                    "message", errorMsg,
                    "date", date.toString()
            ));
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("[DEBUG-GENERATE] Unexpected error generating meal plan for date={}", date, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "error", "Unexpected error",
                    "message", errorMsg,
                    "date", date.toString(),
                    "exceptionType", e.getClass().getSimpleName()
            ));
        }
    }
    
    /**
     * TRIGGER weekly (7-day) meal plan generation with AI and PERSIST.
     * 
     * This endpoint GENERATES and PERSISTS a new MealPlan by:
     * - Creating MealPlan entity (root aggregate)
     * - Creating MealPlanVersion (version 1, initial)
     * - Generating 7 consecutive DayPlans (calls AI for each)
     * - Each day exercises STEP 6.1 function calling contract
     * - Persisting everything to database
     * 
     * FUNCTION CALLING CONTRACT ENFORCEMENT:
     * - Each of the 7 days generates multiple recipes
     * - Each recipe MUST call calculateNutrition function
     * - Nutrition values are calculated from database only
     * - Logs will show function calling details for each recipe
     * 
     * PERSISTENCE:
     * - Creates permanent MealPlan record
     * - Creates MealPlanVersion with all DayPlans
     * - Meals are persisted via cascade
     * - Can be retrieved via GET /api/debug/meal-plans/week endpoint
     * 
     * @param startDate the start date for week (Monday, ISO 8601 format: YYYY-MM-DD)
     * @return Success message with generation and persistence details
     * @throws IllegalStateException if user not found or generation fails
     */
    @PostMapping("/week")
    public ResponseEntity<?> triggerWeekGeneration(
            @RequestParam(name = "startDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {
        try {
            logger.info("[DEBUG-GENERATE] Triggering AI weekly meal plan generation for startDate={}", startDate);
            logger.info("[DEBUG-GENERATE] This will exercise STEP 6.1 function calling contract 7 times");
            
            // Call service to generate and persist 7-day MealPlan
            // This service handles full persistence with MealPlan entity
            MealPlan mealPlan = weeklyMealPlanService.generateWeeklyPlan(
                    DEBUG_USER_ID,
                    startDate
            );
            
            // Extract statistics
            Long mealPlanId = mealPlan.getId();
            int dayCount = mealPlan.getCurrentVersion() != null 
                && mealPlan.getCurrentVersion().getDayPlans() != null 
                ? mealPlan.getCurrentVersion().getDayPlans().size() 
                : 0;
            
            int totalMeals = 0;
            if (mealPlan.getCurrentVersion() != null 
                && mealPlan.getCurrentVersion().getDayPlans() != null) {
                totalMeals = mealPlan.getCurrentVersion().getDayPlans().stream()
                    .mapToInt(day -> day.getMeals() != null ? day.getMeals().size() : 0)
                    .sum();
            }
            
            logger.info("[DEBUG-GENERATE] Successfully generated and persisted weekly meal plan:");
            logger.info("  - MealPlan ID: {}", mealPlanId);
            logger.info("  - Days: {}", dayCount);
            logger.info("  - Total Meals: {}", totalMeals);
            logger.info("[DEBUG-GENERATE] Check logs for [STEP 6.1] and [FC] prefixes to verify function calling");
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "AI weekly meal plan generated and persisted successfully",
                    "mealPlanId", mealPlanId,
                    "startDate", startDate.toString(),
                    "dayCount", dayCount,
                    "totalMeals", totalMeals,
                    "note", "Check backend logs for function calling details ([STEP 6.1], [FC] prefixes)",
                    "retrieval", "Use GET /api/debug/meal-plans/week?startDate=" + startDate + " to view"
            ));
            
        } catch (IllegalStateException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            logger.error("[DEBUG-GENERATE] Failed to generate weekly meal plan for startDate={}: {}", 
                startDate, errorMsg);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "error", "Generation failed",
                    "message", errorMsg,
                    "startDate", startDate.toString()
            ));
        } catch (Exception e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            logger.error("[DEBUG-GENERATE] Unexpected error generating weekly meal plan for startDate={}", 
                startDate, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "error", "Unexpected error",
                    "message", errorMsg,
                    "startDate", startDate.toString(),
                    "exceptionType", e.getClass().getSimpleName()
            ));
        }
    }
}
