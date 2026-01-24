package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.mealplan.dto.DailyNutritionSummary;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

/**
 * Service for aggregating and summarizing daily nutrition data.
 * 
 * Purpose: Provide nutrition visualization for STEP 6.3.1 - Nutrition Summary Backend.
 * 
 * Design Intent:
 * - NO AI involvement - pure data aggregation
 * - Aggregates nutrition from all meals in a DayPlan
 * - Compares totals to user-defined nutritional targets
 * - Calculates percentage progress towards goals
 * 
 * Current Limitations (MVP):
 * - Meal entity does NOT currently store GeneratedRecipe nutrition data
 * - Aggregation is read-only over existing DayPlan/Meal data; totals default to 0 when nutrition is absent
 * - Future enhancement: Add JSON field to Meal entity to store GeneratedRecipe with nutrition
 * 
 * Production Requirements:
 * - Persist GeneratedRecipe.NutritionInfo in Meal entity (JSON column)
 * - Load actual nutrition data from database
 * - Support portion adjustments and recalculations
 * - Historical tracking and trend analysis
 * 
 * Why This Design:
 * - Separation of concerns: visualization logic separate from data storage
 * - Easy to enhance when Meal entity stores nutrition
 * - Enables UI development without waiting for full persistence layer
 * - Debug-friendly with clear logging
 */
