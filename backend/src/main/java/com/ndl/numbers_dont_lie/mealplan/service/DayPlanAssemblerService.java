package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.ai.AiStrategyService;
import com.ndl.numbers_dont_lie.ai.RecipeGenerationService;
import com.ndl.numbers_dont_lie.ai.RecipeRetrievalService;
import com.ndl.numbers_dont_lie.ai.dto.*;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * STEP 5.1: DayPlan Assembly Service
 * 
 * Orchestrates the complete AI pipeline to generate a daily meal plan:
 * ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
 * │ STEP 4.1 │ →  │ STEP 4.2 │ →  │STEP 4.3.1│ →  │STEP 4.3.2│ →  │ DayPlan  │
 * │ Strategy │    │MealSlots │    │Retrieval │    │Generation│    │Assembly  │
 * └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
 *      ↓               ↓               ↓               ↓               ↓
 *   Reuse          Reuse          Query           Generate        Assemble
 *   Cached         Cached         Similar         Recipe          Meals
 *   Strategy       Structure      Recipes         per Slot        into Day
 * 
 * DESIGN PRINCIPLES:
 * 1. Reuses AI strategy and meal structure (Steps 4.1, 4.2) - NO regeneration
 * 2. Generates ONE recipe per meal slot using RAG + function calling (Steps 4.3.1, 4.3.2)
 * 3. Assembles all meals into a single DayPlan
 * 4. Handles partial failures gracefully - one failed meal doesn't break entire day
 * 5. Backend logic handles assembly - AI only generates individual recipes
 * 
 * WHY PER-MEAL AI GENERATION:
 * - Each meal has unique calorie targets and macro requirements
 * - Different meal types require different recipe styles (breakfast vs dinner)
 * - RAG can retrieve relevant recipes specific to each meal context
 * - Function calling ensures accurate nutrition per meal
 * - Parallel generation possible in future (not implemented yet)
 * 
 * WHY BACKEND ASSEMBLY:
 * - AI excels at single recipe generation, not structured plan creation
 * - Backend ensures data consistency (meal count, order, types)
 * - Backend enforces business rules (meal frequency, timing)
 * - Backend handles persistence and error recovery
 * - Separation of concerns: AI for content, backend for structure
 * 
 * ERROR HANDLING:
 * - Individual meal generation failure: logs error, creates placeholder meal
 * - Partial plan still useful: user can regenerate failed meals individually
 * - Strategy/structure missing: throws exception (cannot proceed)
 * - User profile missing: throws exception (required for constraints)
 * 
 * LIMITATIONS (STEP 5.1):
 * - Single day only (weekly plans in future step)
 * - No versioning (implemented in future step)
 * - No persistence (caller handles save)
 * - No parallel generation (sequential for simplicity)
 */
@Service
public class DayPlanAssemblerService {
    private static final Logger logger = LoggerFactory.getLogger(DayPlanAssemblerService.class);
    
    private final AiStrategyService aiStrategyService;
    private final RecipeGenerationService recipeGenerationService;
    private final RecipeRetrievalService recipeRetrievalService;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NutritionalPreferencesRepository nutritionalPreferencesRepository;
    
    // Default meal times (can be customized per user in future)
    private static final LocalTime DEFAULT_BREAKFAST_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_LUNCH_TIME = LocalTime.of(12, 30);
    private static final LocalTime DEFAULT_DINNER_TIME = LocalTime.of(18, 30);
    private static final LocalTime DEFAULT_SNACK_TIME = LocalTime.of(15, 0);
    
    public DayPlanAssemblerService(
            AiStrategyService aiStrategyService,
            RecipeGenerationService recipeGenerationService,
            RecipeRetrievalService recipeRetrievalService,
            UserRepository userRepository,
            ProfileRepository profileRepository,
            NutritionalPreferencesRepository nutritionalPreferencesRepository) {
        this.aiStrategyService = aiStrategyService;
        this.recipeGenerationService = recipeGenerationService;
        this.recipeRetrievalService = recipeRetrievalService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
    }
    
    /**
     * Assemble a complete DayPlan for the given user and date.
     * 
     * Prerequisites:
     * - User must have AI strategy cached (STEP 4.1)
     * - User must have meal structure cached (STEP 4.2)
     * - User must have profile configured
     * 
     * Process:
     * 1. Fetch cached AI strategy and meal structure
     * 2. Fetch user profile and dietary preferences
     * 3. For each meal slot:
     *    a. Query similar recipes (STEP 4.3.1)
     *    b. Generate recipe with RAG + function calling (STEP 4.3.2)
     *    c. Convert to Meal entity
     * 4. Assemble all Meals into DayPlan
     * 
     * @param userId User ID
     * @param date Target date for the plan
     * @param mealPlanVersion Parent MealPlanVersion (for persistence)
     * @return Assembled DayPlan with all meals
     * @throws IllegalStateException if strategy/structure not cached or user profile missing
     */
    @Transactional
    public DayPlan assembleDayPlan(Long userId, LocalDate date, MealPlanVersion mealPlanVersion) {
        logger.info("Starting DayPlan assembly for userId={}, date={}", userId, date);
        
        // Step 1: Fetch prerequisites
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        
        AiStrategyResult strategy = aiStrategyService.getCachedStrategy(String.valueOf(userId));
        if (strategy == null) {
            throw new IllegalStateException(
                "AI strategy not found for user " + userId + ". Run STEP 4.1 first.");
        }
        
        AiMealStructureResult mealStructure = aiStrategyService.getCachedMealStructure(String.valueOf(userId));
        if (mealStructure == null) {
            throw new IllegalStateException(
                "Meal structure not found for user " + userId + ". Run STEP 4.2 first.");
        }
        
        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalStateException(
                "User profile not configured for user " + userId));
        
