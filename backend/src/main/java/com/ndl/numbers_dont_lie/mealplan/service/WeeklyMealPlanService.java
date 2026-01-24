package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.mealplan.dto.DailyNutritionSummary;
import com.ndl.numbers_dont_lie.mealplan.dto.WeeklyNutritionSummary;
import com.ndl.numbers_dont_lie.mealplan.dto.WeeklyPlanResponse;
import com.ndl.numbers_dont_lie.mealplan.entity.*;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.service.NutritionSummaryService;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * STEP 5.2: Weekly Meal Plan Assembly Service
 * 
 * Orchestrates weekly meal plan generation by reusing daily assembly logic:
 * ┌─────────────────────────────────────────────────────────────────┐
 * │ WeeklyMealPlanService.generateWeeklyPlan()                      │
 * └─────────────────────────────────────────────────────────────────┘
 *                           ↓
 *     ┌─────────────────────────────────────────────┐
 *     │ For Day 1 → Day 7 (startDate + i)          │
 *     └─────────────────────────────────────────────┘
 *                           ↓
 *         ┌───────────────────────────────────┐
 *         │ DayPlanAssemblerService           │
 *         │   .assembleDayPlan()              │
 *         └───────────────────────────────────┘
 *                           ↓
 *                    (STEP 5.1 logic)
 *                           ↓
 *         ┌───────────────────────────────────┐
 *         │ STEP 4.1: Cached AI Strategy     │
 *         │ STEP 4.2: Cached Meal Structure  │
 *         │ STEP 4.3.1: RAG Retrieval        │
 *         │ STEP 4.3.2: Recipe Generation    │
 *         └───────────────────────────────────┘
 *                           ↓
 *     ┌─────────────────────────────────────────────┐
 *     │ Assemble 7 DayPlans into MealPlan          │
 *     └─────────────────────────────────────────────┘
 * 
 * DESIGN PRINCIPLES:
 * 1. Reuses STEP 5.1 (DayPlanAssemblerService) - NO duplication
 * 2. NO direct AI calls - all AI logic delegated to daily assembly
 * 3. Sequential generation (7 days, one at a time)
 * 4. Partial failure handling - one day failure doesn't break week
 * 5. Creates persistent MealPlan + MealPlanVersion structure
 * 
 * WHY REUSE DAILY ASSEMBLY:
 * - DayPlanAssemblerService already handles complete AI pipeline
 * - Each day is independent (same strategy/structure, different recipes)
 * - Reduces code duplication and maintenance burden
 * - Ensures consistency between daily and weekly generation
 * - Allows weekly logic to focus on orchestration, not AI details
 * 
 * WHY NO DIRECT AI CALLS:
 * - AI strategy (STEP 4.1) generated once, reused for all 7 days
 * - Meal structure (STEP 4.2) generated once, reused for all 7 days
 * - Recipe generation (STEP 4.3.2) happens per-meal in daily assembly
 * - Weekly service only coordinates dates and persistence
 * - Separation of concerns: weekly = orchestration, daily = AI pipeline
 * 
 * ERROR HANDLING:
 * - Individual day failure: logs error, creates placeholder DayPlan
 * - Partial week still useful: user can regenerate failed days
 * - Strategy/structure missing: throws exception (cannot proceed)
 * - User profile missing: throws exception (required for constraints)
 * 
 * LIMITATIONS (STEP 5.2):
 * - No versioning logic (implemented in future step)
 * - No customization per day (same strategy/structure for all)
 * - Sequential generation (no parallelization)
 * - No recipe reuse across days (generates fresh recipes daily)
 */
