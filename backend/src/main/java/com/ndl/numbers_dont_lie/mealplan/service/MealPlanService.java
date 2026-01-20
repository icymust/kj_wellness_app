package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.mealplan.dto.MealFrequency;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import com.ndl.numbers_dont_lie.mealplan.entity.VersionReason;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
/**
 * Service for meal plan lifecycle management.
 * 
 * DATA SOURCE ENFORCEMENT:
 * - Meal frequency MUST come from User Profile (Project 1)
 * - Timezone MUST come from User Profile (Project 1)
 * - Production entry point: createInitialPlanFromUserProfile()
 * - This ensures no duplicate data entry and reuses Project 1 data
 * 
 * Responsibilities:
 * - Generate initial placeholder meal plans
 * - Clone versions for regeneration or manual changes
 * - Restore previous versions
 * 
 * Does NOT:
 * - Call AI for recipe selection
 * - Calculate nutrition
 * - Validate recipe compatibility
 */
@Service
public class MealPlanService {

    private final MealPlanRepository mealPlanRepository;
    private final MealPlanVersionRepository versionRepository;
    private final UserProfileService userProfileService;

    // Default meal times (can be overridden in future)
    private static final LocalTime DEFAULT_BREAKFAST_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_LUNCH_TIME = LocalTime.of(12, 30);
    private static final LocalTime DEFAULT_DINNER_TIME = LocalTime.of(18, 30);
    private static final LocalTime DEFAULT_SNACK_TIME = LocalTime.of(15, 0);

    public MealPlanService(MealPlanRepository mealPlanRepository,
                          MealPlanVersionRepository versionRepository,
                          UserProfileService userProfileService) {
        this.mealPlanRepository = mealPlanRepository;
        this.versionRepository = versionRepository;
        this.userProfileService = userProfileService;
    }

    /**
     * Create initial meal plan from User Profile data.
     * 
     * PRODUCTION ENTRY POINT - Use this method in controllers/APIs.
     * 
     * This method enforces that meal frequency and timezone are fetched
     * from User Profile (Project 1), ensuring no duplicate data entry.
     * 
     * @param userId User ID
     * @param duration DAILY or WEEKLY
     * @return Created MealPlan with placeholder meals
     * @throws IllegalStateException if User Profile is not configured
     */
    @Transactional
    public MealPlan createInitialPlanFromUserProfile(Long userId, PlanDuration duration) {
        // Fetch data from User Profile (Project 1)
        MealFrequency mealFrequency = userProfileService.getMealFrequency(userId);
        String timezone = userProfileService.getTimezone(userId);
        
        // Delegate to internal plan generation
        return generateInitialPlan(userId, duration, timezone, mealFrequency);
    }

    /**
     * Internal method for generating meal plans.
     * 
     * INTERNAL USE ONLY - Not for direct controller/API usage.
     * 
     * MealFrequency MUST originate from User Profile, not ad-hoc creation.
     * This method is protected to allow:
     * - Testing with specific meal frequencies
     * - Internal service-layer reuse
     * 
     * Production code MUST use createInitialPlanFromUserProfile() instead.
     * 
     * Creates:
     * - MealPlan (root aggregate)
     * - MealPlanVersion (version 1, INITIAL_GENERATION)
     * - DayPlan(s) based on duration
     * - Meal slots based on meal frequency
     * 
     * All meals are placeholders:
     * - recipeId = null
     * - customMealName = null
     * 
     * @param userId User ID
     * @param duration DAILY or WEEKLY
     * @param timezone IANA timezone (e.g., "Europe/Tallinn")
     * @param mealFrequency Number of each meal type per day (from User Profile)
     * @return Created MealPlan
     */
    @Transactional
    protected MealPlan generateInitialPlan(Long userId, PlanDuration duration, 
                                       String timezone, MealFrequency mealFrequency) {
        // Create meal plan
        MealPlan mealPlan = new MealPlan(userId, duration, timezone);
        mealPlan = mealPlanRepository.save(mealPlan);

        // Create initial version
        MealPlanVersion version = new MealPlanVersion(mealPlan, 1, VersionReason.INITIAL_GENERATION);
        version = versionRepository.save(version);

        // Determine number of days
        int daysToGenerate = (duration == PlanDuration.DAILY) ? 1 : 7;
        LocalDate startDate = LocalDate.now();

        // Generate day plans with placeholder meals
        for (int i = 0; i < daysToGenerate; i++) {
            LocalDate planDate = startDate.plusDays(i);
            DayPlan dayPlan = new DayPlan(version, planDate);
            version.addDayPlan(dayPlan);

            // Create meal slots based on frequency
            createPlaceholderMeals(dayPlan, planDate, mealFrequency);
        }

        // Set as current version
        mealPlan.setCurrentVersion(version);
        return mealPlanRepository.save(mealPlan);
    }

