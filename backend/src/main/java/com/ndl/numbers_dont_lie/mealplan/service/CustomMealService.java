package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.mealplan.dto.AddCustomMealRequest;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealPlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing custom meals added by users.
 * 
 * Design intent:
 * - Custom meals are FULLY ISOLATED from generated meal logic
 * - They do NOT affect:
 *   - Profile-based generation
 *   - Meal frequency calculations
 *   - Nutrition summary calculations
 *   - Regeneration triggers
 * 
 * - Custom meals are stored in the same Meal table but marked with is_custom=true
 * - They can be deleted without any side effects
 */
@Service
public class CustomMealService {
    private static final Logger logger = LoggerFactory.getLogger(CustomMealService.class);
    
    private final DayPlanRepository dayPlanRepository;
    private final MealRepository mealRepository;
    private final MealPlanRepository mealPlanRepository;
    
    public CustomMealService(
            DayPlanRepository dayPlanRepository,
            MealRepository mealRepository,
            MealPlanRepository mealPlanRepository) {
        this.dayPlanRepository = dayPlanRepository;
        this.mealRepository = mealRepository;
        this.mealPlanRepository = mealPlanRepository;
    }
    
    /**
     * Add a custom meal to a user's day plan.
     * 
     * Process:
     * 1. Load or create DayPlan for (userId, date)
     * 2. Create new Meal:
     *    - is_custom = true
     *    - custom_meal_name = name
     *    - meal_type = parsed mealType
     *    - planned_time = midnight (placeholder)
     *    - recipeId = NULL (no recipe)
     * 3. Append to DayPlan.meals
     * 4. Persist
     * 
     * IMPORTANT:
     * - Does NOT trigger regeneration
     * - Does NOT affect existing meals
     * - Does NOT update context_hash
     * 
     * @param userId User ID
     * @param request AddCustomMealRequest with date, mealType, name
     * @return Created Meal entity
     * @throws IllegalArgumentException if inputs invalid
     */
    @Transactional
    public Meal addCustomMeal(Long userId, AddCustomMealRequest request, PlanDuration preferredDuration) {
        logger.info("[CUSTOM_MEAL] Adding custom meal for userId={}, request={}", userId, request);
        
        // Validate inputs
        if (request.getDate() == null) {
            throw new IllegalArgumentException("Date is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Meal name is required");
        }
        if (request.getMealType() == null || request.getMealType().trim().isEmpty()) {
            throw new IllegalArgumentException("Meal type is required");
        }
        
        // Parse meal type
        MealType mealType;
        try {
            mealType = MealType.valueOf(request.getMealType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid meal type: " + request.getMealType());
        }
        
        // Load DayPlan for (userId, date)
        Optional<DayPlan> dayPlanOpt = Optional.empty();
        if (preferredDuration != null) {
            dayPlanOpt = findDayPlanForDuration(userId, request.getDate(), preferredDuration);
        } else {
            dayPlanOpt = findDayPlanForDuration(userId, request.getDate(), PlanDuration.DAILY);
            if (dayPlanOpt.isEmpty()) {
                dayPlanOpt = findDayPlanForDuration(userId, request.getDate(), PlanDuration.WEEKLY);
            }
        }
        DayPlan dayPlan;
        
        if (dayPlanOpt.isPresent()) {
            dayPlan = dayPlanOpt.get();
            logger.debug("[CUSTOM_MEAL] Found existing DayPlan id={} for date={}", dayPlan.getId(), request.getDate());
        } else {
            logger.warn("[CUSTOM_MEAL] No DayPlan found for userId={}, date={}. Custom meal requires existing DayPlan.", 
                userId, request.getDate());
            throw new IllegalArgumentException("No meal plan exists for date: " + request.getDate());
        }
        
        // Create new custom Meal
        Meal customMeal = new Meal();
        customMeal.setDayPlan(dayPlan);
        customMeal.setMealType(mealType);
        customMeal.setIsCustom(true);
        customMeal.setCustomMealName(request.getName().trim());
        customMeal.setRecipeId(null); // Custom meals have no recipe
        
        // Set placeholder index and planned_time
        // Index: use next available index for this meal type
        int nextIndex = (int) dayPlan.getMeals().stream()
            .filter(m -> m.getMealType() == mealType)
            .count() + 1;
        customMeal.setIndex(nextIndex);
        
        // planned_time: use midnight as placeholder (not used for custom meals)
        customMeal.setPlannedTime(
            LocalDateTime.of(request.getDate(), 
                java.time.LocalTime.MIDNIGHT)
        );
        
        // Append to DayPlan
        dayPlan.getMeals().add(customMeal);
        
        // Persist (cascade via DayPlan)
        mealRepository.save(customMeal);
        
        logger.info("[CUSTOM_MEAL] Added custom meal '{}' id={} userId={} date={}", 
            request.getName(), customMeal.getId(), userId, request.getDate());
        
        return customMeal;
    }

    private Optional<DayPlan> findDayPlanForDuration(Long userId, java.time.LocalDate date, PlanDuration duration) {
        if (duration == PlanDuration.WEEKLY) {
            return findWeeklyDayPlanForDate(userId, date);
        }
        return mealPlanRepository.findTopByUserIdAndDurationOrderByIdDesc(userId, duration)
            .flatMap(plan -> dayPlanRepository.findByMealPlanIdAndDateWithMeals(plan.getId(), date));
    }

    private Optional<DayPlan> findWeeklyDayPlanForDate(Long userId, java.time.LocalDate date) {
        java.time.LocalDate weekStart = getWeekStart(date);
        Optional<MealPlan> planOpt = findWeeklyPlanByStartDate(userId, weekStart);
        if (planOpt.isPresent()) {
            MealPlan plan = planOpt.get();
            MealPlanVersion currentVersion = plan.getCurrentVersion();
            if (currentVersion != null && currentVersion.getId() != null) {
                Optional<DayPlan> dayOpt = dayPlanRepository
                    .findByMealPlanVersionIdAndDateWithMeals(currentVersion.getId(), date);
                if (dayOpt.isPresent()) {
                    return dayOpt;
                }
            }
        }

        List<MealPlan> plans = mealPlanRepository.findByUserId(userId);
        DayPlan best = null;
        int bestMealCount = -1;
        int bestVersion = -1;
        Long bestPlanId = null;

        for (MealPlan plan : plans) {
            if (plan == null || plan.getDuration() != PlanDuration.WEEKLY) {
                continue;
            }
            MealPlanVersion currentVersion = plan.getCurrentVersion();
            if (currentVersion == null || currentVersion.getId() == null) {
                continue;
            }
            Optional<DayPlan> dayOpt = dayPlanRepository.findByMealPlanVersionIdAndDateWithMeals(currentVersion.getId(), date);
            if (dayOpt.isEmpty()) {
                continue;
            }
            DayPlan dayPlan = dayOpt.get();
            int mealCount = dayPlan.getMeals() != null ? dayPlan.getMeals().size() : 0;
            int versionNumber = currentVersion.getVersionNumber() != null ? currentVersion.getVersionNumber() : 0;
            Long planId = plan.getId();

            if (mealCount > bestMealCount
                || (mealCount == bestMealCount && versionNumber > bestVersion)
                || (mealCount == bestMealCount && versionNumber == bestVersion
                    && bestPlanId != null && planId != null && planId > bestPlanId)) {
                best = dayPlan;
                bestMealCount = mealCount;
                bestVersion = versionNumber;
                bestPlanId = planId;
            }
        }

        return Optional.ofNullable(best);
    }

    private java.time.LocalDate getWeekStart(java.time.LocalDate date) {
        java.time.DayOfWeek dow = date.getDayOfWeek();
        int diff = dow == java.time.DayOfWeek.SUNDAY ? -6 : java.time.DayOfWeek.MONDAY.getValue() - dow.getValue();
        return date.plusDays(diff);
    }

    private Optional<MealPlan> findWeeklyPlanByStartDate(Long userId, java.time.LocalDate startDate) {
        java.time.LocalDate endDate = startDate.plusDays(6);
        MealPlan bestPlan = null;
        int bestDayCount = -1;
        int bestMealCount = -1;
        int bestVersionNumber = -1;
        Long bestPlanId = null;

        List<MealPlan> plans = mealPlanRepository.findByUserId(userId);
        for (MealPlan plan : plans) {
            if (plan == null || plan.getDuration() != PlanDuration.WEEKLY) {
                continue;
            }
            MealPlanVersion currentVersion = plan.getCurrentVersion();
            if (currentVersion == null || currentVersion.getId() == null) {
                continue;
            }
            List<DayPlan> dayPlans = dayPlanRepository
                .findByMealPlanVersionIdAndDateRangeWithMeals(currentVersion.getId(), startDate, endDate);
            if (dayPlans.isEmpty()) {
                continue;
            }
            int dayCount = dayPlans.size();
            int mealCount = 0;
            for (DayPlan dayPlan : dayPlans) {
                mealCount += dayPlan.getMeals() != null ? dayPlan.getMeals().size() : 0;
            }
            int versionNumber = currentVersion.getVersionNumber() != null ? currentVersion.getVersionNumber() : 0;
            Long planId = plan.getId();

            if (dayCount > bestDayCount
                || (dayCount == bestDayCount && mealCount > bestMealCount)
                || (dayCount == bestDayCount && mealCount == bestMealCount && versionNumber > bestVersionNumber)
                || (dayCount == bestDayCount && mealCount == bestMealCount && versionNumber == bestVersionNumber
                    && bestPlanId != null && planId != null && planId > bestPlanId)) {
                bestPlan = plan;
                bestDayCount = dayCount;
                bestMealCount = mealCount;
                bestVersionNumber = versionNumber;
                bestPlanId = planId;
            }
        }

        return Optional.ofNullable(bestPlan);
    }
    
    /**
     * Delete a custom meal.
     * 
     * Rules:
     * - Only allow deletion if is_custom = true
     * - Deleting does NOT trigger regeneration
     * - Other meals in DayPlan remain unchanged
     * 
     * @param mealId Meal ID to delete
     * @param userId User ID (for authorization)
     * @throws IllegalArgumentException if meal not found or is not custom
     */
    @Transactional
    public void deleteCustomMeal(Long mealId, Long userId) {
        logger.info("[CUSTOM_MEAL] Deleting custom meal id={} userId={}", mealId, userId);
        
        // Load meal
        Optional<Meal> mealOpt = mealRepository.findById(mealId);
        if (!mealOpt.isPresent()) {
            throw new IllegalArgumentException("Meal not found: " + mealId);
        }
        
        Meal meal = mealOpt.get();
        
        // Verify it's custom
        if (!meal.getIsCustom()) {
            throw new IllegalArgumentException("Cannot delete non-custom meal: " + mealId);
        }
        
        // Verify ownership (userId matches DayPlan)
        if (!meal.getDayPlan().getUserId().equals(userId)) {
            throw new IllegalArgumentException("User not authorized to delete this meal");
        }
        
        // Remove from DayPlan
        DayPlan dayPlan = meal.getDayPlan();
        dayPlan.getMeals().remove(meal);
        
        // Delete from database
        mealRepository.delete(meal);
        
        logger.info("[CUSTOM_MEAL] Deleted custom meal id={} userId={}", mealId, userId);
    }
}
