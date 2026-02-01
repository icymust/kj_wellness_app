package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;
import com.ndl.numbers_dont_lie.ai.dto.RecipeGenerationRequest;
import com.ndl.numbers_dont_lie.ai.dto.RetrievedRecipe;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import com.ndl.numbers_dont_lie.ai.function.NutritionCalculator;
import com.ndl.numbers_dont_lie.ai.function.FunctionCallingOrchestrator;
import com.ndl.numbers_dont_lie.ai.function.CalculateNutritionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * STEP 4.3.2: Recipe Generation with RAG + Function Calling
 * 
 * COMPLETE AI PIPELINE:
 * ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
 * │ STEP 4.1 │ →  │ STEP 4.2 │ →  │STEP 4.3.1│ →  │STEP 4.3.2│ →  │  OUTPUT  │
 * │ Strategy │    │MealSlots │    │Retrieval │    │Generation│    │  Recipe  │
 * └──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
 *      ↓               ↓               ↓               ↓
 *   Calories      Distribution    RAG Context    AI + Functions
 *   Macros        Per Meal        Similar        calculate_nutrition
 *   Constraints                   Recipes
 * 
 * WHY RAG:
 * - AI adapts real recipes, not inventing from scratch
 * - Improves plausibility and diversity
 * - Reduces hallucination of impossible dishes
 * - Provides proven ingredient combinations
 * 
 * WHY FUNCTION CALLING:
 * - Nutrition MUST be accurate, not guessed
 * - Uses verified ingredient database
 * - Ensures consistency across all recipes
 * - Provides audit trail for nutritional claims
 * - Eliminates risk of hallucinated nutrition data
 * 
 * This service orchestrates:
 * 1. Prompt augmentation with retrieved recipe summaries
 * 2. AI generation of recipe structure (NO nutrition)
 * 3. Function call to calculate nutrition
 * 4. AI embedding of nutrition into final recipe JSON
 */
@Service
public class RecipeGenerationService {
    private static final Logger logger = LoggerFactory.getLogger(RecipeGenerationService.class);
    
    private final GroqClient groqClient;
    private final NutritionCalculator nutritionCalculator;
    private final FunctionCallingOrchestrator functionOrchestrator;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // DEBUG ONLY: Mock mode for recipe generation (bypasses Groq API)
    @Value("${app.debug.ai.mock:false}")
    private boolean mockMode;

    public RecipeGenerationService(
            GroqClient groqClient,
            NutritionCalculator nutritionCalculator,
            FunctionCallingOrchestrator functionOrchestrator) {
        this.groqClient = groqClient;
        this.nutritionCalculator = nutritionCalculator;
        this.functionOrchestrator = functionOrchestrator;
    }

    /**
     * Generate a recipe for a specific meal slot using RAG + function calling.
     * 
     * Flow:
     * 1. Build augmented prompt with retrieved recipe context
     * 2. Define calculate_nutrition function
     * 3. Call AI (may invoke function)
     * 4. If function called: execute and return result to AI
     * 5. AI returns final recipe JSON with nutrition embedded
     * 
     * DEBUG ONLY: If mock mode enabled, returns mock recipe instead.
     * 
     * @param request Context from STEP 4.1, 4.2, 4.3.1
     * @return Generated recipe with calculated nutrition
     */
    public GeneratedRecipe generate(RecipeGenerationRequest request) {
        logger.info("[STEP 4.3.2] Starting recipe generation with strict function calling contract");
        
        // DEBUG ONLY: Check mock mode first
        if (mockMode) {
            logger.warn("[DEBUG MOCK] Returning mock recipe for {} (Groq API bypassed)", request.getMealSlot().getMealType());
            GeneratedRecipe mockRecipe = createMockRecipe(request);
            logger.warn("[DEBUG MOCK] Generated mock recipe: {} ({} cal)", mockRecipe.getTitle(), mockRecipe.getNutrition().getCalories());
            return mockRecipe;
        }
        
        // Step 1: Build augmented prompt
        String prompt = buildAugmentedPrompt(request);
        functionOrchestrator.logAiPrompt(prompt);

        // Step 2: Define function via orchestrator
        Map<String, Object> functionDef = functionOrchestrator.defineCalculateNutritionFunction();
        List<Map<String, Object>> functions = List.of(functionDef);

        // Step 3: Initial AI call
        logger.info("[STEP 4.3.2] Calling AI with function definition");
        JsonNode response = groqClient.callForJson(prompt, functions, 0.4);

        // Step 4: Check for function call
        if (response.has("function_call")) {
            logger.info("[STEP 4.3.2] AI requested function call");
            return handleFunctionCall(response, prompt, functions, request);
        }

        // Step 5: Parse final recipe
        logger.warn("[STEP 4.3.2] AI did not call function - this may indicate AI did not follow contract");
        return parseRecipe(response);
    }

