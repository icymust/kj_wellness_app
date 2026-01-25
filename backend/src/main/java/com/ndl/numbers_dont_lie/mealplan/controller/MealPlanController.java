package com.ndl.numbers_dont_lie.mealplan.controller;

import com.ndl.numbers_dont_lie.ai.AiStrategyService;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureResult;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyRequest;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.mealplan.dto.DailyNutritionSummary;
import com.ndl.numbers_dont_lie.mealplan.dto.WeeklyPlanResponse;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.entity.VersionReason;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanVersionRepository;
import com.ndl.numbers_dont_lie.mealplan.service.DayPlanAssemblerService;
import com.ndl.numbers_dont_lie.mealplan.service.MealReplacementService;
import com.ndl.numbers_dont_lie.mealplan.service.NutritionSummaryService;
import com.ndl.numbers_dont_lie.mealplan.service.WeeklyMealPlanService;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * STEP 6.4: Production Meal Plan API
 * 
 * User-facing API for meal plan and nutrition endpoints.
 * Delegates to existing services (DayPlanAssemblerService, NutritionSummaryService).
 * 
 * TEMPORARY:
 * - userId hardcoded to 2 (will be replaced with auth context when available)
 * - No authentication required yet
 * 
 * This controller will remain minimal until authentication is integrated.
 */
@RestController
@RequestMapping("/api/meal-plans")
public class MealPlanController {
    private static final Logger logger = LoggerFactory.getLogger(MealPlanController.class);
    
    private final DayPlanAssemblerService dayPlanAssemblerService;
    private final NutritionSummaryService nutritionSummaryService;
    private final WeeklyMealPlanService weeklyMealPlanService;
    private final AiStrategyService aiStrategyService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final MealReplacementService mealReplacementService;
    private final DayPlanRepository dayPlanRepository;
    private final MealPlanRepository mealPlanRepository;
    private final MealPlanVersionRepository mealPlanVersionRepository;
    
    public MealPlanController(
            DayPlanAssemblerService dayPlanAssemblerService,
            NutritionSummaryService nutritionSummaryService,
            WeeklyMealPlanService weeklyMealPlanService,
            AiStrategyService aiStrategyService,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            MealReplacementService mealReplacementService,
            DayPlanRepository dayPlanRepository,
            MealPlanRepository mealPlanRepository,
            MealPlanVersionRepository mealPlanVersionRepository) {
        this.dayPlanAssemblerService = dayPlanAssemblerService;
        this.nutritionSummaryService = nutritionSummaryService;
        this.weeklyMealPlanService = weeklyMealPlanService;
        this.aiStrategyService = aiStrategyService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.mealReplacementService = mealReplacementService;
        this.dayPlanRepository = dayPlanRepository;
        this.mealPlanRepository = mealPlanRepository;
        this.mealPlanVersionRepository = mealPlanVersionRepository;
    }
    