@Service
public class WeeklyMealPlanService {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyMealPlanService.class);
    
    private final DayPlanAssemblerService dayPlanAssembler;
    private final MealPlanRepository mealPlanRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NutritionSummaryService nutritionSummaryService;
    
    public WeeklyMealPlanService(
            DayPlanAssemblerService dayPlanAssembler,
            MealPlanRepository mealPlanRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            NutritionSummaryService nutritionSummaryService) {
        this.dayPlanAssembler = dayPlanAssembler;
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.nutritionSummaryService = nutritionSummaryService;
    }
    
    /**
     * Generate a complete weekly meal plan (7 consecutive days).
     * 
     * Prerequisites:
     * - User must have AI strategy cached (STEP 4.1)
     * - User must have meal structure cached (STEP 4.2)
     * - User must have profile configured
     * - AI results must be fresh (not expired)
     * 
     * Process:
     * 1. Fetch user and profile
     * 2. Create MealPlan and MealPlanVersion entities
     * 3. For each day (startDate → startDate + 6):
     *    a. Call DayPlanAssemblerService.assembleDayPlan() [STEP 5.1]
     *    b. Add DayPlan to version
     *    c. Handle failures gracefully
     * 4. Persist MealPlan with all versions and days
     * 5. Return complete MealPlan
     * 
     * @param userId User ID
     * @param startDate First day of the week (ISO 8601 date)
     * @return Complete MealPlan with 7 DayPlans
     * @throws IllegalStateException if prerequisites not met
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MealPlan generateWeeklyPlan(Long userId, LocalDate startDate) {
        logger.info("[WEEK_PLAN] Generating week for userId={} startDate={}", 
            userId, startDate);
        
        // Step 1: Fetch user and profile
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalStateException(
                "User profile not configured for user " + userId));
        
        String timezone = profile.getTimezone() != null ? profile.getTimezone() : "UTC";
        
        // Step 2: Create MealPlan (root aggregate)
        MealPlan mealPlan = new MealPlan(userId, PlanDuration.WEEKLY, timezone);
        
        // Step 3: Create MealPlanVersion (version 1, initial creation)
        MealPlanVersion version = new MealPlanVersion(
            mealPlan, 
            1, 
            VersionReason.INITIAL_GENERATION
        );
        
        // Step 4: Generate 7 consecutive DayPlans
        List<DayPlan> dayPlans = new ArrayList<>();
        List<DailyNutritionSummary> dailySummaries = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        Set<String> weekUsedTitles = new java.util.HashSet<>();

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate currentDate = startDate.plusDays(dayOffset);
            
            try {
                logger.debug("Generating day plan {}/7 for date {}", dayOffset + 1, currentDate);
                
                // STEP 5.1: Reuse daily assembly logic
                // This internally uses STEP 4.1 (strategy), STEP 4.2 (structure),
                // STEP 4.3.1 (RAG), and STEP 4.3.2 (recipe generation)
                DayPlan dayPlan = dayPlanAssembler.assembleDayPlan(userId, currentDate, version);

                // Soft constraint: log duplicate titles across the week
                dayPlan.getMeals().forEach(meal -> {
                    String title = meal.getCustomMealName();
                    if (title != null && weekUsedTitles.stream().anyMatch(t -> t.equalsIgnoreCase(title))) {
                        logger.info("[WEEK_PLAN] Repeat title across week: {} on {}", title, currentDate);
                    } else if (title != null) {
                        weekUsedTitles.add(title.toLowerCase());
                    }
                });
                
                dayPlans.add(dayPlan);
                DailyNutritionSummary summary = nutritionSummaryService.generateSummary(dayPlan, userId);
                dailySummaries.add(summary);
                successCount++;
                
                logger.info("[WEEK_PLAN] Day {} generated with {} meals", 
                    currentDate, dayPlan.getMeals().size());
                
            } catch (Exception e) {
                logger.error("Failed to generate day plan {}/7 for {}. Error: {}", 
                    dayOffset + 1, currentDate, e.getMessage(), e);
                
                // Create placeholder DayPlan on failure
                // Allows partial week to be saved and individual days regenerated later
                DayPlan placeholderDay = createPlaceholderDayPlan(version, currentDate);
                dayPlans.add(placeholderDay);
                DailyNutritionSummary emptySummary = new DailyNutritionSummary();
                emptySummary.setDate(currentDate);
                emptySummary.setNutritionEstimated(true);
                dailySummaries.add(emptySummary);
                failureCount++;
            }
        }
        
        // Step 5: Add all DayPlans to version
        for (DayPlan dayPlan : dayPlans) {
            version.addDayPlan(dayPlan);
        }
        
        // Step 6: Link version to MealPlan
        mealPlan.setCurrentVersion(version);
        mealPlan.getVersions().add(version);
        version.setMealPlan(mealPlan);
        
        // Step 7: Persist (cascade saves version and day plans)
        MealPlan savedPlan = mealPlanRepository.save(mealPlan);
        WeeklyNutritionSummary weeklySummary = aggregateWeeklyNutrition(dailySummaries);
        logger.info("[WEEK_PLAN] Weekly nutrition: cal={} target={} est={}",
            Math.round(weeklySummary.getTotalCalories()),
            Math.round(weeklySummary.getTargetCalories()),
            weeklySummary.isNutritionEstimated());
        
        logger.info("Weekly meal plan generation complete. Success: {}, Failures: {}", 
            successCount, failureCount);
        
        if (failureCount > 0) {
            logger.warn("Some days failed to generate. Partial week created with placeholders. " +
                "User can regenerate failed days individually.");
        }
        
        return savedPlan;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public WeeklyPlanResponse generateWeeklyPlanPreview(Long userId, LocalDate startDate) {
        logger.info("[WEEK_PLAN] Generating week for userId={} startDate={}", userId, startDate);

        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));

        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalStateException(
                "User profile not configured for user " + userId));

        String timezone = profile.getTimezone() != null ? profile.getTimezone() : "UTC";
        MealPlanVersion tempVersion = new MealPlanVersion();
        tempVersion.setVersionNumber(0);
        tempVersion.setCreatedAt(java.time.LocalDateTime.now());
        tempVersion.setReason(VersionReason.INITIAL_GENERATION);

        List<DayPlan> dayPlans = new ArrayList<>();
        List<DailyNutritionSummary> dailySummaries = new ArrayList<>();
        Set<String> weekUsedTitles = new java.util.HashSet<>();

        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate currentDate = startDate.plusDays(dayOffset);
            try {
                DayPlan dayPlan = dayPlanAssembler.assembleDayPlan(userId, currentDate, tempVersion);
                dayPlan.getMeals().forEach(meal -> {
                    String title = meal.getCustomMealName();
                    if (title != null && weekUsedTitles.stream().anyMatch(t -> t.equalsIgnoreCase(title))) {
                        logger.info("[WEEK_PLAN] Repeat title across week: {} on {}", title, currentDate);
                    } else if (title != null) {
                        weekUsedTitles.add(title.toLowerCase());
                    }
                });
                dayPlans.add(dayPlan);
                dailySummaries.add(nutritionSummaryService.generateSummary(dayPlan, userId));
                logger.info("[WEEK_PLAN] Day {} generated with {} meals", currentDate, dayPlan.getMeals().size());
            } catch (Exception e) {
                logger.error("[WEEK_PLAN] Failed to generate day {}: {}", currentDate, e.getMessage(), e);
                DayPlan placeholderDay = createPlaceholderDayPlan(tempVersion, currentDate);
                dayPlans.add(placeholderDay);
                DailyNutritionSummary emptySummary = new DailyNutritionSummary();
                emptySummary.setDate(currentDate);
                emptySummary.setNutritionEstimated(true);
                dailySummaries.add(emptySummary);
            }
        }

        WeeklyNutritionSummary weeklySummary = aggregateWeeklyNutrition(dailySummaries);
        return new WeeklyPlanResponse(dayPlans, weeklySummary);
    }

    private WeeklyNutritionSummary aggregateWeeklyNutrition(List<DailyNutritionSummary> dailySummaries) {
        WeeklyNutritionSummary weekly = new WeeklyNutritionSummary();
        double totalCal = 0, totalPro = 0, totalCarb = 0, totalFat = 0;
        double targetCal = 0, targetPro = 0, targetCarb = 0, targetFat = 0;
        boolean estimated = false;

        for (DailyNutritionSummary day : dailySummaries) {
            totalCal += safe(day.getTotalCalories());
            totalPro += safe(day.getTotalProtein());
            totalCarb += safe(day.getTotalCarbs());
            totalFat += safe(day.getTotalFats());
            targetCal += safe(day.getTargetCalories());
            targetPro += safe(day.getTargetProtein());
            targetCarb += safe(day.getTargetCarbs());
            targetFat += safe(day.getTargetFats());
            if (Boolean.TRUE.equals(day.isNutritionEstimated())) {
                estimated = true;
            }
        }

        weekly.setTotalCalories(totalCal);
        weekly.setTotalProtein(totalPro);
        weekly.setTotalCarbs(totalCarb);
        weekly.setTotalFats(totalFat);
        weekly.setTargetCalories(targetCal);
        weekly.setTargetProtein(targetPro);
        weekly.setTargetCarbs(targetCarb);
        weekly.setTargetFats(targetFat);
        weekly.setCaloriesPercentage(percent(totalCal, targetCal));
        weekly.setProteinPercentage(percent(totalPro, targetPro));
        weekly.setCarbsPercentage(percent(totalCarb, targetCarb));
        weekly.setFatsPercentage(percent(totalFat, targetFat));
        weekly.setNutritionEstimated(estimated);
        return weekly;
    }

    private double safe(Double value) {
        return value != null ? value : 0.0;
    }

    private double percent(double actual, double target) {
        if (target <= 0.0) return 0.0;
        return (actual / target) * 100.0;
    }
    
    /**
     * Create a placeholder DayPlan when daily generation fails.
     * 
     * Fallback Behavior:
     * - Creates DayPlan with correct date
     * - Links to parent MealPlanVersion
     * - Contains NO meals (empty list)
     * - Allows partial week to be persisted
     * - User can regenerate this specific day later
     * 
     * Why Placeholder Instead of Throwing:
     * - One day failure shouldn't block entire week
     * - Partial meal plan still provides value (6/7 days usable)
     * - Enables incremental fixes without full regeneration
     * - User can identify and retry failed days
     */
    private DayPlan createPlaceholderDayPlan(MealPlanVersion version, LocalDate date) {
        logger.debug("Creating placeholder DayPlan for date: {}", date);
        
        DayPlan placeholder = new DayPlan(version, date);
        // No meals added - empty list indicates generation failure
        
        return placeholder;
    }
    
    /**
     * Calculate end date for weekly plan (startDate + 6 days).
     * 
     * ISO 8601 Date Handling:
     * - Uses LocalDate (timezone-agnostic)
     * - Consecutive days via plusDays()
     * - startDate is day 0, endDate is day 6
     * - Example: startDate = 2024-01-15 → endDate = 2024-01-21
     */
    public LocalDate calculateEndDate(LocalDate startDate) {
        return startDate.plusDays(6);
    }
    
    /**
     * Get date range for a weekly plan.
     * Utility method for UI/API display.
     */
    public DateRange getWeekDateRange(MealPlan weeklyPlan) {
        if (weeklyPlan.getDuration() != PlanDuration.WEEKLY) {
            throw new IllegalArgumentException("MealPlan is not WEEKLY duration");
        }
        
        MealPlanVersion currentVersion = weeklyPlan.getCurrentVersion();
        if (currentVersion == null || currentVersion.getDayPlans().isEmpty()) {
            return null;
        }
        
        List<DayPlan> dayPlans = currentVersion.getDayPlans();
        LocalDate startDate = dayPlans.stream()
            .map(DayPlan::getDate)
            .min(LocalDate::compareTo)
            .orElse(null);
        
        LocalDate endDate = dayPlans.stream()
            .map(DayPlan::getDate)
            .max(LocalDate::compareTo)
            .orElse(null);
        
        return new DateRange(startDate, endDate);
    }
    
    /**
     * Simple date range container.
     */
    public static class DateRange {
        private final LocalDate startDate;
        private final LocalDate endDate;
        
        public DateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }
        
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        
        @Override
        public String toString() {
            return startDate + " to " + endDate;
        }
    }
}
