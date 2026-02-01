package com.ndl.numbers_dont_lie.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.GroqClient;
import com.ndl.numbers_dont_lie.ai.dto.AiGeneratedRecipePayload;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.recipe.entity.DifficultyLevel;
import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
import com.ndl.numbers_dont_lie.recipe.entity.MealType;
import com.ndl.numbers_dont_lie.recipe.entity.PreparationStep;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.RecipeIngredient;
import com.ndl.numbers_dont_lie.recipe.repository.IngredientRepository;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiRecipeMvpService {
    private static final Logger logger = LoggerFactory.getLogger(AiRecipeMvpService.class);
    private final GroqClient groqClient;
    private final NutritionalPreferencesRepository nutritionalPreferencesRepository;
    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final DayPlanRepository dayPlanRepository;
    private final MealRepository mealRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiRecipeMvpService(
            GroqClient groqClient,
            NutritionalPreferencesRepository nutritionalPreferencesRepository,
            RecipeRepository recipeRepository,
            IngredientRepository ingredientRepository,
            DayPlanRepository dayPlanRepository,
            MealRepository mealRepository) {
        this.groqClient = groqClient;
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.dayPlanRepository = dayPlanRepository;
        this.mealRepository = mealRepository;
    }

    @Transactional
    public Recipe generateAiRecipeAndAttach(Long userId, String mealTypeRaw, Long mealId) {
        if (userId == null || mealTypeRaw == null || mealTypeRaw.isBlank()) {
            throw new IllegalArgumentException("userId and mealType are required");
        }

        String mealTypeNormalized = mealTypeRaw.trim().toUpperCase(Locale.ROOT);
        MealType mealType = MealType.fromString(mealTypeNormalized);

        logger.info("[AI_RECIPE] Generating AI recipe for userId={} mealType={}", userId, mealTypeNormalized);

        NutritionalPreferences prefs = nutritionalPreferencesRepository.findByUserId(userId).orElse(null);
        List<String> dietaryPrefs = prefs != null ? new ArrayList<>(prefs.getDietaryPreferences()) : List.of();
        List<String> allergies = prefs != null ? new ArrayList<>(prefs.getAllergies()) : List.of();
        List<String> disliked = prefs != null ? new ArrayList<>(prefs.getDislikedIngredients()) : List.of();
        List<String> cuisines = prefs != null ? new ArrayList<>(prefs.getCuisinePreferences()) : List.of();
        Integer calorieTarget = prefs != null ? prefs.getCalorieTarget() : null;

        String prompt = buildPrompt(mealTypeNormalized, dietaryPrefs, allergies, disliked, cuisines, calorieTarget);
        JsonNode response = groqClient.callForJson(prompt, 0.6);
        AiGeneratedRecipePayload payload = parsePayload(response);

        Recipe recipe = persistRecipe(payload, mealType);
        if (mealId != null) {
            updateMealById(userId, mealId, recipe);
        } else {
            updateTodayMeal(userId, mealTypeNormalized, recipe);
        }

        logger.info("[AI_RECIPE] AI recipe saved with id={}", recipe.getId());
        return recipe;
    }

    private String buildPrompt(
            String mealType,
            List<String> dietaryPrefs,
            List<String> allergies,
            List<String> disliked,
            List<String> cuisines,
            Integer calorieTarget) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a recipe as STRICT JSON with the exact schema below.\n");
        sb.append("Return JSON only. No markdown, no extra text.\n\n");
        sb.append("Schema:\n");
        sb.append("{\n");
        sb.append("  \"name\": string,\n");
        sb.append("  \"description\": string,\n");
        sb.append("  \"mealType\": string,\n");
        sb.append("  \"cuisine\": string,\n");
        sb.append("  \"ingredients\": [ { \"name\": string, \"quantity\": number, \"unit\": string } ],\n");
        sb.append("  \"steps\": [ string ],\n");
        sb.append("  \"nutrition\": { \"calories\": number, \"protein\": number, \"carbs\": number, \"fats\": number }\n");
        sb.append("}\n\n");
        sb.append("Example output:\n");
        sb.append("{\"name\":\"Herbed Oat Bowl\",\"description\":\"Warm savory oats with herbs.\",");
        sb.append("\"mealType\":\"BREAKFAST\",\"cuisine\":\"Mediterranean\",");
        sb.append("\"ingredients\":[{\"name\":\"oats\",\"quantity\":60,\"unit\":\"g\"},{\"name\":\"olive oil\",\"quantity\":10,\"unit\":\"ml\"}],");
        sb.append("\"steps\":[\"Simmer oats until tender.\",\"Stir in herbs and oil.\"],");
        sb.append("\"nutrition\":{\"calories\":380,\"protein\":18,\"carbs\":45,\"fats\":14}}\n\n");
        sb.append("Constraints:\n");
        sb.append("- mealType: ").append(mealType).append("\n");
        sb.append("- dietary preferences: ").append(dietaryPrefs).append("\n");
        sb.append("- allergies/intolerances: ").append(allergies).append("\n");
        sb.append("- disliked ingredients: ").append(disliked).append("\n");
        sb.append("- preferred cuisines: ").append(cuisines).append("\n");
        if (calorieTarget != null) {
            sb.append("- calorie target (hint, not strict): ").append(calorieTarget).append("\n");
        }
        sb.append("\n");
        sb.append("Use realistic ingredients and quantities in grams or milliliters.\n");
        sb.append("Ensure steps are clear and concise.\n");
        sb.append("Ensure the recipe feels distinct and not a generic repeat; include a small unique twist.\n");
        return sb.toString();
    }

    private AiGeneratedRecipePayload parsePayload(JsonNode response) {
        try {
            AiGeneratedRecipePayload payload = objectMapper.convertValue(response, AiGeneratedRecipePayload.class);
            if (payload.getName() == null || payload.getName().isBlank()) {
                throw new IllegalArgumentException("AI recipe name missing");
            }
            if (payload.getIngredients() == null || payload.getIngredients().isEmpty()) {
                throw new IllegalArgumentException("AI recipe ingredients missing");
            }
            if (payload.getSteps() == null || payload.getSteps().isEmpty()) {
                throw new IllegalArgumentException("AI recipe steps missing");
            }
            return payload;
        } catch (IllegalArgumentException e) {
            throw new AiClientException("Invalid AI recipe payload: " + e.getMessage(), e);
        }
    }

    private Recipe persistRecipe(AiGeneratedRecipePayload payload, MealType mealType) {
        String stableId = generateNextRecipeStableId();

        Recipe recipe = new Recipe();
        recipe.setStableId(stableId);
        recipe.setTitle(payload.getName());
        recipe.setCuisine(payload.getCuisine());
        recipe.setMeal(mealType);
        recipe.setServings(1);
        recipe.setSummary(payload.getDescription());
        recipe.setTimeMinutes(null);
        recipe.setDifficultyLevel(DifficultyLevel.MEDIUM);
        recipe.setSource("ai-generated");
        recipe.setImageUrl(null);
        recipe.setAiGenerated(true);

        List<RecipeIngredient> recipeIngredients = new ArrayList<>();
        for (AiGeneratedRecipePayload.IngredientItem item : payload.getIngredients()) {
            if (item == null || item.getName() == null || item.getName().isBlank()) {
                continue;
            }
            Ingredient ingredient = resolveIngredient(item.getName(), item.getUnit());
            Double quantity = item.getQuantity() != null ? item.getQuantity() : 100.0;
            RecipeIngredient recipeIngredient = new RecipeIngredient(recipe, ingredient, quantity);
            recipeIngredients.add(recipeIngredient);
        }
        recipe.setIngredients(recipeIngredients);

        List<PreparationStep> steps = new ArrayList<>();
        List<String> stepTexts = payload.getSteps();
        for (int i = 0; i < stepTexts.size(); i++) {
            String text = stepTexts.get(i);
            if (text == null || text.isBlank()) {
                continue;
            }
            PreparationStep step = new PreparationStep(recipe, i + 1, null, text);
            steps.add(step);
        }
        recipe.setPreparationSteps(steps);

        return recipeRepository.save(recipe);
    }

    private Ingredient resolveIngredient(String name, String unit) {
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        Optional<Ingredient> exact = ingredientRepository.findByLabel(normalized);
        if (exact.isPresent()) {
            return exact.get();
        }

        List<Ingredient> fuzzy = ingredientRepository.findByLabelContainingIgnoreCase(normalized);
        if (!fuzzy.isEmpty()) {
            return fuzzy.get(0);
        }

        String stableId = generateNextIngredientStableId();
        String normalizedUnit = unit != null ? unit.trim().toLowerCase(Locale.ROOT) : "gram";
        Ingredient ingredient = new Ingredient(stableId, normalized, normalizedUnit, 100.0, 0.0, 0.0, 0.0, 0.0);
        return ingredientRepository.save(ingredient);
    }

    private String generateNextRecipeStableId() {
        long nextId = recipeRepository.findTopByOrderByIdDesc()
            .map(r -> r.getId() + 1)
            .orElse(1L);
        return String.format("r%05d", nextId);
    }

    private String generateNextIngredientStableId() {
        long nextId = ingredientRepository.findTopByOrderByIdDesc()
            .map(i -> i.getId() + 1)
            .orElse(1L);
        return String.format("ing%013d", nextId);
    }

    private void updateTodayMeal(Long userId, String mealTypeRaw, Recipe recipe) {
        LocalDate today = LocalDate.now();
        DayPlan dayPlan = dayPlanRepository
            .findByUserIdAndDateWithMealsAndDuration(userId, today, PlanDuration.DAILY)
            .orElseThrow(() -> new IllegalArgumentException("No day plan found for today"));

        com.ndl.numbers_dont_lie.mealplan.entity.Meal targetMeal = dayPlan.getMeals().stream()
            .filter(meal -> meal.getMealType() != null && meal.getMealType().name().equalsIgnoreCase(mealTypeRaw))
            .sorted(Comparator.comparingInt(m -> m.getIndex() != null ? m.getIndex() : 0))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No meal of type " + mealTypeRaw + " found for today"));

        targetMeal.setRecipeId(recipe.getStableId());
        targetMeal.setCustomMealName(recipe.getTitle());
        targetMeal.setIsCustom(false);
        if (targetMeal.getPlannedCalories() == null && targetMeal.getCalorieTarget() != null) {
            targetMeal.setPlannedCalories(targetMeal.getCalorieTarget());
        }

        mealRepository.save(targetMeal);
    }

    private void updateMealById(Long userId, Long mealId, Recipe recipe) {
        com.ndl.numbers_dont_lie.mealplan.entity.Meal meal = mealRepository.findById(mealId)
            .orElseThrow(() -> new IllegalArgumentException("Meal not found: " + mealId));

        if (!meal.getDayPlan().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Meal does not belong to user");
        }

        meal.setRecipeId(recipe.getStableId());
        meal.setCustomMealName(recipe.getTitle());
        meal.setIsCustom(false);
        if (meal.getPlannedCalories() == null && meal.getCalorieTarget() != null) {
            meal.setPlannedCalories(meal.getCalorieTarget());
        }

        mealRepository.save(meal);
    }
}
