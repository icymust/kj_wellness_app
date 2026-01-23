package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.entity.VersionReason;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * STEP 5.3: MealPlan Versioning Service
 *
 * Manages version history and provides regeneration/restoration capabilities.
 *
 * SNAPSHOT-BASED VERSIONING DESIGN:
 * ═════════════════════════════════
 * 
 * Why Full Snapshots?
 * ───────────────────
 * 1. HISTORY PRESERVATION
 *    - Each version is a complete, independent snapshot
 *    - No fragmentation or dependency on other versions
 *    - User can inspect any historical version in full detail
 *
 * 2. EASY RESTORATION
 *    - Restore simply clones the entire snapshot as new version
 *    - No complex merge/diff logic needed
 *    - Atomic operation - either succeeds completely or fails
 *
 * 3. TESTING SUPPORT
 *    - Snapshots enable A/B testing different versions
 *    - Can compare versions side-by-side
 *    - Reproducible results for regression testing
 *
 * 4. AUDIT TRAIL
 *    - Full snapshot at each point in time
 *    - Reason field explains why version was created
 *    - Timestamps enable chronological analysis
 *
 * 5. INDEPENDENT REGENERATION
 *    - Regenerate any version without affecting others
 *    - Each version can evolve independently
 *    - Previous versions remain unchanged
 *
 * Data Flow:
 * ──────────
 * Version 1 (INITIAL_GENERATION)
 * ├── DayPlan 1 → Meal 1, Meal 2, Meal 3
 * ├── DayPlan 2 → Meal 1, Meal 2, Meal 3
 * └── ...
 * 
 * User regenerates some recipes...
 * 
 * Version 2 (REGENERATED)
 * ├── DayPlan 1 → Meal 1, Meal 2 (updated), Meal 3
 * ├── DayPlan 2 → Meal 1, Meal 2, Meal 3 (updated)
 * └── ... (all 7 days complete)
 * 
 * User likes Version 1 better, restores it...
 * 
 * Version 3 (RESTORED from Version 1)
 * ├── [Clone of Version 1 DayPlans]
 * ├── ...
 * └── (Version 1 and 2 remain unchanged in history)
 *
 * REGENERATION FLOW:
 * ═════════════════
 * 1. Fetch current MealPlan + UserPreferences
 * 2. Call WeeklyMealPlanService with same startDate + userId
 * 3. Get new MealPlanVersion with fresh recipes
 * 4. Increment version number
 * 5. Set reason to REGENERATED
 * 6. Make it currentVersion
 * 7. Persist (old versions remain in DB)
 *
 * RESTORATION FLOW:
 * ════════════════
 * 1. Fetch specified historical version (immutable snapshot)
 * 2. Clone all DayPlans from snapshot
 * 3. Create new MealPlanVersion with cloned data
 * 4. Increment version number from current max
 * 5. Set reason to RESTORED
 * 6. Make it currentVersion
 * 7. Persist (all previous versions remain)
 *
 * VERSION HISTORY PRESERVATION:
 * ════════════════════════════
 * MealPlan.versions list never removes old versions.
 * User can always restore any previous version without loss.
 *
 * Performance Considerations:
 * ──────────────────────────
 * - Each version clones entire DayPlan + Meal tree (~21 meals/week)
 * - Storage cost: ~50-100KB per version
 * - For MVP: sufficient, can optimize later with compression
 * - For production: consider differential storage or archiving
 */
@Service
public class MealPlanVersionService {
    private static final Logger logger = LoggerFactory.getLogger(MealPlanVersionService.class);
    
    private final MealPlanRepository mealPlanRepository;
    private final WeeklyMealPlanService weeklyMealPlanService;
    
    public MealPlanVersionService(
            MealPlanRepository mealPlanRepository,
            WeeklyMealPlanService weeklyMealPlanService) {
        this.mealPlanRepository = mealPlanRepository;
        this.weeklyMealPlanService = weeklyMealPlanService;
    }
    
    /**
     * Regenerate a meal plan by creating a new version with fresh recipes.
     *
     * Process:
     * 1. Fetch current MealPlan
     * 2. Extract start date from current version
     * 3. Call WeeklyMealPlanService to generate new recipes
     * 4. Create new version with reason=REGENERATED
     * 5. Update MealPlan.currentVersion
     * 6. Persist
     *
     * Result:
     * - New version created with fresh recipes
     * - All previous versions remain in history
     * - User can restore old version at any time
     * - Version number incremented
     *
     * @param planId MealPlan ID
     * @param userId User ID (for preferences)
     * @return Updated MealPlan with new version
     * @throws IllegalStateException if plan not found or incompatible
     */
    @Transactional
    public MealPlan regenerateMealPlan(Long planId, Long userId) {
        logger.info("Starting meal plan regeneration for planId={}, userId={}", planId, userId);
        
        // Step 1: Fetch current MealPlan
        MealPlan mealPlan = mealPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalStateException("MealPlan not found: " + planId));
        