    /**
     * Get today's meal plan.
     * 
     * AUTO-BOOTSTRAPS: If no AI strategy exists, automatically generates one.
     * User will see meals on first visit without manual bootstrap.
     * 
     * @param date optional date (defaults to today if omitted)
     * @return DayPlan with meals (auto-generated if needed)
     */
    @GetMapping("/day")
    @Transactional
    public ResponseEntity<DayPlan> getDayPlan(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        // Use provided date or default to today
        if (date == null) {
            date = LocalDate.now();
        }
        
        logger.info("[MEAL_PLAN] Fetching day plan for userId={}, date={}", userId, date);
        // 1) try existing persistent day plan
        Optional<DayPlan> existingPlan = dayPlanRepository.findByUserIdAndDate(userId, date);
        if (existingPlan.isPresent()) {
            logger.info("[MEAL_PLAN] Returning persisted plan with id={} and {} meals", existingPlan.get().getId(), existingPlan.get().getMeals().size());
            return ResponseEntity.ok(existingPlan.get());
        }
        
        try {
            // Create temporary MealPlanVersion for assembly
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(LocalDateTime.now());
            tempVersion.setReason(VersionReason.INITIAL_GENERATION);
            
            // Generate DayPlan
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    userId,
                    date,
                    tempVersion
            );

            // Persist minimal plan so meals have IDs for downstream actions
            DayPlan saved = persistDayPlan(userId, date, dayPlan);
            
            logger.info("[MEAL_PLAN] Successfully loaded plan for userId={}, date={}, meals={}, persistedId={}", 
                userId, date, saved.getMeals().size(), saved.getId());
            return ResponseEntity.ok(saved);
            
        } catch (IllegalStateException e) {
            // Check if this is "AI strategy not found" error
            if (e.getMessage() != null && e.getMessage().contains("AI strategy not found")) {
                logger.info("[MEAL_PLAN] No plan found, auto-generating for userId={} date={}", userId, date);
                
                try {
                    // Auto-bootstrap: Generate AI strategy and meal structure
                    bootstrapAiForUser(userId);
                    
                    // Retry generating DayPlan
                    MealPlanVersion tempVersion = new MealPlanVersion();
                    tempVersion.setVersionNumber(0);
                    tempVersion.setCreatedAt(LocalDateTime.now());
                    tempVersion.setReason(VersionReason.INITIAL_GENERATION);
                    
                    DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                            userId,
                            date,
                            tempVersion
                    );
                    
                    logger.info("[MEAL_PLAN] Auto-bootstrap SUCCESS! Generated plan with {} meals", 
                        dayPlan.getMeals().size());
                    return ResponseEntity.ok(dayPlan);
                    
                } catch (Exception bootstrapError) {
                    logger.error("[MEAL_PLAN] Auto-bootstrap FAILED for userId={}: {}", 
                        userId, bootstrapError.getMessage(), bootstrapError);
                    
                    // Return empty fallback if bootstrap fails
                    DayPlan fallbackPlan = new DayPlan();
                    fallbackPlan.setDate(date);
                    fallbackPlan.setMeals(java.util.Collections.emptyList());
                    return ResponseEntity.ok(fallbackPlan);
                }
            }
            
