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
import com.ndl.numbers_dont_lie.mealplan.util.DayPlanContextHash;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

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
    private final RecipeRepository recipeRepository;
    
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
            NutritionalPreferencesRepository nutritionalPreferencesRepository,
            RecipeRepository recipeRepository) {
        this.aiStrategyService = aiStrategyService;
        this.recipeGenerationService = recipeGenerationService;
        this.recipeRetrievalService = recipeRetrievalService;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
        this.recipeRepository = recipeRepository;
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
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public DayPlan assembleDayPlan(Long userId, LocalDate date, MealPlanVersion mealPlanVersion) {
        return assembleDayPlan(userId, date, mealPlanVersion, null, null);
    }

    /**
     * Assemble a day plan with context change detection.
     * 
     * If existingDayPlan is provided and context has changed, clear existing meals and regenerate.
     * Logs: "[DAY_PLAN] Context changed → regenerating meals" and "[DAY_PLAN] Old hash=..., New hash=..."
     * 
     * @param userId The user ID
     * @param date The date for the meal plan
     * @param mealPlanVersion The meal plan version
     * @param existingDayPlan Optional existing day plan to check for context changes
     * @return A new or updated DayPlan
     */
    public DayPlan assembleDayPlan(Long userId, LocalDate date, MealPlanVersion mealPlanVersion, DayPlan existingDayPlan) {
        return assembleDayPlan(userId, date, mealPlanVersion, existingDayPlan, null);
    }

    /**
     * Assemble a day plan with optional recipe exclusion for weekly uniqueness.
     * 
     * @param userId The user ID
     * @param date The date for the meal plan
     * @param mealPlanVersion The meal plan version
     * @param existingDayPlan Optional existing day plan to check for context changes
     * @param excludeRecipeIds Optional set of recipe stable IDs (String) to avoid (for weekly uniqueness)
     * @return A new or updated DayPlan
     */
    public DayPlan assembleDayPlan(Long userId, LocalDate date, MealPlanVersion mealPlanVersion, DayPlan existingDayPlan, Set<String> excludeRecipeIds) {
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
        
        // Step 2.5: Compute context hash to detect preference changes
        String currentContextHash = DayPlanContextHash.generate(
            userId,
            constraints.dietaryRestrictions,
            constraints.allergies,
            constraints.dislikedIngredients,
            constraints.cuisinePreferences,
            null,
            mealStructure.getMeals()
        );
        logger.info("[DAY_PLAN] Computed context hash: {}", currentContextHash);
        
        // Step 2.6: Detect context changes
        boolean contextChanged = false;
        if (existingDayPlan != null && existingDayPlan.getContextHash() != null) {
            String oldContextHash = existingDayPlan.getContextHash();
            if (!oldContextHash.equals(currentContextHash)) {
                contextChanged = true;
                logger.info("[DAY_PLAN] Context changed → regenerating meals");
                logger.info("[DAY_PLAN] Old hash={}, New hash={}", oldContextHash, currentContextHash);
            }
        }
        
        // Step 3: Create DayPlan shell
        DayPlan dayPlan = existingDayPlan != null ? existingDayPlan : new DayPlan(mealPlanVersion, date);
        dayPlan.setUserId(userId);
        
        // Clear meals if context changed
        if (contextChanged) {
            dayPlan.getMeals().clear();
        }
        
        dayPlan.setContextHash(currentContextHash);
        String timezone = profile.getTimezone() != null ? profile.getTimezone() : "UTC";
        ZoneId zoneId = ZoneId.of(timezone);
        
        // Step 4: Generate meals for each slot (expand based on profile counts)
        List<AiMealStructureResult.MealSlot> mealSlots = expandMealSlots(
            mealStructure.getMeals(), 
            constraints.breakfastCount,
            constraints.lunchCount,
            constraints.dinnerCount,
            constraints.snackCount
        );
        logger.info("[MEAL_STRUCTURE] Expanded to {} total meal slots for date {}", mealSlots.size(), date);
        
        // Log each slot with its calorie target
        logger.info("[MEAL_STRUCTURE_DEBUG] === Expanded slots breakdown ===");
        for (AiMealStructureResult.MealSlot slot : mealSlots) {
            logger.info("[MEAL_STRUCTURE_DEBUG] Slot: {}[{}] calorieTarget={}", 
                slot.getMealType(), slot.getIndex(), slot.getCalorieTarget());
        }
        logger.info("[MEAL_STRUCTURE_DEBUG] === End slots ===");
        
        List<Meal> generatedMeals = new ArrayList<>();
        Set<String> usedRecipeTitles = new HashSet<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (AiMealStructureResult.MealSlot slot : mealSlots) {
            try {
                // Respect snack suppression: if snackCount is 0, skip snack slots entirely
                if ("snack".equalsIgnoreCase(slot.getMealType()) && constraints.snackCount != null && constraints.snackCount == 0) {
                    logger.info("Skipping snack generation because snackCount=0");
                    continue;
                }

                Meal meal = generateMealForSlot(
                    user, 
                    strategy, 
                    slot, 
                    constraints, 
                    dayPlan, 
                    date, 
                    zoneId,
                    usedRecipeTitles,
                    excludeRecipeIds
                );

                if (meal != null) {
                    generatedMeals.add(meal);
                    successCount++;
                    logger.info("Successfully generated meal: {} (index {})", 
                        slot.getMealType(), slot.getIndex());
                }
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
    
    private List<AiMealStructureResult.MealSlot> expandMealSlots(
            List<AiMealStructureResult.MealSlot> baseSlots,
            int breakfastCount,
            int lunchCount,
            int dinnerCount,
            int snackCount) {
        
        logger.info("[MEAL_STRUCTURE] === BEFORE EXPANSION ===");
        logger.info("[MEAL_STRUCTURE] Base slots from AI (count = {}):", baseSlots.size());
        for (AiMealStructureResult.MealSlot slot : baseSlots) {
            logger.info("[MEAL_STRUCTURE]   - {}: calorieTarget={}", slot.getMealType(), slot.getCalorieTarget());
        }
        logger.info("[MEAL_STRUCTURE] Profile counts: breakfast={}, lunch={}, dinner={}, snack={}",
            breakfastCount, lunchCount, dinnerCount, snackCount);
        
        List<AiMealStructureResult.MealSlot> expanded = new ArrayList<>();
        
        for (AiMealStructureResult.MealSlot baseSlot : baseSlots) {
            String mealType = baseSlot.getMealType().toLowerCase();
            int count = 1;
            
            switch (mealType) {
                case "breakfast" -> count = breakfastCount;
                case "lunch" -> count = lunchCount;
                case "dinner" -> count = dinnerCount;
                case "snack" -> count = snackCount;
            }
            
            logger.info("[MEAL_STRUCTURE] Generating {} slots for {}", count, mealType);
            
            if (count == 0) {
                continue;
            }
            
            for (int i = 0; i < count; i++) {
                AiMealStructureResult.MealSlot slot = new AiMealStructureResult.MealSlot();
                slot.setMealType(baseSlot.getMealType());
                slot.setIndex(i);
                
                int caloriePerSlot = baseSlot.getCalorieTarget() / count;
                slot.setCalorieTarget(caloriePerSlot);
                logger.info("[MEAL_STRUCTURE] Expanded slot: {}[{}] = {} cal", mealType, i, caloriePerSlot);
                slot.setMacroFocus(baseSlot.getMacroFocus());
                
                String timingNote = baseSlot.getTimingNote();
                if (count > 1) {
                    timingNote = (timingNote != null ? timingNote : mealType) + " " + (i + 1);
                }
                slot.setTimingNote(timingNote);
                
                expanded.add(slot);
            }
        }
        
        logger.info("[MEAL_STRUCTURE] Total slots after expansion: {}", expanded.size());
        return expanded;
    }

    /**
     * Generate a single meal for a meal slot.
     * 
     * Flow:
     * 1. Build recipe query from slot and user preferences
     * 2. Retrieve similar recipes (RAG - STEP 4.3.1)
     * 3. Filter retrieved recipes based on user preferences (allergies, dislikes, dietary)
     * 4. Build recipe generation request
     * 5. Generate recipe with AI (STEP 4.3.2)
     * 6. Convert to Meal entity
     */
        private Meal generateMealForSlot(
            UserEntity user,
            AiStrategyResult strategy,
            AiMealStructureResult.MealSlot slot,
            UserDietaryConstraints constraints,
            DayPlan dayPlan,
            LocalDate date,
            ZoneId zoneId,
            Set<String> usedRecipeTitles,
            Set<String> excludeRecipeIds) {
        
        logger.debug("Generating meal for slot: {} (index {})", slot.getMealType(), slot.getIndex());
        logger.info("[PREFERENCES] Loaded for userId={}: allergies={}, disliked={}, dietary={}, cuisines={}", 
            user.getId(), constraints.allergies, constraints.dislikedIngredients, 
            constraints.dietaryRestrictions, constraints.cuisinePreferences);
        
        // Step 1: Try to select from database first
        logger.info("[RECIPE_SELECTION] Attempting to select DB recipe for {}", slot.getMealType());
        Recipe dbRecipe = selectDatabaseRecipeForSlot(slot, constraints, usedRecipeTitles, Double.valueOf(slot.getCalorieTarget()), excludeRecipeIds);
        if (dbRecipe != null) {
            logger.info("[RECIPE_SELECTION] SUCCESS - Selected DB recipe: {} (stableId={})", dbRecipe.getTitle(), dbRecipe.getStableId());
            if (excludeRecipeIds != null && dbRecipe.getStableId() != null) {
                excludeRecipeIds.add(dbRecipe.getStableId());
            }
            GeneratedRecipe dbGenerated = convertRecipeToGeneratedRecipe(dbRecipe);
            if (dbGenerated.getTitle() != null) {
                usedRecipeTitles.add(dbGenerated.getTitle().toLowerCase());
            }
            return convertToMeal(dbGenerated, slot, dayPlan, date, zoneId, dbRecipe.getStableId());
        }

        logger.warn("[RECIPE_FALLBACK] No suitable DB recipe found. Falling back to AI for {}", slot.getMealType());

        // Step 2: Build recipe query for RAG (AI fallback)
        RecipeQuery query = buildRecipeQuery(slot, constraints);
        List<RetrievedRecipe> retrievedRecipes = recipeRetrievalService.retrieve(query, 5);
        logger.debug("Retrieved {} similar recipes for {}", retrievedRecipes.size(), slot.getMealType());

        // Step 3: Filter retrieved recipes based on user preferences (soft)
        List<RetrievedRecipe> filteredRecipes = filterRecipesByPreferences(
            retrievedRecipes, constraints, slot.getMealType());
        logger.info("[PREFERENCES] Filtered {} → {} recipes (allergies/dislikes/dietary)", 
            retrievedRecipes.size(), filteredRecipes.size());

        // Step 4: Build recipe generation request
        RecipeGenerationRequest request = new RecipeGenerationRequest();
        request.setUserId(String.valueOf(user.getId()));
        request.setStrategy(strategy);
        request.setMealSlot(slot);
        request.setRetrievedRecipes(filteredRecipes.isEmpty() ? retrievedRecipes : filteredRecipes);
        request.setDietaryRestrictions(constraints.dietaryRestrictions);
        request.setAllergies(constraints.allergies);
        request.setDietaryPreferences(constraints.dietaryPreferences);
        request.setTargetCalories(slot.getCalorieTarget());
        request.setServings(1); // Default to 1 serving

        // Step 5: Generate recipe (STEP 4.3.2)
        GeneratedRecipe generatedRecipe = recipeGenerationService.generate(request);
        logger.debug("Generated recipe: {}", generatedRecipe.getTitle());

        // Step 5.5: EXPLICIT RECIPE FILTERING - Apply strict constraints to generated recipe
        GeneratedRecipe filteredRecipe = applyExplicitRecipeFiltering(
            generatedRecipe, 
            constraints, 
            slot.getMealType(),
            user.getId(),
            Double.valueOf(slot.getCalorieTarget()),
            usedRecipeTitles);

        if (filteredRecipe.getTitle() != null) {
            usedRecipeTitles.add(filteredRecipe.getTitle().toLowerCase());
        }

        // Step 6: Convert to Meal entity
        return convertToMeal(filteredRecipe, slot, dayPlan, date, zoneId, null);
    }
    
    /**
     * Apply explicit dietary constraint filtering to a generated recipe.
     * 
     * FILTER RULES (MVP):
     * 1. dietaryPreferences (HARD FILTER)
     *    - If user has "vegetarian": exclude any recipe whose name/description/ingredients imply meat
     *      (beef, pork, chicken, turkey, fish, salmon, tuna, bacon, sausage, etc.)
     * 
     * 2. allergies (HARD FILTER)
     *    - Exclude any recipe whose name/description/ingredients mention allergic items
     *      (e.g. peanuts, shellfish, dairy, gluten, etc.)
     * 
     * 3. dislikedIngredients (HARD FILTER)
     *    - Exclude any recipe whose name/description/ingredients mention disliked items
     *      (e.g. cilantro, olives, mushrooms)
     * 
     * 4. cuisinePreferences (SOFT FILTER)
     *    - If cuisinePreferences present: prefer matching recipes
     *    - If no recipes match: DO NOT exclude, continue with recipe
     * 
     * @param recipe Generated recipe to validate
     * @param constraints User dietary constraints
     * @param mealType Meal type for logging
     * @param userId User ID for tracing
     * @return Validated recipe if it passes all filters, or fallback if rejected
     */
    private GeneratedRecipe applyExplicitRecipeFiltering(
            GeneratedRecipe recipe,
            UserDietaryConstraints constraints,
            String mealType,
            Long userId,
            Double targetCalories,
            Set<String> usedRecipeTitles) {
        
        logger.info("[RECIPE_FILTER] Starting validation for userId={} recipe='{}'", userId, recipe.getTitle());
        
        // Build comprehensive recipe text for filtering (title + summary + ingredients)
        String recipeText = buildRecipeTextForFiltering(recipe);
        String recipeLower = recipeText.toLowerCase();
        
        // RULE 1: DIETARY RESTRICTIONS (HARD FILTER - vegetarian)
        for (String dietary : constraints.dietaryRestrictions) {
            if ("vegetarian".equalsIgnoreCase(dietary)) {
                if (containsAnyMeatIndicators(recipeLower)) {
                    logger.warn("[RECIPE_FILTER] Excluded recipe='{}' reason=vegetarian (contains meat)", 
                        recipe.getTitle());
                    return createFallbackRecipe(mealType, "Vegetarian constraint", constraints, targetCalories, usedRecipeTitles);
                }
            }
        }
        
        // RULE 2: ALLERGIES (HARD FILTER)
        for (String allergen : constraints.allergies) {
            if (recipeContainsIngredient(recipe, allergen)) {
                logger.warn("[RECIPE_FILTER] Excluded recipe='{}' reason=allergen({})", 
                    recipe.getTitle(), allergen);
                return createFallbackRecipe(mealType, "Allergen: " + allergen, constraints, targetCalories, usedRecipeTitles);
            }
        }
        
        // RULE 3: DISLIKED INGREDIENTS (HARD FILTER)
        for (String disliked : constraints.dislikedIngredients) {
            if (recipeContainsIngredient(recipe, disliked)) {
                logger.warn("[RECIPE_FILTER] Excluded recipe='{}' reason=disliked({})", 
                    recipe.getTitle(), disliked);
                return createFallbackRecipe(mealType, "Disliked: " + disliked, constraints, targetCalories, usedRecipeTitles);
            }
        }
        
        // RULE 4: CUISINE PREFERENCES (SOFT FILTER)
        if (!constraints.cuisinePreferences.isEmpty()) {
            if (recipe.getCuisine() != null && 
                constraints.cuisinePreferences.contains(recipe.getCuisine())) {
                logger.info("[RECIPE_FILTER] ✓ Recipe matches preferred cuisine: {}", recipe.getCuisine());
            } else {
                logger.debug("[RECIPE_FILTER] Recipe cuisine {} not in preferences, but accepting anyway (soft filter)", 
                    recipe.getCuisine());
            }
        }
        
        // All filters passed
        logger.info("[RECIPE_FILTER] ✓ Recipe passed all dietary constraints: '{}'", recipe.getTitle());
        return recipe;
    }
    
    /**
     * Build comprehensive recipe text from title, summary, and ingredients for filtering.
     */
    private String buildRecipeTextForFiltering(GeneratedRecipe recipe) {
        StringBuilder text = new StringBuilder();
        
        if (recipe.getTitle() != null) {
            text.append(recipe.getTitle()).append(" ");
        }
        if (recipe.getSummary() != null) {
            text.append(recipe.getSummary()).append(" ");
        }
        
        if (recipe.getIngredients() != null) {
            for (GeneratedRecipe.GeneratedIngredient ing : recipe.getIngredients()) {
                if (ing.getName() != null) {
                    text.append(ing.getName()).append(" ");
                }
            }
        }
        
        return text.toString();
    }
    
    /**
     * Check if recipe contains any indicators of meat (chicken, beef, pork, fish, etc).
     */
    private boolean containsAnyMeatIndicators(String recipeLower) {
        String[] meatIndicators = {
            "beef", "pork", "chicken", "turkey", "lamb", "veal",
            "fish", "salmon", "tuna", "trout", "cod", "shrimp", "prawn", "lobster", "crab",
            "bacon", "ham", "sausage", "steak", "ribs", "tenderloin", "breast", "thigh",
            "duck", "goose", "venison", "game", "meat"
        };
        
        for (String meat : meatIndicators) {
            if (recipeLower.contains(meat)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Check if recipe contains a specific ingredient (by name matching).
     */
    private boolean recipeContainsIngredient(GeneratedRecipe recipe, String ingredient) {
        String ingredientLower = ingredient.toLowerCase();
        
        // Check in title and summary
        String recipeText = buildRecipeTextForFiltering(recipe).toLowerCase();
        if (recipeText.contains(ingredientLower)) {
            return true;
        }
        
        // Also check ingredient names explicitly
        if (recipe.getIngredients() != null) {
            for (GeneratedRecipe.GeneratedIngredient ing : recipe.getIngredients()) {
                if (ing.getName() != null && ing.getName().toLowerCase().contains(ingredientLower)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Create a safe fallback recipe when filtering rejects the generated recipe.
     * First attempts to find a safe recipe from database, then falls back to placeholder.
     */
    private GeneratedRecipe createFallbackRecipe(
            String mealType,
            String reason,
            UserDietaryConstraints constraints,
            Double targetCalories,
            Set<String> usedRecipeTitles) {
        logger.warn("[RECIPE_FALLBACK] AI recipe rejected: {}", reason);
        
        // Try to find a safe database recipe first
        Recipe safeDbRecipe = findSafeDatabaseRecipe(mealType, constraints, targetCalories, usedRecipeTitles);
        if (safeDbRecipe != null) {
            logger.info("[RECIPE_FALLBACK] Using DB recipe: {}", safeDbRecipe.getTitle());
            return convertRecipeToGeneratedRecipe(safeDbRecipe);
        }
        
        // No safe DB recipe found, use placeholder fallback
        logger.warn("[RECIPE_FALLBACK] No safe DB recipes found, using fallback placeholder");
        
        GeneratedRecipe fallback = new GeneratedRecipe();
        fallback.setTitle("[Fallback - " + reason + "]");
        fallback.setMeal(mealType);
        fallback.setCuisine("International");
        fallback.setSummary("Safe fallback meal due to dietary constraints");
        fallback.setServings(1);
        fallback.setDietaryTags(Arrays.asList("vegetarian", "hypoallergenic"));
        
        // Simple safe ingredients
        List<GeneratedRecipe.GeneratedIngredient> ingredients = new ArrayList<>();
        GeneratedRecipe.GeneratedIngredient ing1 = new GeneratedRecipe.GeneratedIngredient();
        ing1.setName("Rice");
        ing1.setQuantity(100.0);
        ing1.setUnit("g");
        ingredients.add(ing1);
        
        GeneratedRecipe.GeneratedIngredient ing2 = new GeneratedRecipe.GeneratedIngredient();
        ing2.setName("Steamed Vegetables");
        ing2.setQuantity(150.0);
        ing2.setUnit("g");
        ingredients.add(ing2);
        
        fallback.setIngredients(ingredients);
        return fallback;
    }
    
    /**
     * Find a safe recipe from database that matches meal type and passes dietary filters.
     * 
     * @param mealType Meal type (breakfast, lunch, dinner, snack)
     * @param constraints Optional dietary constraints (if null, will use basic safe criteria)
     * @return Safe recipe from database, or null if none found
     */
    private Recipe findSafeDatabaseRecipe(
            String mealType,
            UserDietaryConstraints constraints,
            Double targetCalories,
            Set<String> usedRecipeTitles) {
        logger.info("[RECIPE_FALLBACK] Searching DB for safe {} recipe", mealType);
        
        // Convert string meal type to Recipe's MealType enum
        com.ndl.numbers_dont_lie.recipe.entity.MealType recipeMealType;
        try {
            recipeMealType = com.ndl.numbers_dont_lie.recipe.entity.MealType.valueOf(mealType.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("[RECIPE_FALLBACK] Invalid meal type: {}", mealType);
            return null;
        }
        
        // Query recipes matching meal type - pass Recipe's MealType
        List<Recipe> candidates = recipeRepository.findByMeal(recipeMealType);
        
        if (candidates.isEmpty()) {
            logger.debug("[RECIPE_FALLBACK] No {} recipes in database", mealType);
            return null;
        }
        
        logger.debug("[RECIPE_FALLBACK] Found {} candidate recipes for {}", candidates.size(), mealType);
        
        List<Recipe> eligible = new ArrayList<>();
        if (constraints == null) {
            for (Recipe recipe : candidates) {
                if (isRecipeVegetarian(recipe)) {
                    eligible.add(recipe);
                }
            }
            if (eligible.isEmpty()) {
                eligible.addAll(candidates);
            }
        } else {
            for (Recipe recipe : candidates) {
                if (isRecipeSafeForConstraints(recipe, constraints)) {
                    eligible.add(recipe);
                }
            }
        }
        
        if (eligible.isEmpty()) {
            logger.debug("[RECIPE_FALLBACK] No safe recipes found after filtering");
            return null;
        }
        
        return selectBestRecipeCandidate(eligible, constraints, targetCalories, usedRecipeTitles);
    }

    /**
     * Select the best database recipe for a given meal slot (DB-first strategy).
     */
    private Recipe selectDatabaseRecipeForSlot(
            AiMealStructureResult.MealSlot slot,
            UserDietaryConstraints constraints,
            Set<String> usedRecipeTitles,
            Double targetCalories,
            Set<String> excludeRecipeIds) {

        com.ndl.numbers_dont_lie.recipe.entity.MealType recipeMealType;
        try {
            recipeMealType = com.ndl.numbers_dont_lie.recipe.entity.MealType.valueOf(slot.getMealType().toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("[RECIPE_DECISION] Invalid meal type for DB selection: {}", slot.getMealType());
            return null;
        }

        List<Recipe> candidates = recipeRepository.findByMeal(recipeMealType);
        logger.info("[RECIPE_DECISION] Found {} candidate recipes for mealType={}", candidates.size(), slot.getMealType());
        if (candidates.isEmpty()) {
            logger.info("[RECIPE_DECISION] No DB candidates for mealType={}", slot.getMealType());
            return null;
        }

        List<Recipe> eligible = new ArrayList<>();
        for (Recipe recipe : candidates) {
            boolean duplicate = recipe.getTitle() != null && usedRecipeTitles.stream()
                    .anyMatch(t -> t.equalsIgnoreCase(recipe.getTitle()));
            if (duplicate) {
                logger.debug("[RECIPE_DECISION] Skip duplicate title: {}", recipe.getTitle());
                continue;
            }
            // WEEKLY UNIQUENESS: Skip recipes already used this week
            if (excludeRecipeIds != null && recipe.getStableId() != null && excludeRecipeIds.contains(recipe.getStableId())) {
                logger.debug("[RECIPE_DECISION] Skip already used in week: {} (stableId={})", recipe.getTitle(), recipe.getStableId());
                continue;
            }
            if (isRecipeSafeForConstraints(recipe, constraints)) {
                eligible.add(recipe);
                logger.debug("[RECIPE_DECISION] Recipe eligible: {}", recipe.getTitle());
            } else {
                logger.debug("[RECIPE_DECISION] Recipe rejected by constraints: {}", recipe.getTitle());
            }
        }

        if (eligible.isEmpty()) {
            logger.info("[RECIPE_DECISION] No eligible DB recipes after filtering for {}", slot.getMealType());
            return null;
        }

        logger.info("[RECIPE_DECISION] {} eligible recipes, selecting best candidate", eligible.size());
        return selectBestRecipeCandidate(eligible, constraints, targetCalories, usedRecipeTitles);
    }

    private Recipe selectBestRecipeCandidate(
            List<Recipe> candidates,
            UserDietaryConstraints constraints,
            Double targetCalories,
            Set<String> usedRecipeTitles) {
        logger.info("[RECIPE_DECISION] selectBestRecipeCandidate called with {} candidates", candidates.size());
        List<ScoredCandidate> scored = new ArrayList<>();
        for (Recipe candidate : candidates) {
            scored.add(scoreCandidate(candidate, constraints, targetCalories, usedRecipeTitles));
        }
        scored.sort(Comparator.comparingDouble(ScoredCandidate::score).reversed());
        ScoredCandidate best = scored.get(0);
        logger.info("[RECIPE_DECISION] Best recipe selected: {} (stableId={}, score={})", 
            best.recipe.getTitle(), best.recipe.getStableId(), formatScore(best.score));
        for (ScoredCandidate entry : scored) {
            if (entry == best) {
                logger.info("[RECIPE_DECISION] Selected {} score={} reasons={}",
                    entry.recipe.getTitle(), formatScore(entry.score), entry.reasons);
            } else {
                logger.info("[RECIPE_DECISION] Rejected {} score={} reasons={}",
                    entry.recipe.getTitle(), formatScore(entry.score), entry.reasons);
            }
        }
        return best.recipe;
    }

        private ScoredCandidate scoreCandidate(
            Recipe recipe,
            UserDietaryConstraints constraints,
            Double targetCalories,
            Set<String> usedRecipeTitles) {
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        // Cuisine preference match
        if (constraints != null && !constraints.cuisinePreferences.isEmpty() && recipe.getCuisine() != null) {
            boolean matches = constraints.cuisinePreferences.stream()
                    .anyMatch(pref -> pref.equalsIgnoreCase(recipe.getCuisine()));
            if (matches) {
                score += 30.0;
                reasons.add("cuisine match");
            } else {
                reasons.add("cuisine not preferred");
            }
        } else if (recipe.getCuisine() != null) {
            reasons.add("no cuisine preference");
        } else {
            reasons.add("cuisine unknown");
        }

        // Macro / calorie alignment (neutral for now, as DB recipes lack calorie data)
        if (targetCalories != null) {
            reasons.add("calorie target " + targetCalories.intValue() + " (data unavailable, neutral)");
        }

        // Preparation time - shorter is better
        if (recipe.getTimeMinutes() != null) {
            double timeScore = Math.max(0.0, 25.0 - recipe.getTimeMinutes());
            score += timeScore;
            reasons.add("prep " + recipe.getTimeMinutes() + "min");
        } else {
            reasons.add("prep time unknown");
        }

        // Variety - avoid repeating the same title in a day plan
        boolean duplicate = usedRecipeTitles != null && recipe.getTitle() != null &&
                usedRecipeTitles.stream().anyMatch(t -> t.equalsIgnoreCase(recipe.getTitle()));
        if (duplicate) {
            score -= 40.0;
            reasons.add("repeat penalty");
        } else {
            reasons.add("unique title");
        }

        return new ScoredCandidate(recipe, score, reasons);
    }

    private String formatScore(double score) {
        return String.format("%.2f", score);
    }

    private static class ScoredCandidate {
        private final Recipe recipe;
        private final double score;
        private final List<String> reasons;

        ScoredCandidate(Recipe recipe, double score, List<String> reasons) {
            this.recipe = recipe;
            this.score = score;
            this.reasons = reasons;
        }

        public double score() {
            return score;
        }
    }
    
    /**
     * Check if a recipe is vegetarian based on dietary tags.
     */
    private boolean isRecipeVegetarian(Recipe recipe) {
        if (recipe.getDietaryTags() == null) {
            return false;
        }
        return recipe.getDietaryTags().stream()
                .anyMatch(tag -> tag.equalsIgnoreCase("vegetarian") || 
                                tag.equalsIgnoreCase("vegan"));
    }
    
    /**
     * Check if a recipe is safe for user's dietary constraints.
     * Uses same filtering logic as applyExplicitRecipeFiltering but for DB recipes.
     */
    private boolean isRecipeSafeForConstraints(Recipe recipe, UserDietaryConstraints constraints) {
        String recipeTitle = recipe.getTitle() != null ? recipe.getTitle().toLowerCase() : "";
        String recipeSummary = recipe.getSummary() != null ? recipe.getSummary().toLowerCase() : "";
        String recipeText = recipeTitle + " " + recipeSummary;
        
        // Check vegetarian constraint
        for (String dietary : constraints.dietaryRestrictions) {
            if ("vegetarian".equalsIgnoreCase(dietary)) {
                if (containsAnyMeatIndicators(recipeText)) {
                    logger.debug("[RECIPE_FALLBACK] Recipe '{}' contains meat", recipe.getTitle());
                    return false;
                }
                // Also check dietary tags
                if (recipe.getDietaryTags() != null && 
                    !recipe.getDietaryTags().stream().anyMatch(tag -> 
                        tag.equalsIgnoreCase("vegetarian") || tag.equalsIgnoreCase("vegan"))) {
                    logger.debug("[RECIPE_FALLBACK] Recipe '{}' not tagged vegetarian", recipe.getTitle());
                    return false;
                }
            }
        }
        
        // Check allergies (basic text matching - not perfect but safe)
        for (String allergen : constraints.allergies) {
            if (recipeText.contains(allergen.toLowerCase())) {
                logger.debug("[RECIPE_FALLBACK] Recipe '{}' may contain allergen: {}", recipe.getTitle(), allergen);
                return false;
            }
        }
        
        // Check disliked ingredients
        for (String disliked : constraints.dislikedIngredients) {
            if (recipeText.contains(disliked.toLowerCase())) {
                logger.debug("[RECIPE_FALLBACK] Recipe '{}' contains disliked ingredient: {}", recipe.getTitle(), disliked);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Convert database Recipe entity to GeneratedRecipe DTO for meal assembly.
     */
    private GeneratedRecipe convertRecipeToGeneratedRecipe(Recipe recipe) {
        GeneratedRecipe generated = new GeneratedRecipe();
        generated.setTitle(recipe.getTitle());
        generated.setMeal(recipe.getMeal() != null ? recipe.getMeal().name().toLowerCase() : "lunch");
        generated.setCuisine(recipe.getCuisine());
        generated.setSummary(recipe.getSummary());
        generated.setServings(recipe.getServings());
        
        if (recipe.getDietaryTags() != null) {
            generated.setDietaryTags(new ArrayList<>(recipe.getDietaryTags()));
        }
        
        // Convert ingredients if available
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            List<GeneratedRecipe.GeneratedIngredient> genIngredients = new ArrayList<>();
            recipe.getIngredients().forEach(recipeIng -> {
                if (recipeIng.getIngredient() != null) {
                    GeneratedRecipe.GeneratedIngredient genIng = new GeneratedRecipe.GeneratedIngredient();
                    genIng.setName(recipeIng.getIngredient().getLabel());
                    genIng.setQuantity(recipeIng.getQuantity());
                    genIng.setUnit(recipeIng.getIngredient().getUnit());
                    genIngredients.add(genIng);
                }
            });
            generated.setIngredients(genIngredients);
        } else {
            // Minimal fallback ingredients
            List<GeneratedRecipe.GeneratedIngredient> ingredients = new ArrayList<>();
            GeneratedRecipe.GeneratedIngredient ing = new GeneratedRecipe.GeneratedIngredient();
            ing.setName(recipe.getTitle());
            ing.setQuantity(1.0);
            ing.setUnit("serving");
            ingredients.add(ing);
            generated.setIngredients(ingredients);
        }
        
        return generated;
    }

    /**
     * Filter retrieved recipes based on user dietary preferences.
     * 
     * CURRENT LIMITATIONS (STEP 5.1):
     * - RetrievedRecipe only contains title, cuisine, relevance
     * - Full ingredient data not available at retrieval stage
     * - AI-generated recipes will apply filters via RAG constraints
     * 
     * RULES (Future Enhancement):
     * 1. Exclude recipes containing allergic ingredients (when ingredient data available)
     * 2. Exclude recipes containing disliked ingredients (when ingredient data available)
     * 3. Respect dietary preferences (when recipe tags available)
     * 4. Prefer cuisine preferences (can check now)
     * 
     * For now, log preferences and sort by cuisine preference.
     * Full filtering happens via AI RAG constraints in RecipeGenerationRequest.
     * 
     * @param recipes Retrieved recipes from RAG
     * @param constraints User dietary constraints
     * @param mealType Meal type for logging
     * @return Recipes optionally sorted by cuisine preference (no hard filtering yet)
     */
    private List<RetrievedRecipe> filterRecipesByPreferences(
            List<RetrievedRecipe> recipes,
            UserDietaryConstraints constraints,
            String mealType) {
        
        // Log what preferences are available for filtering
        if (!constraints.allergies.isEmpty()) {
            logger.info("[RECIPE FILTER] Allergies to avoid: {}", constraints.allergies);
        }
        if (!constraints.dislikedIngredients.isEmpty()) {
            logger.info("[RECIPE FILTER] Disliked ingredients: {}", constraints.dislikedIngredients);
        }
        if (!constraints.dietaryRestrictions.isEmpty()) {
            logger.info("[RECIPE FILTER] Dietary restrictions: {}", constraints.dietaryRestrictions);
        }
        if (!constraints.cuisinePreferences.isEmpty()) {
            logger.info("[RECIPE FILTER] Preferred cuisines: {}", constraints.cuisinePreferences);
        }
        
        // Sort recipes by cuisine preference (best-effort, no hard filtering)
        List<RetrievedRecipe> sorted = new ArrayList<>(recipes);
        
        if (!constraints.cuisinePreferences.isEmpty()) {
            sorted.sort((r1, r2) -> {
                boolean r1Match = constraints.cuisinePreferences.contains(r1.getCuisine());
                boolean r2Match = constraints.cuisinePreferences.contains(r2.getCuisine());
                // Preferred cuisines first
                return Boolean.compare(r2Match, r1Match);
            });
            
            // Log sorting results
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                RetrievedRecipe r = sorted.get(i);
                boolean isPreferred = constraints.cuisinePreferences.contains(r.getCuisine());
                logger.debug("[RECIPE FILTER] Top-{}: {} ({}){}", 
                    i+1, r.getTitle(), r.getCuisine(), isPreferred ? " [PREFERRED]" : "");
            }
        } else {
            // Log all retrieved recipes if no cuisine preference
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                logger.debug("[RECIPE FILTER] Retrieved-{}: {} ({})", 
                    i+1, sorted.get(i).getTitle(), sorted.get(i).getCuisine());
            }
        }
        
        // NOTE: Strict filtering (allergies, dislikes, dietary) happens in AI prompt
        // via RecipeGenerationRequest constraints, which are passed to RecipeGenerationService
        // and included in the augmented prompt to Groq. AI ensures compliance.
        
        return sorted;
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
            ZoneId zoneId,
            String recipeStableId) {
        
        logger.info("[MEAL_CREATION] convertToMeal called: title={}, mealType={}, recipeStableId={}", 
            generatedRecipe.getTitle(), slot.getMealType(), recipeStableId);
        
        // Determine meal time based on type
        LocalTime mealTime = getMealTime(slot.getMealType(), slot.getIndex());
        LocalDateTime plannedTime = LocalDateTime.of(date, mealTime);
        
        // Convert meal type string to enum
        MealType mealType = convertToMealType(slot.getMealType());
        
        // Create Meal entity
        Meal meal = new Meal(dayPlan, mealType, slot.getIndex(), plannedTime);
        
        // Store calorie target for nutrition calculation
        meal.setCalorieTarget(slot.getCalorieTarget());
        meal.setPlannedCalories(slot.getCalorieTarget()); // Initialize from slot
        logger.info("[MEAL_CREATION] Created meal: {}[{}] with calorieTarget={}, plannedCalories={}, title={}", 
            slot.getMealType(), slot.getIndex(), slot.getCalorieTarget(), slot.getCalorieTarget(), generatedRecipe.getTitle());
        
        // Store recipe stable ID if this meal references a DB recipe
        if (recipeStableId != null) {
            meal.setRecipeId(recipeStableId);
            logger.info("[MEAL_CREATION] ✓ SET recipe_id={} on meal", recipeStableId);
        } else {
            logger.info("[MEAL_CREATION] ✗ recipe_id is NULL - AI generated meal");
        }
        
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
        meal.setCalorieTarget(slot.getCalorieTarget());
        meal.setPlannedCalories(slot.getCalorieTarget()); // Initialize from slot
        logger.info("[MEAL_CREATION] Created placeholder meal: {}[{}] with calorieTarget={}, plannedCalories={}", 
            slot.getMealType(), slot.getIndex(), slot.getCalorieTarget(), slot.getCalorieTarget());
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
            constraints.dislikedIngredients = new ArrayList<>(nutPrefs.getDislikedIngredients());
            constraints.cuisinePreferences = new ArrayList<>(nutPrefs.getCuisinePreferences());
            constraints.dietaryPreferences = new HashMap<>();
            // Convert Set to Map for compatibility with RecipeGenerationRequest
            for (String pref : nutPrefs.getDietaryPreferences()) {
                constraints.dietaryPreferences.put(pref, true);
            }
            constraints.snackCount = nutPrefs.getSnackCount();
            // Read meal counts from profile
            constraints.breakfastCount = nutPrefs.getBreakfastCount() != null ? nutPrefs.getBreakfastCount() : 1;
            constraints.lunchCount = nutPrefs.getLunchCount() != null ? nutPrefs.getLunchCount() : 1;
            constraints.dinnerCount = nutPrefs.getDinnerCount() != null ? nutPrefs.getDinnerCount() : 1;
        } else {
            // Fallback to UserEntity JSON fields
            constraints.dietaryRestrictions = parseCsvField(user.getDietaryRestrictionsJson());
            constraints.dietaryPreferences = new HashMap<>();
            List<String> prefs = parseCsvField(user.getDietaryPreferencesJson());
            for (String pref : prefs) {
                constraints.dietaryPreferences.put(pref, true);
            }
            constraints.allergies = new ArrayList<>();
            constraints.dislikedIngredients = new ArrayList<>();
            constraints.cuisinePreferences = new ArrayList<>();
            constraints.snackCount = null;
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
        List<String> dislikedIngredients = new ArrayList<>();
        Map<String, Boolean> dietaryPreferences = new HashMap<>();
        List<String> cuisinePreferences = new ArrayList<>();
        Integer snackCount;
        int breakfastCount = 1;
        int lunchCount = 1;
        int dinnerCount = 1;
    }
}
