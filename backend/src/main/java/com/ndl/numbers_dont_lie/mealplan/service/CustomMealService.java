package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.mealplan.dto.AddCustomMealRequest;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
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
    
    public CustomMealService(
            DayPlanRepository dayPlanRepository,
            MealRepository mealRepository) {
        this.dayPlanRepository = dayPlanRepository;
        this.mealRepository = mealRepository;
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
    public Meal addCustomMeal(Long userId, AddCustomMealRequest request) {
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
        Optional<DayPlan> dayPlanOpt = dayPlanRepository.findByUserIdAndDate(userId, request.getDate());
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