@Service
public class NutritionSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(NutritionSummaryService.class);
    
    private final NutritionalPreferencesRepository nutritionalPreferencesRepository;
    
    public NutritionSummaryService(NutritionalPreferencesRepository nutritionalPreferencesRepository) {
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
    }
    
    /**
     * Generate nutrition summary for a specific day.
     * 
     * Process:
     * 1. Load user nutritional targets from NutritionalPreferences
     * 2. Aggregate nutrition from all meals in DayPlan
     * 3. Calculate percentage of targets achieved
     * 4. Build DailyNutritionSummary DTO
     * 
    * Current Implementation:
    * - Aggregates using available DayPlan/Meal data (none stored → zeros)
    * - Calculates percentages against user targets (avoids divide-by-zero)
     * 
     * @param dayPlan The day plan containing meals
     * @param userId User ID for loading nutritional targets
     * @return DailyNutritionSummary with aggregated data and percentages
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED, readOnly = true)
    public DailyNutritionSummary generateSummary(DayPlan dayPlan, Long userId) {
        logger.debug("[NUTRITION-SUMMARY] Generating summary for date: {}, user: {}", 
            dayPlan.getDate(), userId);
        
        // Step 1: Load user nutritional targets
        NutritionalPreferences preferences = nutritionalPreferencesRepository
            .findByUserId(userId)
            .orElse(null);
        
        if (preferences == null) {
            logger.warn("[NUTRITION-SUMMARY] No nutritional preferences found for user: {}", userId);
            return createEmptySummary(dayPlan, "No nutritional targets set");
        }
        
        // Step 2: Get targets
        Integer targetCalories = preferences.getCalorieTarget();
        Integer targetProtein = preferences.getProteinTarget();
        Integer targetCarbs = preferences.getCarbsTarget();
        Integer targetFats = preferences.getFatsTarget();
        
        if (targetCalories == null || targetCalories == 0) {
            logger.warn("[NUTRITION-SUMMARY] User has no calorie target set");
            return createEmptySummary(dayPlan, "Please set your nutritional targets in Profile");
        }
        
        // Step 3: Aggregate nutrition from meals (read-only)
        DailyNutritionSummary summary = aggregateTotals(dayPlan, targetCalories, targetProtein, targetCarbs, targetFats);
        summary.setDate(dayPlan.getDate());
        
        logger.debug("[NUTRITION-SUMMARY] Summary generated: meals={}, cal={}/{} ({}%)", 
            dayPlan.getMeals().size(),
            Math.round(summary.getTotalCalories()),
            Math.round(summary.getTargetCalories()),
            Math.round(summary.getCaloriesPercentage()));
        
        return summary;
    }
    
    /**
     * Calculate percentage of target achieved.
     * Returns null if target is null or zero.
     */
    private double calculatePercentage(double actual, double target) {
        if (target <= 0.0) {
            return 0.0;
        }
        return (actual / target) * 100.0;
    }
    
    /**
     * Create empty summary when targets not set or data unavailable.
     */
    private DailyNutritionSummary createEmptySummary(DayPlan dayPlan, String reason) {
        DailyNutritionSummary summary = new DailyNutritionSummary();
        summary.setDate(dayPlan.getDate());
        summary.setTotalCalories(0.0);
        summary.setTotalProtein(0.0);
        summary.setTotalCarbs(0.0);
        summary.setTotalFats(0.0);
        summary.setTargetCalories(0.0);
        summary.setTargetProtein(0.0);
        summary.setTargetCarbs(0.0);
        summary.setTargetFats(0.0);
        summary.setCaloriesPercentage(0.0);
        summary.setProteinPercentage(0.0);
        summary.setCarbsPercentage(0.0);
        summary.setFatsPercentage(0.0);
        summary.setNutritionEstimated(false);
        
        logger.info("[NUTRITION-SUMMARY] Returning empty summary: {}", reason);
        return summary;
    }

    /**
     * Read-only aggregation of existing DayPlan/Meal data. Since Meal currently does not
     * persist nutrition, uses MVP estimation based on meal count and daily targets.
     * 
     * Estimation Strategy:
     * - Divide daily calorie target equally across all meals
     * - Apply standard macro ratios: protein=25%, carbs=45%, fats=30%
     * - Convert to grams: protein/carbs = calories / 4, fats = calories / 9
     * - Flag result as estimated for transparency
     */
    private DailyNutritionSummary aggregateTotals(
            DayPlan dayPlan,
            Integer targetCalories,
            Integer targetProtein,
            Integer targetCarbs,
            Integer targetFats) {
        double totalCalories = 0.0;
        double totalProtein = 0.0;
        double totalCarbs = 0.0;
        double totalFats = 0.0;
        boolean isEstimated = false;

        int mealCount = dayPlan.getMeals().size();
        
        // No nutrition stored on Meal yet; use estimation
        if (mealCount > 0 && targetCalories != null && targetCalories > 0) {
            logger.debug("[NUTRITION] Using MVP estimation: {} meals, {} cal target", 
                mealCount, targetCalories);
            
            // Estimate calories per meal (equal distribution)
            double caloriesPerMeal = targetCalories.doubleValue() / mealCount;
            totalCalories = targetCalories.doubleValue();
            
            // Apply standard macro ratios:
            // Protein: 25% of calories ÷ 4 cal/g = grams
            // Carbs: 45% of calories ÷ 4 cal/g = grams
            // Fats: 30% of calories ÷ 9 cal/g = grams
            totalProtein = (totalCalories * 0.25) / 4.0;
            totalCarbs = (totalCalories * 0.45) / 4.0;
            totalFats = (totalCalories * 0.30) / 9.0;
            
            isEstimated = true;
            
            logger.debug("[NUTRITION] Estimated totals: cal={}, pro={}g, carb={}g, fat={}g",
                Math.round(totalCalories), Math.round(totalProtein), 
                Math.round(totalCarbs), Math.round(totalFats));
        } else {
            logger.debug("[NUTRITION] No meals or no target, returning zeros");
        }

        double targetCal = targetCalories != null ? targetCalories.doubleValue() : 0.0;
        double targetPro = targetProtein != null ? targetProtein.doubleValue() : 0.0;
        double targetCarb = targetCarbs != null ? targetCarbs.doubleValue() : 0.0;
        double targetFat = targetFats != null ? targetFats.doubleValue() : 0.0;

        DailyNutritionSummary summary = new DailyNutritionSummary();
        summary.setTotalCalories(totalCalories);
        summary.setTotalProtein(totalProtein);
        summary.setTotalCarbs(totalCarbs);
        summary.setTotalFats(totalFats);
        summary.setTargetCalories(targetCal);
        summary.setTargetProtein(targetPro);
        summary.setTargetCarbs(targetCarb);
        summary.setTargetFats(targetFat);
        summary.setCaloriesPercentage(calculatePercentage(totalCalories, targetCal));
        summary.setProteinPercentage(calculatePercentage(totalProtein, targetPro));
        summary.setCarbsPercentage(calculatePercentage(totalCarbs, targetCarb));
        summary.setFatsPercentage(calculatePercentage(totalFats, targetFat));
        summary.setNutritionEstimated(isEstimated);

        return summary;
    }
    
    /**
     * FUTURE ENHANCEMENT: Load actual nutrition from Meal entity.
     * 
     * When Meal entity is enhanced to store GeneratedRecipe.NutritionInfo:
     * 
     * 1. Add JSON column to meals table:
     *    ALTER TABLE meals ADD COLUMN nutrition_data JSONB;
     * 
     * 2. Update Meal entity:
     *    @Column(columnDefinition = "jsonb")
     *    private GeneratedRecipe.NutritionInfo nutrition;
     * 
     * 3. Update DayPlanAssemblerService.convertToMeal():
     *    meal.setNutrition(generatedRecipe.getNutrition());
     * 
     * 4. Implement this method:
     *    private DailyNutritionSummary aggregateFromMeals(DayPlan dayPlan, targets) {
     *        double totalCal = 0, totalPro = 0, totalCarb = 0, totalFat = 0;
     *        for (Meal meal : dayPlan.getMeals()) {
     *            if (meal.getNutrition() != null) {
     *                totalCal += meal.getNutrition().getCaloriesPerServing();
     *                totalPro += meal.getNutrition().getProteinPerServing();
     *                // ... etc
     *            }
     *        }
     *        return buildSummary(totalCal, totalPro, totalCarb, totalFat, targets);
     *    }
     * 
     * 5. Replace placeholder logic with: aggregateFromMeals(dayPlan, preferences)
     */
}
