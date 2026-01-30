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
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.entity.VersionReason;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanVersionRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.mealplan.service.DayPlanAssemblerService;
import com.ndl.numbers_dont_lie.mealplan.service.MealReplacementService;
import com.ndl.numbers_dont_lie.mealplan.service.MealMoveService;
import com.ndl.numbers_dont_lie.mealplan.service.NutritionSummaryService;
import com.ndl.numbers_dont_lie.mealplan.service.WeeklyMealPlanService;
import com.ndl.numbers_dont_lie.mealplan.service.CustomMealService;
import com.ndl.numbers_dont_lie.mealplan.dto.AddCustomMealRequest;
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
    private final CustomMealService customMealService;
    private final MealRepository mealRepository;
    private final RecipeRepository recipeRepository;
    private final MealMoveService mealMoveService;
    
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
            MealPlanVersionRepository mealPlanVersionRepository,
            CustomMealService customMealService,
            MealRepository mealRepository,
            RecipeRepository recipeRepository,
            MealMoveService mealMoveService) {
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
        this.customMealService = customMealService;
        this.mealRepository = mealRepository;
        this.recipeRepository = recipeRepository;
        this.mealMoveService = mealMoveService;
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
        // 1) try existing persistent day plan (with meals eagerly loaded)
        Optional<DayPlan> existingPlan = dayPlanRepository.findByUserIdAndDateWithMealsAndDuration(
                userId, date, PlanDuration.DAILY);
        if (existingPlan.isPresent()) {
            DayPlan plan = existingPlan.get();
            logger.info("[MEAL_PLAN] Returning persisted plan with id={} and {} meals", plan.getId(), plan.getMeals().size());
            // DEBUG: Log each meal's recipe_id
            for (Meal meal : plan.getMeals()) {
                logger.info("[MEAL_PLAN_DEBUG] Meal: type={}, recipe_id={}, custom_name={}, id={}", 
                    meal.getMealType(), meal.getRecipeId(), meal.getCustomMealName(), meal.getId());
            }
            return ResponseEntity.ok(plan);
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
            // DEBUG: Log each meal's recipe_id
            for (Meal meal : saved.getMeals()) {
                logger.info("[MEAL_PLAN_DEBUG] Generated Meal: type={}, recipe_id={}, custom_name={}, id={}", 
                    meal.getMealType(), meal.getRecipeId(), meal.getCustomMealName(), meal.getId());
            }
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
            // 1) First, check if we have an existing day plan in DB (with meals eagerly loaded)
            Optional<DayPlan> existingPlan = dayPlanRepository.findByUserIdAndDateWithMealsAndDuration(
                    userId, date, PlanDuration.DAILY);
            
            DayPlan dayPlan;
            if (existingPlan.isPresent()) {
                // Use existing plan - no need to regenerate
                dayPlan = existingPlan.get();
                logger.info("[MEAL_PLAN] Using existing plan id={} with {} meals for nutrition summary", 
                    dayPlan.getId(), dayPlan.getMeals().size());
            } else {
                // No existing plan - try to generate one
                logger.info("[MEAL_PLAN] No existing plan found, attempting to generate for nutrition summary");
                
                MealPlanVersion tempVersion = new MealPlanVersion();
                tempVersion.setVersionNumber(0);
                tempVersion.setCreatedAt(LocalDateTime.now());
                tempVersion.setReason(VersionReason.INITIAL_GENERATION);
                
                dayPlan = dayPlanAssemblerService.assembleDayPlan(
                        userId,
                        date,
                        tempVersion
                );
            }
            
            // 2) Generate nutrition summary from day plan
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
    public ResponseEntity<?> getWeeklyPlan(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "startDate", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {
        
        // Validation: startDate is required
        if (startDate == null) {
            logger.warn("[WEEK_PLAN] Missing required parameter: startDate");
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing required parameter: startDate",
                "message", "startDate must be provided in ISO-8601 format (YYYY-MM-DD)",
                "example", "/api/meal-plans/week?userId=2&startDate=2026-01-24"
            ));
        }

        logger.info("[WEEK_PLAN] Loading weekly plan for userId={} startDate={}", userId, startDate);
        try {
            LocalDate endDate = startDate.plusDays(6);

            // 1) Try to load most recent persisted weekly plan from DB
            java.util.Optional<MealPlan> latestWeeklyPlan = mealPlanRepository
                .findTopByUserIdAndDurationOrderByIdDesc(userId, PlanDuration.WEEKLY);

            if (latestWeeklyPlan.isPresent()) {
                MealPlan plan = latestWeeklyPlan.get();
                java.util.List<DayPlan> persistedDays = dayPlanRepository
                    .findByMealPlanIdAndDateRangeWithMeals(plan.getId(), startDate, endDate);
                if (!persistedDays.isEmpty()) {
                    logger.info("[WEEK_PLAN] Found {} persisted weekly day plans (planId={})", persistedDays.size(), plan.getId());

                    // Fill missing dates with empty placeholders (do not persist)
                    java.util.Map<LocalDate, DayPlan> byDate = new java.util.HashMap<>();
                    for (DayPlan day : persistedDays) {
                        byDate.put(day.getDate(), day);
                    }
                    java.util.List<DayPlan> fullWeek = new java.util.ArrayList<>();
                    for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                        LocalDate date = startDate.plusDays(dayOffset);
                        DayPlan dayPlan = byDate.get(date);
                        if (dayPlan == null) {
                            DayPlan placeholder = new DayPlan();
                            placeholder.setDate(date);
                            placeholder.setUserId(userId);
                            placeholder.setMeals(java.util.Collections.emptyList());
                            dayPlan = placeholder;
                        }
                        fullWeek.add(dayPlan);
                    }

                    WeeklyPlanResponse response = weeklyMealPlanService.buildWeeklyPlanResponse(startDate, fullWeek);
                    return ResponseEntity.ok(response);
                }
            }

            // 2) No persisted weekly plan found → generate and persist a new weekly plan
            logger.info("[WEEK_PLAN] No persisted weekly plan found, generating new plan");
            if (aiStrategyService.getCachedStrategy(String.valueOf(userId)) == null ||
                aiStrategyService.getCachedMealStructure(String.valueOf(userId)) == null) {
                logger.info("[WEEK_PLAN] AI cache missing, bootstrapping for userId={}", userId);
                bootstrapAiForUser(userId);
            }
            try {
                MealPlan savedPlan = weeklyMealPlanService.generateWeeklyPlan(userId, startDate);
                MealPlanVersion currentVersion = savedPlan.getCurrentVersion();
                java.util.List<DayPlan> dayPlans = currentVersion != null ? currentVersion.getDayPlans() : java.util.Collections.emptyList();
                dayPlans.sort(java.util.Comparator.comparing(DayPlan::getDate));

                WeeklyPlanResponse response = weeklyMealPlanService.buildWeeklyPlanResponse(startDate, dayPlans);
                return ResponseEntity.ok(response);
            } catch (IllegalStateException e) {
                String message = e.getMessage() != null ? e.getMessage() : "";
                if (message.contains("AI strategy not found") || message.contains("Meal structure not found")) {
                    logger.info("[WEEK_PLAN] Missing AI cache, auto-bootstrapping for userId={}", userId);
                    bootstrapAiForUser(userId);

                    MealPlan savedPlan = weeklyMealPlanService.generateWeeklyPlan(userId, startDate);
                    MealPlanVersion currentVersion = savedPlan.getCurrentVersion();
                    java.util.List<DayPlan> dayPlans = currentVersion != null ? currentVersion.getDayPlans() : java.util.Collections.emptyList();
                    dayPlans.sort(java.util.Comparator.comparing(DayPlan::getDate));

                    WeeklyPlanResponse response = weeklyMealPlanService.buildWeeklyPlanResponse(startDate, dayPlans);
                    return ResponseEntity.ok(response);
                }
                throw e;
            }
        } catch (Exception e) {
            logger.error("[WEEK_PLAN] Failed to load week plan: {}", e.getMessage(), e);
            LocalDate endDate = startDate.plusDays(6);
            WeeklyPlanResponse fallback = new WeeklyPlanResponse(
                startDate,
                endDate,
                java.util.Collections.emptyList(),
                new com.ndl.numbers_dont_lie.mealplan.dto.WeeklyNutritionSummary()
            );
            return ResponseEntity.ok(fallback);
        }
    }

    /**
     * Force regenerate weekly plan (new version).
     */
    @PostMapping("/week/refresh")
    public ResponseEntity<?> refreshWeeklyPlan(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "startDate", required = true)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate) {
        if (startDate == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Missing required parameter: startDate",
                "message", "startDate must be provided in ISO-8601 format (YYYY-MM-DD)"
            ));
        }

        try {
            if (aiStrategyService.getCachedStrategy(String.valueOf(userId)) == null ||
                aiStrategyService.getCachedMealStructure(String.valueOf(userId)) == null) {
                logger.info("[WEEK_PLAN] Refresh: AI cache missing, bootstrapping for userId={}", userId);
                bootstrapAiForUser(userId);
            }

            MealPlan savedPlan = weeklyMealPlanService.generateWeeklyPlan(userId, startDate);
            MealPlanVersion currentVersion = savedPlan.getCurrentVersion();
            java.util.List<DayPlan> dayPlans = currentVersion != null ? currentVersion.getDayPlans() : java.util.Collections.emptyList();
            dayPlans.sort(java.util.Comparator.comparing(DayPlan::getDate));

            WeeklyPlanResponse response = weeklyMealPlanService.buildWeeklyPlanResponse(startDate, dayPlans);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[WEEK_PLAN] Refresh failed: {}", e.getMessage(), e);
            LocalDate endDate = startDate.plusDays(6);
            WeeklyPlanResponse fallback = new WeeklyPlanResponse(
                startDate,
                endDate,
                java.util.Collections.emptyList(),
                new com.ndl.numbers_dont_lie.mealplan.dto.WeeklyNutritionSummary()
            );
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
     * Reorder meals within a day by moving a meal up or down in the time order.
     *
     * POST /api/meal-plans/meals/{mealId}/move?direction=up|down
     *
     * Swaps planned_time with the adjacent meal to preserve ordering.
     */
    @PostMapping("/meals/{mealId}/move")
    @Transactional
    public ResponseEntity<?> moveMeal(
            @PathVariable Long mealId,
            @RequestParam(name = "direction") String direction) {
        logger.info("[MEAL_MOVE] Request: mealId={}, direction={}", mealId, direction);
        DayPlan dayPlan = mealMoveService.moveMeal(mealId, direction);

        Map<String, Object> response = new HashMap<>();
        response.put("id", dayPlan.getId());
        response.put("date", dayPlan.getDate());
        response.put("user_id", dayPlan.getUserId());
        response.put("context_hash", dayPlan.getContextHash());
        response.put("meals", dayPlan.getMeals());

        return ResponseEntity.ok(response);
    }

    /**
     * Alias for moveMeal to support day-scoped routes.
     */
    @PostMapping("/day/meals/{mealId}/move")
    @Transactional
    public ResponseEntity<?> moveMealInDay(
            @PathVariable Long mealId,
            @RequestParam(name = "direction") String direction) {
        return moveMeal(mealId, direction);
    }
    
    /**
     * Add a custom meal to a day plan.
     * 
     * POST /api/meals/custom
     * 
     * Input JSON:
     * {
     *   "date": "YYYY-MM-DD",
     *   "meal_type": "breakfast|lunch|dinner|snack",
     *   "name": "Custom Meal Name"
     * }
     * 
     * Behavior:
     * - Creates a new custom meal for the specified date
     * - Marks meal with is_custom=true
     * - Does NOT affect generated meals
     * - Does NOT trigger regeneration
     * - Does NOT affect nutrition summary calculations
     * 
     * @param userId User ID (from request param)
     * @param request AddCustomMealRequest with date, meal_type, name
     * @return Created Meal object
     */
    @PostMapping("/meals/custom")
    @Transactional
    public ResponseEntity<?> addCustomMeal(
            @RequestParam(name = "userId") Long userId,
            @RequestBody AddCustomMealRequest request) {
        logger.info("[CUSTOM_MEAL_API] POST /meals/custom userId={}, request={}", userId, request);
        
        try {
            Meal customMeal = customMealService.addCustomMeal(userId, request);
            logger.info("[CUSTOM_MEAL_API] Custom meal added successfully: id={}, name='{}'", 
                customMeal.getId(), customMeal.getCustomMealName());
            return ResponseEntity.status(HttpStatus.CREATED).body(customMeal);
            
        } catch (IllegalArgumentException e) {
            logger.warn("[CUSTOM_MEAL_API] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            logger.error("[CUSTOM_MEAL_API] Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to add custom meal")
            );
        }
    }
    
    /**
     * Delete a custom meal.
     * 
     * DELETE /api/meals/custom/{mealId}
     * 
     * Rules:
     * - Only allows deletion if meal is marked as is_custom=true
     * - Deletion does NOT trigger regeneration
     * - Other meals in day plan remain unchanged
     * 
     * @param mealId Meal ID to delete
     * @param userId User ID (from request param)
     * @return 204 No Content if successful
     */
    @DeleteMapping("/meals/custom/{mealId}")
    @Transactional
    public ResponseEntity<?> deleteCustomMeal(
            @PathVariable Long mealId,
            @RequestParam(name = "userId") Long userId) {
        logger.info("[CUSTOM_MEAL_API] DELETE /meals/custom/{} userId={}", mealId, userId);
        
        try {
            customMealService.deleteCustomMeal(mealId, userId);
            logger.info("[CUSTOM_MEAL_API] Custom meal deleted successfully: id={}", mealId);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("[CUSTOM_MEAL_API] Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                Map.of("error", e.getMessage())
            );
        } catch (Exception e) {
            logger.error("[CUSTOM_MEAL_API] Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to delete custom meal")
            );
        }
    }
    
    /**
     * Refresh meal plan for a specific date.
     * 
     * Called when user updates profile and wants to see updated meal plan.
     * Deletes existing plan and regenerates based on current profile.
     * 
     * POST /api/meal-plans/day/refresh?userId=X&date=YYYY-MM-DD
     * 
     * @param userId User ID
     * @param date Date to refresh (defaults to today if omitted)
     * @return Newly generated DayPlan with updated meals
     */
    @PostMapping("/day/refresh")
    @Transactional
    public ResponseEntity<DayPlan> refreshDayPlan(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        // Use provided date or default to today
        if (date == null) {
            date = LocalDate.now();
        }
        
        logger.info("[MEAL_PLAN] REFRESH requested for userId={}, date={}", userId, date);
        
        try {
            // 1) Delete existing day plan to force regeneration (with meals)
            Optional<DayPlan> existingPlan = dayPlanRepository.findByUserIdAndDateWithMealsAndDuration(
                    userId, date, PlanDuration.DAILY);
            if (existingPlan.isPresent()) {
                logger.info("[MEAL_PLAN] Deleting existing plan id={} for refresh", existingPlan.get().getId());
                dayPlanRepository.delete(existingPlan.get());
            }
            
            // 2) Create temporary MealPlanVersion for assembly
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(LocalDateTime.now());
            tempVersion.setReason(VersionReason.INITIAL_GENERATION);
            
            // 3) Generate fresh DayPlan based on current profile
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(
                    userId,
                    date,
                    tempVersion
            );
            
            // 4) Persist refreshed plan
            DayPlan saved = persistDayPlan(userId, date, dayPlan);
            
            logger.info("[MEAL_PLAN] REFRESH SUCCESS! Generated {} meals for userId={}, date={}", 
                saved.getMeals().size(), userId, date);
            return ResponseEntity.ok(saved);
            
        } catch (IllegalStateException e) {
            // Check if this is "AI strategy not found" error
            if (e.getMessage() != null && e.getMessage().contains("AI strategy not found")) {
                logger.info("[MEAL_PLAN] REFRESH: Auto-generating AI strategy for userId={}", userId);
                
                try {
                    // Auto-bootstrap: Generate AI strategy
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
                    
                    DayPlan saved = persistDayPlan(userId, date, dayPlan);
                    
                    logger.info("[MEAL_PLAN] REFRESH with auto-bootstrap SUCCESS! Generated {} meals", 
                        saved.getMeals().size());
                    return ResponseEntity.ok(saved);
                    
                } catch (Exception bootstrapError) {
                    logger.error("[MEAL_PLAN] REFRESH with auto-bootstrap FAILED: {}", 
                        bootstrapError.getMessage(), bootstrapError);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
                }
            }
            
            logger.error("[MEAL_PLAN] REFRESH FAILED with IllegalStateException: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            
        } catch (Exception e) {
            logger.error("[MEAL_PLAN] REFRESH FAILED with unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * Replace a single meal in weekly plan by date and meal type.
     * 
     * POST /api/meal-plans/week/meals/replace?userId=X&date=YYYY-MM-DD&mealType=BREAKFAST
     * 
     * Since weekly plan meals are transient (not persisted with IDs), we identify them by
     * date and meal type. This endpoint generates a fresh meal for that slot.
     * 
     * @param userId User ID
     * @param date Date of the meal to replace
     * @param mealType Type of meal (BREAKFAST, LUNCH, DINNER, SNACK)
     * @return New Meal object (transient, no ID)
     */
    @PostMapping("/week/meals/replace")
    public ResponseEntity<?> replaceWeeklyMeal(
            @RequestParam(name = "userId") Long userId,
            @RequestParam(name = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(name = "mealType") String mealType) {
        
        logger.info("[WEEK_MEAL_REPLACE] Replace request: userId={}, date={}, mealType={}", 
            userId, date, mealType);
        
        try {
            // Parse meal type
            MealType type;
            try {
                type = MealType.valueOf(mealType.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("[WEEK_MEAL_REPLACE] Invalid meal type: {}", mealType);
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid meal type",
                    "message", "mealType must be one of: BREAKFAST, LUNCH, DINNER, SNACK",
                    "provided", mealType
                ));
            }
            
            // Generate a fresh meal for this date/type
            // We'll use the day plan assembler to generate just one meal
            MealPlanVersion tempVersion = new MealPlanVersion();
            tempVersion.setVersionNumber(0);
            tempVersion.setCreatedAt(LocalDateTime.now());
            tempVersion.setReason(VersionReason.MEAL_REGENERATION);
            
            // Generate full day plan (we'll extract the requested meal)
            DayPlan dayPlan = dayPlanAssemblerService.assembleDayPlan(userId, date, tempVersion);
            
            // Find the meal with matching type
            Meal newMeal = dayPlan.getMeals().stream()
                .filter(m -> m.getMealType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No meal found for type: " + type));
            
            logger.info("[WEEK_MEAL_REPLACE] Generated new meal: {} for {}", 
                newMeal.getCustomMealName(), type);
            
            return ResponseEntity.ok(newMeal);
            
        } catch (Exception e) {
            logger.error("[WEEK_MEAL_REPLACE] Failed to replace meal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to replace meal",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Get a specific meal by ID.
     * 
     * GET /api/meals/{mealId}
     * 
     * @param mealId ID of the meal
     * @return Meal object
     */
    @GetMapping("/{mealId}")
    public ResponseEntity<?> getMealById(@PathVariable Long mealId) {
        try {
            Optional<Meal> mealOpt = mealRepository.findById(mealId);
            if (!mealOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Meal not found",
                    "mealId", mealId
                ));
            }
            return ResponseEntity.ok(mealOpt.get());
        } catch (Exception e) {
            logger.error("[GET_MEAL] Failed to get meal {}: {}", mealId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to get meal",
                "message", e.getMessage()
            ));
        }
    }
    
    /**
     * Replace a meal with a recipe from the database.
     * 
     * PUT /api/meals/{mealId}/replace
     * Body: { "recipeId": "r00001" }
     * 
     * This endpoint allows users to replace ONE existing meal with a recipe selected from the database.
     * It preserves the meal type, day plan, and calorie target while updating the recipe.
     * 
     * @param mealId ID of the meal to replace
     * @param request Body with recipeId
     * @return Updated Meal object
     */
    @PutMapping("/{mealId}/replace")
    @Transactional
    public ResponseEntity<?> replaceMealWithRecipe(
            @PathVariable Long mealId,
            @RequestBody Map<String, String> request) {
        
        logger.info("[MEAL_REPLACE] Requested replace for mealId={}", mealId);
        
        try {
            String recipeId = request.get("recipeId");
            if (recipeId == null || recipeId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing recipeId",
                    "message", "Body must contain 'recipeId' field"
                ));
            }
            
            // Load meal by ID
            Optional<Meal> mealOpt = mealRepository.findById(mealId);
            if (!mealOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Meal not found",
                    "mealId", mealId
                ));
            }
            
            Meal oldMeal = mealOpt.get();
            String oldRecipeId = oldMeal.getRecipeId();
            String oldMealName = oldMeal.getCustomMealName();
            
            // Load recipe by stable ID
            Optional<Recipe> recipeOpt = recipeRepository.findByStableId(recipeId);
            if (!recipeOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Recipe not found",
                    "recipeId", recipeId
                ));
            }
            
            Recipe newRecipe = recipeOpt.get();
            
            // Update meal
            oldMeal.setRecipeId(newRecipe.getStableId());
            oldMeal.setCustomMealName(newRecipe.getTitle());
            // Keep calorie target unchanged
            
            // Save updated meal
            Meal savedMeal = mealRepository.save(oldMeal);
            
            logger.info("[MEAL_REPLACE] Old recipe={}, New recipe={}", oldRecipeId, newRecipe.getStableId());
            logger.info("[MEAL_REPLACE] Replace completed successfully");
            
            return ResponseEntity.ok(savedMeal);
            
        } catch (IllegalArgumentException e) {
            logger.error("[MEAL_REPLACE] Invalid argument: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid argument",
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("[MEAL_REPLACE] Failed to replace meal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to replace meal",
                "message", e.getMessage()
            ));
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
