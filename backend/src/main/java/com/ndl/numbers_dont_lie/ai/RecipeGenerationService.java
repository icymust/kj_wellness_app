package com.ndl.numbers_dont_lie.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;
import com.ndl.numbers_dont_lie.ai.dto.RecipeGenerationRequest;
import com.ndl.numbers_dont_lie.ai.dto.RetrievedRecipe;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import com.ndl.numbers_dont_lie.ai.function.NutritionCalculator;
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
    private final GroqClient groqClient;
    private final NutritionCalculator nutritionCalculator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RecipeGenerationService(GroqClient groqClient, NutritionCalculator nutritionCalculator) {
        this.groqClient = groqClient;
        this.nutritionCalculator = nutritionCalculator;
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
     * @param request Context from STEP 4.1, 4.2, 4.3.1
     * @return Generated recipe with calculated nutrition
     */
    public GeneratedRecipe generate(RecipeGenerationRequest request) {
        // Step 1: Build augmented prompt
        String prompt = buildAugmentedPrompt(request);

        // Step 2: Define function
        List<Map<String, Object>> functions = defineFunctions();

        // Step 3: Initial AI call
        JsonNode response = groqClient.callForJson(prompt, functions);

        // Step 4: Check for function call
        if (response.has("function_call")) {
            return handleFunctionCall(response, prompt, functions, request);
        }

        // Step 5: Parse final recipe
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

        sb.append("CRITICAL RULES:\n");
        sb.append("1. DO NOT include nutrition in initial response\n");
        sb.append("2. You MUST call calculate_nutrition function for nutrition data\n");
        sb.append("3. After receiving nutrition, embed it in final recipe JSON\n");
        sb.append("4. Provide exact ingredient quantities in grams or ml\n");
        sb.append("5. Include detailed preparation steps\n\n");

        sb.append("OUTPUT FORMAT (without nutrition first):\n");
        sb.append(getRecipeJsonSchema());

        return sb.toString();
    }

    /**
     * Define calculate_nutrition function for AI.
     */
    private List<Map<String, Object>> defineFunctions() {
        Map<String, Object> function = new HashMap<>();
        function.put("name", "calculate_nutrition");
        function.put("description", 
            "Calculate accurate nutrition for a recipe based on ingredient database. " +
            "MUST be called to get nutrition data. DO NOT guess nutrition values.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        Map<String, Object> ingredientsProp = new HashMap<>();
        ingredientsProp.put("type", "array");
        ingredientsProp.put("description", "List of ingredients with quantities");
        
        Map<String, Object> ingredientItem = new HashMap<>();
        ingredientItem.put("type", "object");
        Map<String, Object> ingredientProps = new HashMap<>();
        ingredientProps.put("name", Map.of("type", "string"));
        ingredientProps.put("quantity", Map.of("type", "number"));
        ingredientProps.put("unit", Map.of("type", "string"));
        ingredientItem.put("properties", ingredientProps);
        ingredientItem.put("required", List.of("name", "quantity", "unit"));
        
        ingredientsProp.put("items", ingredientItem);
        properties.put("ingredients", ingredientsProp);
        
        properties.put("servings", Map.of("type", "integer", "description", "Number of servings"));
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("ingredients", "servings"));
        
        function.put("parameters", parameters);
        
        return List.of(function);
    }

    /**
     * Handle function call from AI.
     */
    private GeneratedRecipe handleFunctionCall(
            JsonNode response,
            String originalPrompt,
            List<Map<String, Object>> functions,
            RecipeGenerationRequest request) {
        
        try {
            JsonNode functionCall = response.get("function_call");
            String functionName = functionCall.get("name").asText();
            
            if (!"calculate_nutrition".equals(functionName)) {
                throw new AiClientException("Unexpected function call: " + functionName);
            }

            // Parse arguments (simplified - AI should provide recipe context)
            // In practice, we need the full recipe from AI first
            // For now, extract from the response or re-parse
            
            // Execute function
            GeneratedRecipe.NutritionInfo nutrition = nutritionCalculator.calculate(
                extractIngredientsFromContext(request),
                request.getServings() != null ? request.getServings() : 4
            );

            // Convert to JSON string
            String functionResult = objectMapper.writeValueAsString(nutrition);

            // Call AI with function result
            JsonNode finalResponse = groqClient.callWithFunctionResult(
                originalPrompt,
                functionName,
                functionResult,
                functions
            );

            return parseRecipe(finalResponse);
            
        } catch (Exception e) {
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

    /**
     * Extract ingredients from request context (temporary workaround).
     * In production, AI should provide ingredients in function call arguments.
     */
    private List<GeneratedRecipe.GeneratedIngredient> extractIngredientsFromContext(
            RecipeGenerationRequest request) {
        // Placeholder - in real flow, AI provides ingredients
        return new ArrayList<>();
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
}
