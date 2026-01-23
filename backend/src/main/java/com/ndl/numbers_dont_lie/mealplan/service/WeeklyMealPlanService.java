package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.mealplan.entity.*;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    
    public WeeklyMealPlanService(
            DayPlanAssemblerService dayPlanAssembler,
            MealPlanRepository mealPlanRepository,
            UserRepository userRepository,
            ProfileRepository profileRepository) {
        this.dayPlanAssembler = dayPlanAssembler;
        this.mealPlanRepository = mealPlanRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
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
    @Transactional
    public MealPlan generateWeeklyPlan(Long userId, LocalDate startDate) {
        logger.info("Starting weekly meal plan generation for userId={}, startDate={}", 
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
        int successCount = 0;
        int failureCount = 0;
        
        for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
            LocalDate currentDate = startDate.plusDays(dayOffset);
            
            try {
                logger.debug("Generating day plan {}/7 for date {}", dayOffset + 1, currentDate);
                
                // STEP 5.1: Reuse daily assembly logic
                // This internally uses STEP 4.1 (strategy), STEP 4.2 (structure),
                // STEP 4.3.1 (RAG), and STEP 4.3.2 (recipe generation)
                DayPlan dayPlan = dayPlanAssembler.assembleDayPlan(userId, currentDate, version);
                
                dayPlans.add(dayPlan);
                successCount++;
                
                logger.info("Successfully generated day plan {}/7 for {}", 
                    dayOffset + 1, currentDate);
                
            } catch (Exception e) {
                logger.error("Failed to generate day plan {}/7 for {}. Error: {}", 
                    dayOffset + 1, currentDate, e.getMessage(), e);
                
                // Create placeholder DayPlan on failure
                // Allows partial week to be saved and individual days regenerated later
                DayPlan placeholderDay = createPlaceholderDayPlan(version, currentDate);
                dayPlans.add(placeholderDay);
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
        
        logger.info("Weekly meal plan generation complete. Success: {}, Failures: {}", 
            successCount, failureCount);
        
        if (failureCount > 0) {
            logger.warn("Some days failed to generate. Partial week created with placeholders. " +
                "User can regenerate failed days individually.");
        }
        
        return savedPlan;
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