    /**
     * Build augmented prompt with retrieved recipe context.
     * 
     * RAG Augmentation:
     * - Includes summaries of similar recipes
     * - Instructs AI to adapt, not copy
     * - Enforces dietary and macro constraints
     * - Requests structured JSON output
     */
    private String buildAugmentedPrompt(RecipeGenerationRequest request) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== STEP 4.3.2: Recipe Generation with RAG ===\n\n");

        // Context from previous steps
        sb.append("CONTEXT FROM STEP 4.1 (Strategy):\n");
        sb.append("- Strategy: ").append(request.getStrategy().getStrategyName()).append("\n");
        sb.append("- Target Calories: ").append(request.getStrategy().getTargetCalories()).append("\n");
        sb.append("- Macro Split: ").append(request.getStrategy().getMacroSplit()).append("\n\n");

        sb.append("CONTEXT FROM STEP 4.2 (Meal Slot):\n");
        var slot = request.getMealSlot();
        sb.append("- Meal Type: ").append(slot.getMealType()).append("\n");
        sb.append("- Index: ").append(slot.getIndex()).append("\n");
        sb.append("- Calorie Target: ").append(slot.getCalorieTarget()).append(" kcal\n");
        sb.append("- Macro Focus: ").append(slot.getMacroFocus()).append("\n");
        if (slot.getTimingNote() != null) {
            sb.append("- Timing Note: ").append(slot.getTimingNote()).append("\n");
        }
        sb.append("\n");

        // RAG Context: Retrieved recipes
        sb.append("CONTEXT FROM STEP 4.3.1 (Retrieved Similar Recipes):\n");
        if (request.getRetrievedRecipes() != null && !request.getRetrievedRecipes().isEmpty()) {
            sb.append("The following recipes were retrieved as similar matches:\n");
            for (int i = 0; i < Math.min(5, request.getRetrievedRecipes().size()); i++) {
                RetrievedRecipe r = request.getRetrievedRecipes().get(i);
                sb.append(String.format("%d. %s (%s) - Relevance: %.2f\n", 
                    i+1, r.getTitle(), r.getCuisine(), r.getRelevanceScore()));
            }
            sb.append("\nUSE THESE AS INSPIRATION. Adapt concepts, ingredient combinations, and techniques.\n");
            sb.append("DO NOT copy exactly. Create a new recipe that fits the constraints below.\n\n");
        } else {
            sb.append("No retrieved recipes available. Generate from scratch based on constraints.\n\n");
        }

        // User constraints
        sb.append("USER CONSTRAINTS:\n");
        if (request.getDietaryRestrictions() != null && !request.getDietaryRestrictions().isEmpty()) {
            sb.append("- Dietary Restrictions: ").append(request.getDietaryRestrictions()).append("\n");
        }
        if (request.getAllergies() != null && !request.getAllergies().isEmpty()) {
            sb.append("- Allergies to avoid: ").append(request.getAllergies()).append("\n");
        }
        if (request.getServings() != null) {
            sb.append("- Servings: ").append(request.getServings()).append("\n");
        }
        sb.append("\n");