            // Other IllegalStateException - return empty fallback
            logger.warn("[MEAL_PLAN] No plan found for date {}, returning empty fallback: {}", date, e.getMessage());
            DayPlan fallbackPlan = new DayPlan();
            fallbackPlan.setDate(date);
            fallbackPlan.setMeals(java.util.Collections.emptyList());
            return ResponseEntity.ok(fallbackPlan);
            
        } catch (Exception e) {
            logger.warn("[MEAL_PLAN] Unexpected error generating meal plan for date {}, returning empty fallback", date, e);
            
            // Return empty fallback DayPlan instead of error
            DayPlan fallbackPlan = new DayPlan();
            fallbackPlan.setDate(date);
            fallbackPlan.setMeals(java.util.Collections.emptyList());
            return ResponseEntity.ok(fallbackPlan);
        }
    }

    /**
     * Persist assembled DayPlan to the database to ensure meals have IDs.
     * Creates minimal MealPlan + MealPlanVersion if they do not exist.
     */
    private DayPlan persistDayPlan(Long userId, LocalDate date, DayPlan assembled) {
        // If any meal already has ID, assume persisted elsewhere
        boolean hasIds = assembled.getMeals().stream().anyMatch(m -> m.getId() != null);
        if (hasIds && assembled.getId() != null) {
            return assembled;
        }

        // Create or reuse MealPlan (duration DAILY, timezone default UTC)
        MealPlan mealPlan = mealPlanRepository.findFirstByUserId(userId)
                .orElseGet(() -> mealPlanRepository.save(new MealPlan(userId, PlanDuration.DAILY, "UTC")));

        // Create new version
        MealPlanVersion version = new MealPlanVersion(mealPlan, 1, VersionReason.INITIAL_GENERATION);
        version = mealPlanVersionRepository.save(version);

        // Attach assembled day plan to version and user
        assembled.setMealPlanVersion(version);
        assembled.setUserId(userId);

        DayPlan saved = dayPlanRepository.save(assembled);
        logger.info("[MEAL_PLAN] Persisted day plan id={}, meals={} for userId={}", saved.getId(), saved.getMeals().size(), userId);
        return saved;
    }
    
    /**
     * Get nutrition summary for a specific day.
     * 
     * RESILIENT: Always returns 200 with valid summary structure.
     * If no plan exists for date, returns empty summary with nutrition_estimated=true.
     * 
     * @param date optional date (defaults to today if omitted)
     * @return DailyNutritionSummary with nutrition totals and progress
     */
    @GetMapping("/day/nutrition")
    public ResponseEntity<DailyNutritionSummary> getDayNutrition(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        // Use provided date or default to today
        if (date == null) {
            date = LocalDate.now();
        }
        
        logger.info("[MEAL_PLAN] Fetching nutrition summary for userId={}, date={}", userId, date);
        
        try {
            // Create temporary MealPlanVersion for assembly
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(LocalDateTime.now());
            tempVersion.setReason(VersionReason.INITIAL_GENERATION);
            
            // Generate DayPlan with meals
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    userId,
                    date,
                    tempVersion
            );
            
            // Generate nutrition summary
            DailyNutritionSummary summary = nutritionSummaryService.generateSummary(dayPlan);
            
            logger.info("[MEAL_PLAN] Nutrition summary generated for userId={}, date={}", userId, date);
            return ResponseEntity.ok(summary);
            
        } catch (IllegalStateException | IllegalArgumentException e) {
            logger.warn("[MEAL_PLAN] No plan found for date {}, returning empty summary: {}", date, e.getMessage());
            
            // Return empty summary marked as estimated (no data available)
            DailyNutritionSummary fallbackSummary = new DailyNutritionSummary();
            fallbackSummary.setDate(date);
            fallbackSummary.setNutritionEstimated(true);
            return ResponseEntity.ok(fallbackSummary);
            
        } catch (Exception e) {
            logger.warn("[MEAL_PLAN] Unexpected error generating nutrition summary for date {}, returning empty", date, e);
            
            // Return empty summary marked as estimated instead of error
            DailyNutritionSummary fallbackSummary = new DailyNutritionSummary();
            fallbackSummary.setDate(date);
            fallbackSummary.setNutritionEstimated(true);
            return ResponseEntity.ok(fallbackSummary);
        }
    }

    /**
     * Generate a weekly meal plan (7 days) using existing daily logic.
     * Returns day plans plus aggregated weekly nutrition summary.
     */
    @GetMapping("/week")
    public ResponseEntity<WeeklyPlanResponse> getWeeklyPlan(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }

        logger.info("[WEEK_PLAN] Fetching week plan for userId={} startDate={}", userId, startDate);
        try {
            WeeklyPlanResponse response = weeklyMealPlanService.generateWeeklyPlanPreview(userId, startDate);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[WEEK_PLAN] Failed to generate week plan: {}", e.getMessage(), e);
            WeeklyPlanResponse fallback = new WeeklyPlanResponse(java.util.Collections.emptyList(), new com.ndl.numbers_dont_lie.mealplan.dto.WeeklyNutritionSummary());
            return ResponseEntity.ok(fallback);
        }
    }
    
    /**
     * Helper method to bootstrap AI strategy and meal structure for a user.
     * Called automatically on first meal plan request if cache is empty.
     * 
     * Executes STEP 4.1 (AI Strategy) and STEP 4.2 (Meal Structure) sequentially.
     * 
     * @param userId User ID to bootstrap
     * @throws IllegalStateException if user/profile not found or AI generation fails
     */
    private void bootstrapAiForUser(Long userId) {
        logger.info("[AI BOOTSTRAP] Starting auto-bootstrap for userId={}", userId);
        
        // Fetch user and profile
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        ProfileEntity profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Profile not found for user: " + userId));
        
        logger.info("[AI BOOTSTRAP] Profile found: age={}, goal={}, mealFreq={}", 
            profile.getAge(), profile.getGoal(), profile.getMealFrequency());
        
        // STEP 4.1: Build and execute AI Strategy Analysis
        AiStrategyRequest strategyRequest = new AiStrategyRequest();
        strategyRequest.setUserId(String.valueOf(userId));
        strategyRequest.setTimezone(profile.getTimezone() != null 
            ? ZoneId.of(profile.getTimezone()) 
            : ZoneId.systemDefault());
        strategyRequest.setAge(profile.getAge() != null ? profile.getAge() : 30);
        strategyRequest.setSex(profile.getGender() != null ? profile.getGender() : "other");
        strategyRequest.setHeightCm(profile.getHeightCm() != null ? profile.getHeightCm() : 170);
        strategyRequest.setWeightKg(profile.getWeightKg() != null ? profile.getWeightKg() : 70);
        strategyRequest.setGoal(profile.getGoal() != null ? profile.getGoal() : "general_fitness");
        strategyRequest.setDietaryPreferences(Collections.emptyMap());
        strategyRequest.setAllergies(Collections.emptyList());
        
        // Parse meal frequency
        int mealCount = parseMealFrequency(profile.getMealFrequency());
        Map<String, Integer> mealFreq = new HashMap<>();
        mealFreq.put("breakfast", 1);
        mealFreq.put("lunch", 1);
        mealFreq.put("dinner", 1);
        mealFreq.put("snacks", Math.max(0, mealCount - 3));
        strategyRequest.setMealFrequency(mealFreq);
        
        logger.info("[AI BOOTSTRAP] Executing STEP 4.1 - AI Strategy Analysis...");
        AiStrategyResult strategyResult = aiStrategyService.analyzeStrategy(strategyRequest);
        
        if (strategyResult == null) {
            throw new IllegalStateException("AI Strategy returned null");
        }
        
        logger.info("[AI BOOTSTRAP] STEP 4.1 SUCCESS: strategy={}, dailyCalories={}", 
            strategyResult.getStrategyName(), 
            strategyResult.getTargetCalories() != null ? strategyResult.getTargetCalories().get("daily") : null);
        
        // STEP 4.2: Build and execute Meal Structure Distribution
        AiMealStructureRequest structureRequest = new AiMealStructureRequest();
        structureRequest.setUserId(String.valueOf(userId));
        structureRequest.setStrategyResult(strategyResult);
        if (strategyResult.getTargetCalories() != null && strategyResult.getTargetCalories().get("daily") != null) {
            structureRequest.setDailyCalorieTarget(strategyResult.getTargetCalories().get("daily"));
        }
        
        logger.info("[AI BOOTSTRAP] Executing STEP 4.2 - Meal Structure Distribution...");
        AiMealStructureResult structureResult = aiStrategyService.analyzeMealStructure(structureRequest);
        
        if (structureResult == null) {
            throw new IllegalStateException("Meal Structure returned null");
        }
        
        logger.info("[AI BOOTSTRAP] STEP 4.2 SUCCESS: mealSlots={}, totalCalories={}", 
            structureResult.getMeals() != null ? structureResult.getMeals().size() : 0,
            structureResult.getTotalCaloriesDistributed());
        
        logger.info("[AI BOOTSTRAP] Auto-bootstrap completed successfully for userId={}", userId);
    }
    
    /**
     * Replace a single meal in a day plan with an alternative recipe.
     * 
     * Respects user dietary preferences and ensures variety within the day.
     * The meal type (breakfast/lunch/dinner/snack) remains unchanged.
     * Nutrition summary will recalculate automatically on next fetch.
     * 
     * @param mealId ID of meal to replace
     * @return Updated Meal object
     */
    @PostMapping("/meals/{mealId}/replace")
    public ResponseEntity<Meal> replaceMeal(@PathVariable Long mealId) {
        logger.info("[MEAL_API] Replace meal request: mealId={}", mealId);
        
        try {
            Meal replacedMeal = mealReplacementService.replaceMeal(mealId);
            logger.info("[MEAL_API] Meal replaced successfully: {}", replacedMeal.getCustomMealName());
            return ResponseEntity.ok(replacedMeal);
            
        } catch (IllegalArgumentException e) {
            logger.error("[MEAL_API] Meal not found: mealId={}", mealId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            
        } catch (IllegalStateException e) {
            logger.error("[MEAL_API] No alternative recipe found for mealId={}: {}", mealId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(null); // 409 Conflict - cannot find suitable replacement
        }
    }
    
    /**
     * Parse meal frequency string (e.g., "THREE_MEALS") to integer count.
     */
    private int parseMealFrequency(String mealFrequency) {
        if (mealFrequency == null) {
            return 3; // Default to 3 meals
        }
        
        switch (mealFrequency.toUpperCase()) {
            case "THREE_MEALS":
            case "3":
                return 3;
            case "FOUR_MEALS":
            case "4":
                return 4;
            case "FIVE_MEALS":
            case "5":
                return 5;
            case "SIX_MEALS":
            case "6":
                return 6;
            default:
                return 3;
        }
    }
}