        // Step 2: Validate ownership
        if (!mealPlan.getUserId().equals(userId)) {
            throw new IllegalStateException("User " + userId + " does not own MealPlan " + planId);
        }
        
        // Step 3: Extract start date from current version
        MealPlanVersion currentVersion = mealPlan.getCurrentVersion();
        if (currentVersion == null || currentVersion.getDayPlans().isEmpty()) {
            throw new IllegalStateException("Current version has no day plans");
        }
        
        LocalDate startDate = currentVersion.getDayPlans().stream()
            .map(DayPlan::getDate)
            .min(LocalDate::compareTo)
            .orElseThrow(() -> new IllegalStateException("Cannot determine start date"));
        
        logger.debug("Extracted start date: {}", startDate);
        
        // Step 4: Generate new weekly plan (STEP 5.2 logic)
        // This creates a completely new MealPlan with fresh recipes
        MealPlan newGeneratedPlan = weeklyMealPlanService.generateWeeklyPlan(userId, startDate);
        
        // Step 5: Extract new version from generated plan
        MealPlanVersion generatedVersion = newGeneratedPlan.getCurrentVersion();
        
        // Step 6: Create regeneration version for existing MealPlan
        Integer newVersionNumber = mealPlan.getVersions().stream()
            .map(MealPlanVersion::getVersionNumber)
            .max(Integer::compareTo)
            .orElse(0) + 1;
        
        MealPlanVersion regeneratedVersion = new MealPlanVersion(
            mealPlan,
            newVersionNumber,
            VersionReason.REGENERATED
        );
        
        // Step 7: Copy DayPlans from generated version
        for (DayPlan generatedDay : generatedVersion.getDayPlans()) {
            DayPlan clonedDay = cloneDayPlan(generatedDay, regeneratedVersion);
            regeneratedVersion.addDayPlan(clonedDay);
        }
        
        // Step 8: Update MealPlan
        mealPlan.setCurrentVersion(regeneratedVersion);
        mealPlan.getVersions().add(regeneratedVersion);
        
        // Step 9: Persist
        MealPlan savedPlan = mealPlanRepository.save(mealPlan);
        
        logger.info("Meal plan regeneration complete. New version: {}", newVersionNumber);
        