        // Instructions
        sb.append("YOUR TASK:\n");
        sb.append("Generate a recipe that:\n");
        sb.append("1. Matches the meal type (").append(slot.getMealType()).append(")\n");
        sb.append("2. Targets approximately ").append(slot.getCalorieTarget()).append(" calories\n");
        sb.append("3. Follows the macro focus: ").append(slot.getMacroFocus()).append("\n");
        sb.append("4. Respects all dietary restrictions and allergies\n");
        sb.append("5. Is inspired by the retrieved recipes (if provided)\n\n");

        sb.append("STEP 6.1 - FUNCTION CALLING CONTRACT (CRITICAL):\n");
        sb.append("┌────────────────────────────────────────────────┐\n");
        sb.append("│ YOU MUST FOLLOW THIS CONTRACT STRICTLY         │\n");
        sb.append("└────────────────────────────────────────────────┘\n\n");
        
        sb.append("1. GENERATE RECIPE STRUCTURE FIRST (without nutrition):\n");
        sb.append("   - Title, cuisine, meal type, servings\n");
        sb.append("   - List ALL ingredients with EXACT quantities in grams/ml\n");
        sb.append("   - Detailed preparation steps\n");
        sb.append("   - NO nutrition numbers in this step\n\n");
        
        sb.append("2. THEN CALL calculateNutrition FUNCTION:\n");
        sb.append("   - Function name: calculateNutrition\n");
        sb.append("   - Input: ingredients array (with name, quantity, unit) + servings\n");
        sb.append("   - Output: calories, protein, carbs, fats (total and per serving)\n");
        sb.append("   - This is the ONLY source of truth for nutrition\n\n");
        
        sb.append("3. AFTER RECEIVING FUNCTION RESULT:\n");
        sb.append("   - Embed nutrition data into final recipe JSON\n");
        sb.append("   - Use EXACTLY the values from function output\n");
        sb.append("   - DO NOT modify, estimate, or round nutrition numbers\n\n");
        
        sb.append("ENFORCEMENT:\n");
        sb.append("- AI cannot hallucinate nutrition values\n");
        sb.append("- All nutrition MUST come from database via function call\n");
        sb.append("- Backend will verify final recipe matches function output\n");
        sb.append("- No exceptions to this contract\n\n");

        sb.append("OUTPUT FORMAT:\n");
        sb.append(getRecipeJsonSchema());