    /**
     * Clone an existing version to create a new one.
     * 
     * Cloning logic:
     * 1. Deep copy all DayPlans from source version
     * 2. Deep copy all Meals within each DayPlan
     * 3. Create new version with incremented number
     * 4. Update MealPlan.currentVersion
     * 5. Original version remains unchanged
     * 
     * @param mealPlanId MealPlan ID
     * @param reason Why the version is being cloned
     * @return New MealPlanVersion
     */
    @Transactional
    public MealPlanVersion cloneVersion(Long mealPlanId, VersionReason reason) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
            .orElseThrow(() -> new IllegalArgumentException("MealPlan not found: " + mealPlanId));

        MealPlanVersion currentVersion = mealPlan.getCurrentVersion();
        if (currentVersion == null) {
            throw new IllegalStateException("No current version to clone");
        }

        // Create new version with incremented number
        int newVersionNumber = currentVersion.getVersionNumber() + 1;
        MealPlanVersion newVersion = new MealPlanVersion(mealPlan, newVersionNumber, reason);
        newVersion = versionRepository.save(newVersion);

        // Deep copy all day plans and meals
        for (DayPlan sourceDayPlan : currentVersion.getDayPlans()) {
            DayPlan clonedDayPlan = new DayPlan(newVersion, sourceDayPlan.getDate());
            newVersion.addDayPlan(clonedDayPlan);

            // Copy all meals
            for (Meal sourceMeal : sourceDayPlan.getMeals()) {
                Meal clonedMeal = new Meal(
                    clonedDayPlan,
                    sourceMeal.getMealType(),
                    sourceMeal.getIndex(),
                    sourceMeal.getPlannedTime()
                );
                clonedMeal.setRecipeId(sourceMeal.getRecipeId());
                clonedMeal.setCustomMealName(sourceMeal.getCustomMealName());
                clonedDayPlan.addMeal(clonedMeal);
            }
        }

        // Update current version pointer
        mealPlan.setCurrentVersion(newVersion);
        mealPlanRepository.save(mealPlan);

        return newVersion;
    }

    /**
     * Restore a previous version by setting it as current.
     * 
     * Restoration logic:
     * - Simply updates MealPlan.currentVersion pointer
     * - No data is deleted or modified
     * - All versions remain in history
     * 
     * @param mealPlanId MealPlan ID
     * @param versionId Version ID to restore
     * @return Updated MealPlan
     */
    @Transactional
    public MealPlan restoreVersion(Long mealPlanId, Long versionId) {
        MealPlan mealPlan = mealPlanRepository.findById(mealPlanId)
            .orElseThrow(() -> new IllegalArgumentException("MealPlan not found: " + mealPlanId));

        MealPlanVersion versionToRestore = versionRepository.findById(versionId)
            .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        // Validate version belongs to meal plan
        if (!versionToRestore.getMealPlan().getId().equals(mealPlanId)) {
            throw new IllegalArgumentException("Version does not belong to this meal plan");
        }

        // Update current version pointer
        mealPlan.setCurrentVersion(versionToRestore);
        return mealPlanRepository.save(mealPlan);
    }

    /**
     * Create placeholder meal slots for a day plan.
     * 
     * Placeholder characteristics:
     * - recipeId = null
     * - customMealName = null
     * - plannedTime = default time for meal type
     * 
     * Multiple meals of same type are differentiated by index.
     */
    private void createPlaceholderMeals(DayPlan dayPlan, LocalDate date, MealFrequency mealFrequency) {
        for (Map.Entry<MealType, Integer> entry : mealFrequency.getFrequency().entrySet()) {
            MealType mealType = entry.getKey();
            int count = entry.getValue();

            for (int index = 1; index <= count; index++) {
                LocalTime baseTime = getDefaultMealTime(mealType);
                // Offset multiple meals of same type by 30 minutes
                LocalTime adjustedTime = baseTime.plusMinutes((index - 1) * 30);
                LocalDateTime plannedTime = LocalDateTime.of(date, adjustedTime);

                Meal meal = new Meal(dayPlan, mealType, index, plannedTime);
                // recipeId and customMealName remain null (placeholders)
                dayPlan.addMeal(meal);
            }
        }
    }

    /**
     * Get default time for a meal type.
     */
    private LocalTime getDefaultMealTime(MealType mealType) {
        return switch (mealType) {
            case BREAKFAST -> DEFAULT_BREAKFAST_TIME;
            case LUNCH -> DEFAULT_LUNCH_TIME;
            case DINNER -> DEFAULT_DINNER_TIME;
            case SNACK -> DEFAULT_SNACK_TIME;
        };
    }
}