        return savedPlan;
    }
    
    /**
     * Restore a previous version of a meal plan.
     *
     * Process:
     * 1. Fetch current MealPlan
     * 2. Find specified historical version
     * 3. Clone all DayPlans from historical version
     * 4. Create new version with reason=RESTORED
     * 5. Update MealPlan.currentVersion
     * 6. Persist (all previous versions remain)
     *
     * Result:
     * - New version created as clone of historical snapshot
     * - Original versions remain unchanged
     * - Version number incremented
     * - User can restore any previous version without loss
     *
     * @param planId MealPlan ID
     * @param versionNumber Version to restore (1, 2, 3, ...)
     * @param userId User ID (for permission check)
     * @return Updated MealPlan with restored version
     * @throws IllegalStateException if plan/version not found
     */
    @Transactional
    public MealPlan restoreVersion(Long planId, Integer versionNumber, Long userId) {
        logger.info("Starting version restore for planId={}, versionNumber={}, userId={}",
            planId, versionNumber, userId);
        
        // Step 1: Fetch current MealPlan
        MealPlan mealPlan = mealPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalStateException("MealPlan not found: " + planId));
        
        // Step 2: Validate ownership
        if (!mealPlan.getUserId().equals(userId)) {
            throw new IllegalStateException("User " + userId + " does not own MealPlan " + planId);
        }
        
        // Step 3: Find historical version to restore
        MealPlanVersion historicalVersion = mealPlan.getVersions().stream()
            .filter(v -> v.getVersionNumber().equals(versionNumber))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "Version " + versionNumber + " not found in MealPlan " + planId));
        
        logger.debug("Found historical version: {}", versionNumber);
        
        // Step 4: Create new version number
        Integer newVersionNumber = mealPlan.getVersions().stream()
            .map(MealPlanVersion::getVersionNumber)
            .max(Integer::compareTo)
            .orElse(0) + 1;
        
        // Step 5: Create restored version
        MealPlanVersion restoredVersion = new MealPlanVersion(
            mealPlan,
            newVersionNumber,
            VersionReason.RESTORED
        );
        
        // Step 6: Clone all DayPlans from historical version
        for (DayPlan historicalDay : historicalVersion.getDayPlans()) {
            DayPlan clonedDay = cloneDayPlan(historicalDay, restoredVersion);
            restoredVersion.addDayPlan(clonedDay);
        }
        
        // Step 7: Update MealPlan
        mealPlan.setCurrentVersion(restoredVersion);
        mealPlan.getVersions().add(restoredVersion);
        
        // Step 8: Persist
        MealPlan savedPlan = mealPlanRepository.save(mealPlan);
        
        logger.info("Version restore complete. Restored version {} as version {}",
            versionNumber, newVersionNumber);
        
        return savedPlan;
    }
    
    /**
     * Get all versions for a meal plan in chronological order.
     *
     * @param planId MealPlan ID
     * @return List of all versions (earliest first)
     * @throws IllegalStateException if plan not found
     */
    @Transactional(readOnly = true)
    public List<MealPlanVersion> getAllVersions(Long planId) {
        MealPlan mealPlan = mealPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalStateException("MealPlan not found: " + planId));
        
        return mealPlan.getVersions().stream()
            .sorted((v1, v2) -> v1.getVersionNumber().compareTo(v2.getVersionNumber()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get version history summary.
     *
     * Useful for UI display showing evolution of meal plan.
     *
     * @param planId MealPlan ID
     * @return List of version summaries
     */
    @Transactional(readOnly = true)
    public List<VersionSummary> getVersionHistory(Long planId) {
        return getAllVersions(planId).stream()
            .map(v -> new VersionSummary(
                v.getVersionNumber(),
                v.getReason(),
                v.getCreatedAt(),
                v.getDayPlans().size()
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Get current active version.
     *
     * @param planId MealPlan ID
     * @return Current MealPlanVersion
     */
    @Transactional(readOnly = true)
    public MealPlanVersion getCurrentVersion(Long planId) {
        MealPlan mealPlan = mealPlanRepository.findById(planId)
            .orElseThrow(() -> new IllegalStateException("MealPlan not found: " + planId));
        
        return mealPlan.getCurrentVersion();
    }
    
    /**
     * Clone a DayPlan including all its meals.
     *
     * Deep copy of entire DayPlan tree:
     * DayPlan → List<Meal>
     *
     * Used for restoration and regeneration snapshots.
     */
    private DayPlan cloneDayPlan(DayPlan source, MealPlanVersion newVersion) {
        DayPlan clonedDay = new DayPlan(newVersion, source.getDate());
        
        // Clone all meals
        for (Meal sourceMeal : source.getMeals()) {
            Meal clonedMeal = cloneMeal(sourceMeal, clonedDay);
            clonedDay.getMeals().add(clonedMeal);
        }
        
        return clonedDay;
    }
    
    /**
     * Clone a Meal including all its properties.
     *
     * Deep copy of Meal entity with all fields.
     */
    private Meal cloneMeal(Meal source, DayPlan parentDay) {
        Meal clonedMeal = new Meal(
            parentDay,
            source.getMealType(),
            source.getIndex(),
            source.getPlannedTime()
        );
        
        clonedMeal.setRecipeId(source.getRecipeId());
        clonedMeal.setCustomMealName(source.getCustomMealName());
        
        return clonedMeal;
    }
    
    /**
     * Simple summary of a version for UI display.
     */
    public static class VersionSummary {
        private final Integer versionNumber;
        private final VersionReason reason;
        private final LocalDateTime createdAt;
        private final Integer dayCount;
        
        public VersionSummary(Integer versionNumber, VersionReason reason, LocalDateTime createdAt, Integer dayCount) {
            this.versionNumber = versionNumber;
            this.reason = reason;
            this.createdAt = createdAt;
            this.dayCount = dayCount;
        }
        
        public Integer getVersionNumber() { return versionNumber; }
        public VersionReason getReason() { return reason; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public Integer getDayCount() { return dayCount; }
        
        @Override
        public String toString() {
            return String.format("Version %d [%s] - %d days (%s)",
                versionNumber, reason.getJsonValue(), dayCount, createdAt);
        }
    }
}