        // Step 2: Fetch user dietary constraints
        UserDietaryConstraints constraints = fetchUserDietaryConstraints(user);
        
        // Step 3: Create DayPlan shell
        DayPlan dayPlan = new DayPlan(mealPlanVersion, date);
        String timezone = profile.getTimezone() != null ? profile.getTimezone() : "UTC";
        ZoneId zoneId = ZoneId.of(timezone);
        
        // Step 4: Generate meals for each slot
        List<AiMealStructureResult.MealSlot> mealSlots = mealStructure.getMeals();
        logger.info("Generating {} meals for date {}", mealSlots.size(), date);
        
        List<Meal> generatedMeals = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (AiMealStructureResult.MealSlot slot : mealSlots) {
            try {
                Meal meal = generateMealForSlot(
                    user, 
                    strategy, 
                    slot, 
                    constraints, 
                    dayPlan, 
                    date, 
                    zoneId
                );
                generatedMeals.add(meal);
                successCount++;
                logger.info("Successfully generated meal: {} (index {})", 
                    slot.getMealType(), slot.getIndex());
            } catch (Exception e) {
                logger.error("Failed to generate meal for slot: {} (index {}). Error: {}", 
                    slot.getMealType(), slot.getIndex(), e.getMessage(), e);
                
                // Create placeholder meal on failure
                Meal placeholderMeal = createPlaceholderMeal(slot, dayPlan, date, zoneId);
                generatedMeals.add(placeholderMeal);
                failureCount++;
            }
        }
        
        // Step 5: Sort meals by planned time
        generatedMeals.sort(Comparator.comparing(Meal::getPlannedTime));
        
        // Step 6: Add meals to DayPlan
        for (Meal meal : generatedMeals) {
            dayPlan.addMeal(meal);
        }
        
        logger.info("DayPlan assembly complete. Success: {}, Failures: {}", 
            successCount, failureCount);
        
        if (failureCount > 0) {
            logger.warn("Some meals failed to generate. Partial plan created with placeholders.");
        }
        