        return sb.toString();
    }

    /**
     * Handle function call from AI.
     * 
     * STEP 6.1 Contract Enforcement:
     * 1. Detect and validate function call
     * 2. Execute via orchestrator (validates inputs)
     * 3. Return result to AI
     * 4. Verify final recipe uses function output
     */
    private GeneratedRecipe handleFunctionCall(
            JsonNode response,
            String originalPrompt,
            List<Map<String, Object>> functions,
            RecipeGenerationRequest request) {
        
        try {
            JsonNode functionCall = response.get("function_call");
            String functionName = functionCall.get("name").asText();
            
            if (!"calculateNutrition".equals(functionName)) {
                throw new AiClientException("Unexpected function call: " + functionName);
            }
            
            logger.info("[STEP 6.1] Executing function call: {}", functionName);
            functionOrchestrator.logFunctionCallRequest(functionCall);

            // Execute function via orchestrator
            // This validates inputs and ensures database calculation
            CalculateNutritionRequest.Output functionResult = 
                functionOrchestrator.executeCalculateNutrition(functionCall);
            
            logger.info("[STEP 6.1] Function executed successfully");
            functionOrchestrator.logFunctionResult(functionResult);

            // Convert to JSON string for AI context
            String functionResultJson = objectMapper.writeValueAsString(functionResult);

            // Call AI with function result
            logger.info("[STEP 6.1] Injecting function result back into AI context");
            JsonNode finalResponse = groqClient.callWithFunctionResult(
                originalPrompt,
                functionName,
                functionResultJson,
                functions
            );

            // Parse final recipe
            GeneratedRecipe recipe = parseRecipe(finalResponse);
            
            // Verify nutrition comes from function output
            logger.info("[STEP 6.1] Verifying recipe nutrition matches function output");
            functionOrchestrator.verifyNutritionFromFunctionOutput(recipe, functionResult);
            
            logger.info("[STEP 6.1] Recipe generation complete with function calling contract enforced");
            return recipe;
            
        } catch (IllegalArgumentException e) {
            logger.error("[STEP 6.1] Function call validation failed: {}", e.getMessage());
            throw new AiClientException("Function call validation failed: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("[STEP 6.1] Function call handling failed: {}", e.getMessage(), e);
            throw new AiClientException("Function call handling failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse final recipe JSON from AI response.
     */
    private GeneratedRecipe parseRecipe(JsonNode json) {
        try {
            return objectMapper.treeToValue(json, GeneratedRecipe.class);
        } catch (Exception e) {
            throw new AiClientException("Failed to parse generated recipe: " + e.getMessage(), e);
        }
    }

    private String getRecipeJsonSchema() {
        return """
            {
              "title": "string",
              "cuisine": "string",
              "meal": "breakfast|lunch|dinner|snack",
              "servings": integer,
              "summary": "string (brief description)",
              "timeMinutes": integer,
              "difficultyLevel": "easy|medium|hard",
              "dietaryTags": ["string"],
              "ingredients": [
                {
                  "ingredientId": "string (optional)",
                  "name": "string",
                  "quantity": number (grams or ml),
                  "unit": "g|ml|cup|tbsp|tsp|piece|etc"
                }
              ],
              "preparationSteps": [
                {
                  "stepNumber": integer,
                  "stepTitle": "string",
                  "instruction": "string"
                }
              ]
            }
            """;
    }

    // ============================================================================
    // DEBUG ONLY: Mock Recipe Generation (bypasses Groq API)
    // ============================================================================

    /**
     * DEBUG ONLY: Generate realistic mock recipe for unblocking frontend.
     * Used when app.debug.ai.mock=true to bypass Groq API errors.
     * 
     * Generates recipes with:
     * - Realistic names and cuisines
     * - Calories matching meal slot target
     * - Proper macro distribution
     * - Real ingredients and steps
     */
    private GeneratedRecipe createMockRecipe(RecipeGenerationRequest request) {
        var mealSlot = request.getMealSlot();
        String mealType = mealSlot.getMealType();
        Double targetCalories = (double) mealSlot.getCalorieTarget();
        
        GeneratedRecipe recipe = new GeneratedRecipe();
        
        // Select recipe based on meal type and target calories
        if ("breakfast".equalsIgnoreCase(mealType)) {
            populateBreakfastRecipe(recipe, targetCalories);
        } else if ("lunch".equalsIgnoreCase(mealType)) {
            populateLunchRecipe(recipe, targetCalories);
        } else if ("dinner".equalsIgnoreCase(mealType)) {
            populateDinnerRecipe(recipe, targetCalories);
        } else if ("snack".equalsIgnoreCase(mealType)) {
            populateSnackRecipe(recipe, targetCalories);
        }
        
        // Add nutrition info based on target
        recipe.setNutrition(calculateMockNutrition(recipe, targetCalories));
        
        return recipe;
    }

    private void populateBreakfastRecipe(GeneratedRecipe recipe, Double targetCalories) {
        recipe.setMeal("breakfast");
        recipe.setServings(1);
        recipe.setTimeMinutes(15);
        recipe.setDifficultyLevel("easy");
        
        // Rotate through breakfast options based on target calories
        if (targetCalories < 400) {
            recipe.setTitle("Greek Yogurt with Berries & Granola");
            recipe.setCuisine("Mediterranean");
            recipe.setSummary("Protein-rich yogurt bowl with fresh berries and whole grain granola");
            recipe.setDietaryTags(Arrays.asList("vegetarian", "high_protein"));
            
            recipe.setIngredients(Arrays.asList(
                createIngredient("Greek yogurt", 150.0, "g"),
                createIngredient("Mixed berries", 80.0, "g"),
                createIngredient("Granola", 40.0, "g"),
                createIngredient("Honey", 10.0, "ml")
            ));
        } else {
            recipe.setTitle("Veggie-Loaded Egg Scramble with Toast");
            recipe.setCuisine("American");
            recipe.setSummary("Protein-packed scrambled eggs with vegetables and whole wheat toast");
            recipe.setDietaryTags(Arrays.asList("high_protein"));
            
            recipe.setIngredients(Arrays.asList(
                createIngredient("Eggs", 120.0, "g"),
                createIngredient("Spinach", 50.0, "g"),
                createIngredient("Bell peppers", 60.0, "g"),
                createIngredient("Whole wheat bread", 50.0, "g"),
                createIngredient("Olive oil", 5.0, "ml"),
                createIngredient("Salt and pepper", 2.0, "g")
            ));
        }
        
        recipe.setPreparationSteps(Arrays.asList(
            createStep(1, "Prepare", "Wash and chop vegetables if needed"),
            createStep(2, "Cook", "Heat pan and prepare main ingredient"),
            createStep(3, "Plate", "Arrange on plate and serve immediately")
        ));
    }

    private void populateLunchRecipe(GeneratedRecipe recipe, Double targetCalories) {
        recipe.setMeal("lunch");
        recipe.setServings(1);
        recipe.setTimeMinutes(20);
        recipe.setDifficultyLevel("easy");
        
        // Rotate through lunch options
        if (targetCalories < 700) {
            recipe.setTitle("Grilled Chicken Salad with Vinaigrette");
            recipe.setCuisine("Mediterranean");
            recipe.setSummary("Lean protein salad with fresh greens, grilled chicken, and light vinaigrette");
            recipe.setDietaryTags(Arrays.asList("high_protein", "low_carb"));
            
            recipe.setIngredients(Arrays.asList(
                createIngredient("Chicken breast", 150.0, "g"),
                createIngredient("Mixed greens", 100.0, "g"),
                createIngredient("Cherry tomatoes", 50.0, "g"),
                createIngredient("Cucumber", 60.0, "g"),
                createIngredient("Olive oil", 7.0, "ml"),
                createIngredient("Balsamic vinegar", 15.0, "ml")
            ));
        } else {
            recipe.setTitle("Salmon Rice Bowl with Roasted Vegetables");
            recipe.setCuisine("Asian");
            recipe.setSummary("Omega-3 rich salmon with brown rice and seasonal roasted vegetables");
            recipe.setDietaryTags(Arrays.asList("high_protein", "omega_3"));
            
            recipe.setIngredients(Arrays.asList(
                createIngredient("Salmon fillet", 140.0, "g"),
                createIngredient("Brown rice", 80.0, "g"),
                createIngredient("Broccoli", 80.0, "g"),
                createIngredient("Carrots", 50.0, "g"),
                createIngredient("Olive oil", 8.0, "ml"),
                createIngredient("Soy sauce", 10.0, "ml"),
                createIngredient("Ginger", 5.0, "g")
            ));
        }
        
        recipe.setPreparationSteps(Arrays.asList(
            createStep(1, "Prepare ingredients", "Wash, chop, and measure all components"),
            createStep(2, "Cook protein", "Grill, bake, or pan-fry main protein source"),
            createStep(3, "Assemble", "Combine ingredients in bowl or plate"),
            createStep(4, "Dress", "Add dressing or sauce and serve")
        ));
    }

    private void populateDinnerRecipe(GeneratedRecipe recipe, Double targetCalories) {
        recipe.setMeal("dinner");
        recipe.setServings(1);
        recipe.setTimeMinutes(30);
        recipe.setDifficultyLevel("medium");
        
        // Rotate through dinner options
        if (targetCalories < 800) {
            recipe.setTitle("Lean Turkey Meatballs with Zucchini Noodles");
            recipe.setCuisine("Italian");
            recipe.setSummary("Low-calorie protein-rich dinner with vegetable pasta alternative");
            recipe.setDietaryTags(Arrays.asList("high_protein", "low_carb", "gluten_free"));
            
            recipe.setIngredients(Arrays.asList(
                createIngredient("Ground turkey", 150.0, "g"),
                createIngredient("Zucchini", 200.0, "g"),
                createIngredient("Tomato sauce", 100.0, "ml"),
                createIngredient("Whole wheat breadcrumbs", 20.0, "g"),
                createIngredient("Garlic", 5.0, "g"),
                createIngredient("Olive oil", 5.0, "ml"),
                createIngredient("Herbs", 3.0, "g")
            ));
        } else {
            recipe.setTitle("Herb-Roasted Chicken with Quinoa & Root Vegetables");
            recipe.setCuisine("Mediterranean");
            recipe.setSummary("Complete balanced meal with lean protein, complex carbs, and nutrients");
            recipe.setDietaryTags(Arrays.asList("high_protein", "complete_meal"));
            
            recipe.setIngredients(Arrays.asList(
                createIngredient("Chicken thighs", 160.0, "g"),
                createIngredient("Quinoa", 60.0, "g"),
                createIngredient("Sweet potato", 100.0, "g"),
                createIngredient("Brussels sprouts", 100.0, "g"),
                createIngredient("Olive oil", 10.0, "ml"),
                createIngredient("Rosemary & thyme", 5.0, "g"),
                createIngredient("Lemon", 30.0, "g")
            ));
        }
        
        recipe.setPreparationSteps(Arrays.asList(
            createStep(1, "Prep", "Prepare all ingredients - wash, chop, measure"),
            createStep(2, "Season", "Season protein and vegetables with herbs"),
            createStep(3, "Cook", "Roast, grill, or pan-fry at appropriate temperature"),
            createStep(4, "Combine", "Plate main protein with sides"),
            createStep(5, "Serve", "Garnish if desired and serve hot")
        ));
    }

    private void populateSnackRecipe(GeneratedRecipe recipe, Double targetCalories) {
        recipe.setMeal("snack");
        recipe.setServings(1);
        recipe.setTimeMinutes(5);
        recipe.setDifficultyLevel("easy");
        
        // Snacks typically 150-300 calories
        recipe.setTitle("Protein Energy Balls with Nuts & Dates");
        recipe.setCuisine("Mediterranean");
        recipe.setSummary("Quick, protein-rich snack made with natural ingredients");
        recipe.setDietaryTags(Arrays.asList("vegetarian", "high_protein", "no_cook"));
        
        recipe.setIngredients(Arrays.asList(
            createIngredient("Dates", 50.0, "g"),
            createIngredient("Almonds", 30.0, "g"),
            createIngredient("Protein powder", 15.0, "g"),
            createIngredient("Coconut oil", 5.0, "ml"),
            createIngredient("Cacao powder", 5.0, "g")
        ));
        
        recipe.setPreparationSteps(Arrays.asList(
            createStep(1, "Process", "Blend all ingredients until combined"),
            createStep(2, "Form", "Roll mixture into balls"),
            createStep(3, "Chill", "Refrigerate for 30 minutes"),
            createStep(4, "Serve", "Ready to eat immediately")
        ));
    }

    /**
     * DEBUG ONLY: Calculate realistic nutrition for mock recipe.
     * Uses reasonable macro ratios:
     * - Breakfast: 30% protein, 45% carbs, 25% fat
     * - Lunch: 35% protein, 40% carbs, 25% fat
     * - Dinner: 30% protein, 40% carbs, 30% fat
     * - Snack: 25% protein, 50% carbs, 25% fat
     */
    private GeneratedRecipe.NutritionInfo calculateMockNutrition(GeneratedRecipe recipe, Double targetCalories) {
        GeneratedRecipe.NutritionInfo nutrition = new GeneratedRecipe.NutritionInfo();
        
        Integer servings = recipe.getServings() != null ? recipe.getServings() : 1;
        Double totalCalories = targetCalories;
        Double caloriesPerServing = totalCalories / servings;
        
        // Macro ratios by meal type
        Double proteinRatio, carbRatio, fatRatio;
        
        switch (recipe.getMeal().toLowerCase()) {
            case "breakfast":
                proteinRatio = 0.30;
                carbRatio = 0.45;
                fatRatio = 0.25;
                break;
            case "lunch":
                proteinRatio = 0.35;
                carbRatio = 0.40;
                fatRatio = 0.25;
                break;
            case "dinner":
                proteinRatio = 0.30;
                carbRatio = 0.40;
                fatRatio = 0.30;
                break;
            case "snack":
                proteinRatio = 0.25;
                carbRatio = 0.50;
                fatRatio = 0.25;
                break;
            default:
                proteinRatio = 0.30;
                carbRatio = 0.40;
                fatRatio = 0.30;
        }
        
        // Calculate macros in grams
        Double proteinCalories = totalCalories * proteinRatio;
        Double proteinGrams = proteinCalories / 4.0; // 1g protein = 4 cal
        
        Double carbCalories = totalCalories * carbRatio;
        Double carbGrams = carbCalories / 4.0; // 1g carbs = 4 cal
        
        Double fatCalories = totalCalories * fatRatio;
        Double fatGrams = fatCalories / 9.0; // 1g fat = 9 cal
        
        // Set total values
        nutrition.setCalories((double) Math.round(totalCalories));
        nutrition.setProtein(Math.round(proteinGrams * 10.0) / 10.0);
        nutrition.setCarbohydrates(Math.round(carbGrams * 10.0) / 10.0);
        nutrition.setFat(Math.round(fatGrams * 10.0) / 10.0);
        nutrition.setFiber(Math.round((carbGrams * 0.15) * 10.0) / 10.0); // ~15% carbs are fiber
        nutrition.setSodium((double) Math.round(500 + Math.random() * 300)); // 500-800mg
        nutrition.setSugar(Math.round(Math.max(2, totalCalories * 0.05) * 10.0) / 10.0); // ~5% of calories as sugar
        
        // Per serving
        nutrition.setCaloriesPerServing((double) Math.round(caloriesPerServing));
        nutrition.setProteinPerServing(Math.round((proteinGrams / servings) * 10.0) / 10.0);
        nutrition.setCarbsPerServing(Math.round((carbGrams / servings) * 10.0) / 10.0);
        nutrition.setFatPerServing(Math.round((fatGrams / servings) * 10.0) / 10.0);
        
        return nutrition;
    }

    private GeneratedRecipe.GeneratedIngredient createIngredient(String name, Double quantity, String unit) {
        GeneratedRecipe.GeneratedIngredient ingredient = new GeneratedRecipe.GeneratedIngredient();
        ingredient.setName(name);
        ingredient.setQuantity(quantity);
        ingredient.setUnit(unit);
        return ingredient;
    }

    private GeneratedRecipe.PreparationStep createStep(Integer number, String title, String instruction) {
        GeneratedRecipe.PreparationStep step = new GeneratedRecipe.PreparationStep();
        step.setStepNumber(number);
        step.setStepTitle(title);
        step.setInstruction(instruction);
        return step;
    }
}
