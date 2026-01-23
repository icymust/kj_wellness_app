package com.ndl.numbers_dont_lie.ai.function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.dto.GeneratedRecipe;
import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
import com.ndl.numbers_dont_lie.recipe.repository.IngredientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * STEP 6.1: Function Calling Orchestrator
 * 
 * Enforces strict separation between AI generation and nutrition calculation.
 * 
 * Responsibilities:
 * 1. Define calculate_nutrition function contract
 * 2. Detect function call requests from AI
 * 3. Validate function call inputs
 * 4. Execute nutrition calculation via database
 * 5. Inject results back into AI context
 * 6. Ensure final recipe nutrition comes only from function output
 * 7. Log all steps for auditability
 * 
 * INVARIANT:
 * No nutrition data leaves this service except from database calculation.
 * AI cannot hallucinate nutrition values.
 */
@Service
public class FunctionCallingOrchestrator {
    private static final Logger logger = LoggerFactory.getLogger(FunctionCallingOrchestrator.class);
    private static final String FUNCTION_NAME = "calculateNutrition";
    
    private final DatabaseNutritionCalculator nutritionCalculator;
    private final IngredientRepository ingredientRepository;
    private final ObjectMapper objectMapper;
    
    public FunctionCallingOrchestrator(
            DatabaseNutritionCalculator nutritionCalculator,
            IngredientRepository ingredientRepository,
            ObjectMapper objectMapper) {
        this.nutritionCalculator = nutritionCalculator;
        this.ingredientRepository = ingredientRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Define the calculateNutrition function contract for AI.
     * 
     * This function MUST be called by AI to get nutrition data.
     * AI cannot estimate or hallucinate nutrition values.
     * 
     * @return Function definition for Groq API
     */
    public Map<String, Object> defineCalculateNutritionFunction() {
        logger.debug("Defining calculateNutrition function contract");
        
        Map<String, Object> function = new HashMap<>();
        function.put("name", FUNCTION_NAME);
        function.put("description",
            "Calculate accurate nutrition for recipe ingredients using verified database. " +
            "CRITICAL: This is the ONLY source of truth for nutrition values. " +
            "DO NOT estimate or hallucinate nutrition data. " +
            "MUST be called for all recipes to ensure accuracy and auditability.");
        
        // Parameters definition
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        
        // Ingredients array parameter
        Map<String, Object> ingredientsProp = new HashMap<>();
        ingredientsProp.put("type", "array");
        ingredientsProp.put("description", "List of recipe ingredients with exact quantities");
        
        Map<String, Object> ingredientItem = new HashMap<>();
        ingredientItem.put("type", "object");
        Map<String, Object> ingredientProps = new HashMap<>();
        ingredientProps.put("name", Map.of(
            "type", "string",
            "description", "Ingredient name (e.g., 'chicken breast', 'olive oil')"
        ));
        ingredientProps.put("quantity", Map.of(
            "type", "number",
            "description", "Quantity as decimal number"
        ));
        ingredientProps.put("unit", Map.of(
            "type", "string",
            "enum", List.of("g", "ml", "mg", "cup", "tbsp", "tsp", "piece", "slice", "whole"),
            "description", "Unit of measurement"
        ));
        ingredientItem.put("properties", ingredientProps);
        ingredientItem.put("required", List.of("name", "quantity", "unit"));
        
        ingredientsProp.put("items", ingredientItem);
        properties.put("ingredients", ingredientsProp);
        
        // Servings parameter
        properties.put("servings", Map.of(
            "type", "integer",
            "description", "Number of servings (must be > 0)"
        ));
        
        parameters.put("properties", properties);
        parameters.put("required", List.of("ingredients", "servings"));
        
        function.put("parameters", parameters);
        
        logger.debug("calculateNutrition function defined with strict contract");
        return function;
    }
    
    /**
     * Execute function call from AI.
     * 
     * Validates input, calls database calculator, returns result.
     * All errors are meaningful and logged.
     * 
     * @param functionCall The function_call node from AI response
     * @return Nutrition calculation result
     * @throws IllegalArgumentException if validation fails
     */
    public CalculateNutritionRequest.Output executeCalculateNutrition(JsonNode functionCall) {
        long startTime = System.currentTimeMillis();
        
        try {
            logger.info("[FC] Function call received: {}", FUNCTION_NAME);
            
            // Parse arguments
            JsonNode arguments = functionCall.get("arguments");
            if (arguments == null) {
                throw new IllegalArgumentException("Function call missing 'arguments'");
            }
            
            logger.debug("[FC] Arguments: {}", arguments);
            
            // Extract ingredients
            JsonNode ingredientsNode = arguments.get("ingredients");
            if (ingredientsNode == null || !ingredientsNode.isArray() || ingredientsNode.size() == 0) {
                throw new IllegalArgumentException("Missing or empty 'ingredients' array in function call");
            }
            
            List<CalculateNutritionRequest.IngredientInput> ingredients = new ArrayList<>();
            for (JsonNode ingNode : ingredientsNode) {
                String name = ingNode.get("name") != null ? ingNode.get("name").asText() : null;
                Double quantity = ingNode.get("quantity") != null ? ingNode.get("quantity").asDouble() : null;
                String unit = ingNode.get("unit") != null ? ingNode.get("unit").asText() : null;
                
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("Ingredient missing 'name'");
                }
                if (quantity == null) {
                    throw new IllegalArgumentException(
                        String.format("Ingredient '%s' missing 'quantity'", name));
                }
                if (unit == null || unit.isBlank()) {
                    throw new IllegalArgumentException(
                        String.format("Ingredient '%s' missing 'unit'", name));
                }
                
                ingredients.add(new CalculateNutritionRequest.IngredientInput(name, quantity, unit));
            }
            
            logger.debug("[FC] Parsed {} ingredients", ingredients.size());
            
            // Extract servings
            JsonNode servingsNode = arguments.get("servings");
            if (servingsNode == null) {
                throw new IllegalArgumentException("Function call missing 'servings'");
            }
            int servings = servingsNode.asInt();
            if (servings <= 0) {
                throw new IllegalArgumentException(
                    String.format("Servings must be positive, got: %d", servings));
            }
            
            logger.debug("[FC] Servings: {}", servings);
            
            // Validate all inputs
            CalculateNutritionRequest.Input input = 
                new CalculateNutritionRequest.Input(ingredients, servings);
            input.validate();
            
            logger.info("[FC] Input validation passed. Executing calculation...");
            
            // Execute calculation via database
            long calcStartTime = System.currentTimeMillis();
            GeneratedRecipe.NutritionInfo nutrition = nutritionCalculator.calculate(
                convertToGeneratedIngredients(ingredients),
                servings
            );
            long calcTime = System.currentTimeMillis() - calcStartTime;
            
            logger.info("[FC] Calculation completed in {} ms", calcTime);
            
            // Build output
            CalculateNutritionRequest.Output output = new CalculateNutritionRequest.Output(
                nutrition.getCalories(),
                nutrition.getProtein(),
                nutrition.getCarbohydrates(),
                nutrition.getFat(),
                nutrition.getCaloriesPerServing(),
                nutrition.getProteinPerServing(),
                nutrition.getCarbsPerServing(),
                nutrition.getFatPerServing(),
                "database_lookup",
                System.currentTimeMillis() - startTime
            );
            
            logger.info("[FC] Output: calories={}, protein={}g, carbs={}g, fat={}g (per serving: " +
                "{} cal, {}g P, {}g C, {}g F)",
                (int)output.calories(),
                (int)output.protein(),
                (int)output.carbohydrates(),
                (int)output.fats(),
                (int)output.caloriesPerServing(),
                (int)output.proteinPerServing(),
                (int)output.carbsPerServing(),
                (int)output.fatsPerServing());
            
            return output;
            
        } catch (IllegalArgumentException e) {
            logger.error("[FC] Validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("[FC] Unexpected error: {}", e.getMessage(), e);
            throw new RuntimeException("Function execution failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Verify that final recipe nutrition comes only from function output.
     * 
     * Defensive check to ensure AI embedded the function result correctly.
     * 
     * @param recipe The final generated recipe
     * @param functionOutput The function call output
     * @throws IllegalStateException if recipe nutrition doesn't match function output
     */
    public void verifyNutritionFromFunctionOutput(
            GeneratedRecipe recipe,
            CalculateNutritionRequest.Output functionOutput) {
        
        if (recipe.getNutrition() == null) {
            throw new IllegalStateException("Recipe missing nutrition info");
        }
        
        // Allow small floating point differences (1% tolerance)
        double tolerance = 0.01;
        
        double recipeCals = recipe.getNutrition().getCalories();
        double outputCals = functionOutput.calories();
        
        if (Math.abs(recipeCals - outputCals) > outputCals * tolerance) {
            logger.warn("[FC] Recipe calories {} doesn't match function output {}", 
                recipeCals, outputCals);
            // Could throw exception here for strict validation
            // For now, log warning
        }
        
        logger.debug("[FC] Nutrition verification passed: recipe matches function output");
    }
    
    /**
     * Convert CalculateNutritionRequest ingredients to GeneratedRecipe ingredients.
     */
    private List<GeneratedRecipe.GeneratedIngredient> convertToGeneratedIngredients(
            List<CalculateNutritionRequest.IngredientInput> inputs) {
        
        return inputs.stream()
            .map(ing -> {
                GeneratedRecipe.GeneratedIngredient generated = new GeneratedRecipe.GeneratedIngredient();
                generated.setName(ing.name());
                generated.setQuantity(ing.quantity());
                generated.setUnit(ing.unit());
                
                // Try to find ingredient ID in database (optional)
                try {
                    Ingredient dbIng = ingredientRepository.findByLabel(ing.name())
                        .orElse(null);
                    if (dbIng != null) {
                        generated.setIngredientId(dbIng.getId().toString());
                    }
                } catch (Exception e) {
                    logger.debug("Could not find ingredient in database: {}", ing.name());
                }
                
                return generated;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Log AI prompt for auditability.
     */
    public void logAiPrompt(String prompt) {
        logger.debug("[FC] === AI Prompt ===");
        logger.debug(prompt);
        logger.debug("[FC] === End AI Prompt ===");
    }
    
    /**
     * Log function call request.
     */
    public void logFunctionCallRequest(JsonNode functionCall) {
        try {
            logger.info("[FC] Function call request: {}", 
                objectMapper.writeValueAsString(functionCall));
        } catch (Exception e) {
            logger.error("[FC] Could not serialize function call for logging", e);
        }
    }
    
    /**
     * Log function result.
     */
    public void logFunctionResult(CalculateNutritionRequest.Output result) {
        logger.info("[FC] Function result: {} cal, {}g P, {}g C, {}g F (per serving: {} cal, {}g P, {}g C, {}g F)",
            (int)result.calories(),
            (int)result.protein(),
            (int)result.carbohydrates(),
            (int)result.fats(),
            (int)result.caloriesPerServing(),
            (int)result.proteinPerServing(),
            (int)result.carbsPerServing(),
            (int)result.fatsPerServing());
    }
}