        return dayPlan;
    }
    
    /**
     * Generate a single meal for a meal slot.
     * 
     * Flow:
     * 1. Build recipe query from slot and user preferences
     * 2. Retrieve similar recipes (RAG - STEP 4.3.1)
     * 3. Build recipe generation request
     * 4. Generate recipe with AI (STEP 4.3.2)
     * 5. Convert to Meal entity
     */
    private Meal generateMealForSlot(
            UserEntity user,
            AiStrategyResult strategy,
            AiMealStructureResult.MealSlot slot,
            UserDietaryConstraints constraints,
            DayPlan dayPlan,
            LocalDate date,
            ZoneId zoneId) {
        
        logger.debug("Generating meal for slot: {} (index {})", slot.getMealType(), slot.getIndex());
        
        // Step 1: Build recipe query for RAG
        RecipeQuery query = buildRecipeQuery(slot, constraints);
        
        // Step 2: Retrieve similar recipes (STEP 4.3.1)
        List<RetrievedRecipe> retrievedRecipes = recipeRetrievalService.retrieve(query, 5);
        logger.debug("Retrieved {} similar recipes for {}", retrievedRecipes.size(), slot.getMealType());
        
        // Step 3: Build recipe generation request
        RecipeGenerationRequest request = new RecipeGenerationRequest();
        request.setUserId(String.valueOf(user.getId()));
        request.setStrategy(strategy);
        request.setMealSlot(slot);
        request.setRetrievedRecipes(retrievedRecipes);
        request.setDietaryRestrictions(constraints.dietaryRestrictions);
        request.setAllergies(constraints.allergies);
        request.setDietaryPreferences(constraints.dietaryPreferences);
        request.setTargetCalories(slot.getCalorieTarget());
        request.setServings(1); // Default to 1 serving
        
        // Step 4: Generate recipe (STEP 4.3.2)
        GeneratedRecipe generatedRecipe = recipeGenerationService.generate(request);
        logger.debug("Generated recipe: {}", generatedRecipe.getTitle());
        
        // Step 5: Convert to Meal entity
        Meal meal = convertToMeal(generatedRecipe, slot, dayPlan, date, zoneId);
        
        return meal;
    }
    
    /**
     * Build a RecipeQuery for retrieving similar recipes.
     */
    private RecipeQuery buildRecipeQuery(
            AiMealStructureResult.MealSlot slot,
            UserDietaryConstraints constraints) {
        
        RecipeQuery query = new RecipeQuery();
        query.setMealType(slot.getMealType());
        query.setDietaryRestrictions(constraints.dietaryRestrictions);
        query.setMacroFocus(slot.getMacroFocus());
        query.setCuisinePreferences(constraints.cuisinePreferences);
        
        // Build free text query from timing note
        if (slot.getTimingNote() != null && !slot.getTimingNote().isBlank()) {
            query.setFreeTextQuery(slot.getTimingNote());
        }
        
        return query;
    }
    
    /**
     * Convert a GeneratedRecipe to a Meal entity.
     */
    private Meal convertToMeal(
            GeneratedRecipe generatedRecipe,
            AiMealStructureResult.MealSlot slot,
            DayPlan dayPlan,
            LocalDate date,
            ZoneId zoneId) {
        
        // Determine meal time based on type
        LocalTime mealTime = getMealTime(slot.getMealType(), slot.getIndex());
        LocalDateTime plannedTime = LocalDateTime.of(date, mealTime);
        
        // Convert meal type string to enum
        MealType mealType = convertToMealType(slot.getMealType());
        
        // Create Meal entity
        Meal meal = new Meal(dayPlan, mealType, slot.getIndex(), plannedTime);
        
        // For now, store recipe as custom meal name
        // In future, this will reference a persisted Recipe entity
        meal.setCustomMealName(generatedRecipe.getTitle());
        
        return meal;
    }
    
    /**
     * Create a placeholder meal when generation fails.
     * This allows partial plans to be saved and individual meals regenerated later.
     */
    private Meal createPlaceholderMeal(
            AiMealStructureResult.MealSlot slot,
            DayPlan dayPlan,
            LocalDate date,
            ZoneId zoneId) {
        
        LocalTime mealTime = getMealTime(slot.getMealType(), slot.getIndex());
        LocalDateTime plannedTime = LocalDateTime.of(date, mealTime);
        MealType mealType = convertToMealType(slot.getMealType());
        
        Meal meal = new Meal(dayPlan, mealType, slot.getIndex(), plannedTime);
        meal.setCustomMealName("[Placeholder - Generation Failed]");
        
        return meal;
    }
    
    /**
     * Fetch user dietary constraints from multiple sources.
     */
    private UserDietaryConstraints fetchUserDietaryConstraints(UserEntity user) {
        UserDietaryConstraints constraints = new UserDietaryConstraints();
        
        // Try NutritionalPreferences first (preferred source)
        NutritionalPreferences nutPrefs = nutritionalPreferencesRepository
            .findById(user.getId())
            .orElse(null);
        
        if (nutPrefs != null) {
            constraints.dietaryRestrictions = new ArrayList<>(nutPrefs.getDietaryPreferences());
            constraints.allergies = new ArrayList<>(nutPrefs.getAllergies());
            constraints.cuisinePreferences = new ArrayList<>(nutPrefs.getCuisinePreferences());
            constraints.dietaryPreferences = new HashMap<>();
            // Convert Set to Map for compatibility with RecipeGenerationRequest
            for (String pref : nutPrefs.getDietaryPreferences()) {
                constraints.dietaryPreferences.put(pref, true);
            }
        } else {
            // Fallback to UserEntity JSON fields
            constraints.dietaryRestrictions = parseCsvField(user.getDietaryRestrictionsJson());
            constraints.dietaryPreferences = new HashMap<>();
            List<String> prefs = parseCsvField(user.getDietaryPreferencesJson());
            for (String pref : prefs) {
                constraints.dietaryPreferences.put(pref, true);
            }
            constraints.allergies = new ArrayList<>();
            constraints.cuisinePreferences = new ArrayList<>();
        }
        
        return constraints;
    }
    
    /**
     * Parse comma-separated field from database.
     */
    private List<String> parseCsvField(String csvField) {
        if (csvField == null || csvField.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.asList(csvField.split(","));
    }
    
    /**
     * Get meal time based on meal type and index.
     * For multiple snacks, offset by 2 hours each.
     */
    private LocalTime getMealTime(String mealType, int index) {
        switch (mealType.toLowerCase()) {
            case "breakfast":
                return DEFAULT_BREAKFAST_TIME;
            case "lunch":
                return DEFAULT_LUNCH_TIME;
            case "dinner":
                return DEFAULT_DINNER_TIME;
            case "snack":
                // Offset snacks by 2 hours each
                return DEFAULT_SNACK_TIME.plusHours(index * 2L);
            default:
                return LocalTime.of(12, 0); // Default to noon
        }
    }
    
    /**
     * Convert string meal type to MealType enum.
     */
    private MealType convertToMealType(String mealTypeStr) {
        try {
            return MealType.valueOf(mealTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown meal type: {}. Defaulting to SNACK", mealTypeStr);
            return MealType.SNACK;
        }
    }
    
    /**
     * Internal class to hold user dietary constraints.
     */
    private static class UserDietaryConstraints {
        List<String> dietaryRestrictions = new ArrayList<>();
        List<String> allergies = new ArrayList<>();
        Map<String, Boolean> dietaryPreferences = new HashMap<>();
        List<String> cuisinePreferences = new ArrayList<>();
    }
}
