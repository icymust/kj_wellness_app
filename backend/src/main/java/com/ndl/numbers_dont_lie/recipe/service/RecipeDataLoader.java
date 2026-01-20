package com.ndl.numbers_dont_lie.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.recipe.entity.DifficultyLevel;
import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
import com.ndl.numbers_dont_lie.recipe.entity.MealType;
import com.ndl.numbers_dont_lie.recipe.entity.Nutrition;
import com.ndl.numbers_dont_lie.recipe.entity.PreparationStep;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.RecipeIngredient;
import com.ndl.numbers_dont_lie.recipe.repository.IngredientRepository;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;

/**
 * Intelligent DataLoader for recipe and ingredient data management.
 * Works with new JSON data structures:
 * - Ingredients: id, label, unit, quantity, nutrition{calories, carbs, protein, fats}
 * - Recipes: id, title, cuisine, meal, servings, ingredients[], summary, time, difficulty_level, dietary_tags[], source, img, preparation[]
 * 
 * On application startup, this component:
 * 1. Checks if data already exists in the database
 * 2. Only loads JSON data if the database is empty (recipes count == 0)
 * 3. Ensures idempotency - safe to restart application without duplicating data
 * 4. Preserves stable IDs across restarts
 * 5. Validates all data before persistence
 * 6. Generates startup report with statistics
 */
@Component
public class RecipeDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RecipeDataLoader.class);
    
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final ObjectMapper objectMapper;
    
    // Statistics tracking
    private long ingredientsBeforeCount = 0;
    private long ingredientsAfterCount = 0;
    private long recipesBeforeCount = 0;
    private long recipesAfterCount = 0;
    
    private int ingredientsInserted = 0;
    private int ingredientsSkipped = 0;
    private int recipesInserted = 0;
    private int recipesSkipped = 0;
    private final List<String> validationErrors = new ArrayList<>();
    private final Set<String> missingIngredients = new HashSet<>();
    
    private boolean loadWasPerformed = false;
    private String loadStatus = "SKIPPED";
    
    public RecipeDataLoader(IngredientRepository ingredientRepository, 
                           RecipeRepository recipeRepository,
                           ObjectMapper objectMapper) {
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("");
        log.info("=".repeat(80));
        log.info("RECIPE DATA LOADER - STARTUP CHECK");
        log.info("=".repeat(80));
        log.info("");
        
        try {
            // Step 1: Startup check - count existing data
            startupCheck();
            
            // Step 2: Conditional loading based on current state
            if (recipesBeforeCount == 0) {
                loadWasPerformed = true;
                loadStatus = "SUCCESS";
                Map<String, Ingredient> ingredientMap = loadIngredients();
                loadRecipes(ingredientMap);
            } else {
                loadStatus = "SKIPPED";
            }
            
            // Step 3: Print startup report
            printStartupReport();
            
        } catch (Exception e) {
            log.error("Fatal error during data loading", e);
            loadStatus = "FAILED";
            throw e;
        }
        
        log.info("=".repeat(80));
        log.info("");
    }

    /**
     * Check current database state for existing data
     */
    @Transactional(readOnly = true)
    protected void startupCheck() {
        ingredientsBeforeCount = ingredientRepository.count();
        recipesBeforeCount = recipeRepository.count();
        
        log.info("Startup check: {} ingredients found", ingredientsBeforeCount);
        log.info("Startup check: {} recipes found", recipesBeforeCount);
        log.info("");
    }

    /**
     * Load and persist ingredients from ingredients.json
     * Returns a map of ingredient ID -> Ingredient for recipe processing
     * 
     * NEW STRUCTURE:
     * {
     *   "id": "ing0974b137",
     *   "label": "cooked brown rice",
     *   "unit": "gram",
     *   "quantity": 100,
     *   "nutrition": { "calories": 32.0, "carbs": 3.2, "protein": 2.1, "fats": 1.2 }
     * }
     */
    @Transactional
    protected Map<String, Ingredient> loadIngredients() throws IOException {
        log.info("Loading ingredients from ingredients.json...");
        
        // Check if ingredients already exist
        if (ingredientsBeforeCount > 0) {
            log.warn("Ingredients already exist in database. Skipping ingredient load.");
            // Rebuild map from existing ingredients
            return rebuildIngredientMap();
        }
        
        Map<String, Ingredient> ingredientMap = new HashMap<>();
        
        InputStream inputStream = new ClassPathResource("data/ingredients.json").getInputStream();
        JsonNode ingredientsJson = objectMapper.readTree(inputStream);
        
        if (!ingredientsJson.isArray()) {
            throw new IllegalStateException("ingredients.json must contain an array");
        }
        
        int totalIngredients = ingredientsJson.size();
        log.info("Found {} ingredients to process", totalIngredients);
        
        for (JsonNode node : ingredientsJson) {
            try {
                Ingredient ingredient = processIngredient(node);
                
                if (ingredient != null) {
                    ingredientRepository.save(ingredient);
                    ingredientMap.put(ingredient.getStableId(), ingredient);
                    ingredientsInserted++;
                    
                    if (ingredientsInserted % 200 == 0) {
                        log.info("Processed {} / {} ingredients", ingredientsInserted, totalIngredients);
                    }
                }
                
            } catch (Exception e) {
                ingredientsSkipped++;
                String label = node.has("label") ? node.get("label").asText() : "unknown";
                String error = String.format("Failed to process ingredient '%s': %s", label, e.getMessage());
                validationErrors.add(error);
                log.error(error, e);
            }
        }
        
        ingredientsAfterCount = ingredientRepository.count();
        log.info("Ingredients loaded: {} inserted, {} skipped (total: {})", 
                 ingredientsInserted, ingredientsSkipped, ingredientsAfterCount);
        log.info("");
        
        return ingredientMap;
    }

    /**
     * Rebuild ingredient map from existing database records
     */
    @Transactional(readOnly = true)
    protected Map<String, Ingredient> rebuildIngredientMap() {
        Map<String, Ingredient> ingredientMap = new HashMap<>();
        ingredientRepository.findAll().forEach(ingredient -> 
            ingredientMap.put(ingredient.getStableId(), ingredient)
        );
        return ingredientMap;
    }

    /**
     * Process and validate a single ingredient from JSON (NEW FORMAT)
     */
    private Ingredient processIngredient(JsonNode node) {
        // Validate required fields
        if (!node.has("id") || node.get("id").asText().trim().isEmpty()) {
            validationErrors.add("Ingredient missing required field: id");
            ingredientsSkipped++;
            return null;
        }
        
        if (!node.has("label") || node.get("label").asText().trim().isEmpty()) {
            validationErrors.add("Ingredient missing required field: label");
            ingredientsSkipped++;
            return null;
        }
        
        String stableId = node.get("id").asText();
        String label = node.get("label").asText().toLowerCase().trim();
        
        // Extract fields with defaults
        String unit = node.has("unit") ? node.get("unit").asText() : "gram";
        Double quantity = node.has("quantity") ? node.get("quantity").asDouble() : 100.0;
        
        // Extract nutrition from nested object
        Double calories = 0.0;
        Double protein = 0.0;
        Double carbs = 0.0;
        Double fats = 0.0;
        
        if (node.has("nutrition") && node.get("nutrition").isObject()) {
            JsonNode nutrition = node.get("nutrition");
            calories = nutrition.has("calories") ? nutrition.get("calories").asDouble() : 0.0;
            protein = nutrition.has("protein") ? nutrition.get("protein").asDouble() : 0.0;
            carbs = nutrition.has("carbs") ? nutrition.get("carbs").asDouble() : 0.0;
            fats = nutrition.has("fats") ? nutrition.get("fats").asDouble() : 0.0;
        }
        
        // Validate numeric fields are non-negative
        if (quantity < 0 || calories < 0 || protein < 0 || carbs < 0 || fats < 0) {
            String error = String.format("Ingredient '%s' has negative values", label);
            validationErrors.add(error);
            ingredientsSkipped++;
            return null;
        }
        
        // Create with new constructor that takes Nutrition object
        Nutrition nutritionObj = new Nutrition(calories, protein, carbs, fats);
        return new Ingredient(stableId, label, unit, quantity, nutritionObj);
    }

    /**
     * Load and persist recipes from recipes.json
     * 
     * NEW STRUCTURE:
     * {
     *   "id": "r00001",
     *   "title": "...",
     *   "cuisine": "...",
     *   "meal": "...",
     *   "servings": 4,
     *   "ingredients": [{ "id": "ing...", "name": "...", "quantity": 100 }],
     *   "summary": "...",
     *   "time": 25,
     *   "difficulty_level": "easy",
     *   "dietary_tags": [...],
     *   "source": "...",
     *   "img": "...",
     *   "preparation": [{ "step": "...", "description": "...", "ingredients": [...] }]
     * }
     */
    @Transactional
    protected void loadRecipes(Map<String, Ingredient> ingredientMap) throws IOException {
        log.info("Loading recipes from recipes.json...");
        
        // Check if recipes already exist
        if (recipesBeforeCount > 0) {
            log.warn("Recipes already exist in database. Skipping recipe load.");
            return;
        }
        
        InputStream inputStream = new ClassPathResource("data/recipes.json").getInputStream();
        JsonNode recipesJson = objectMapper.readTree(inputStream);
        
        if (!recipesJson.isArray()) {
            throw new IllegalStateException("recipes.json must contain an array");
        }
        
        int totalRecipes = recipesJson.size();
        log.info("Found {} recipes to process", totalRecipes);
        
        for (JsonNode node : recipesJson) {
            try {
                Recipe recipe = processRecipe(node, ingredientMap);
                
                if (recipe != null) {
                    recipeRepository.save(recipe);
                    recipesInserted++;
                    
                    if (recipesInserted % 50 == 0) {
                        log.info("Processed {} / {} recipes", recipesInserted, totalRecipes);
                    }
                }
                
            } catch (Exception e) {
                recipesSkipped++;
                String title = node.has("title") ? node.get("title").asText() : "unknown";
                String error = String.format("Failed to process recipe '%s': %s", title, e.getMessage());
                validationErrors.add(error);
                log.error(error, e);
            }
        }
        
        recipesAfterCount = recipeRepository.count();
        log.info("Recipes loaded: {} inserted, {} skipped (total: {})", 
                 recipesInserted, recipesSkipped, recipesAfterCount);
        log.info("");
    }

    /**
     * Process and validate a single recipe from JSON (NEW FORMAT)
     */
    private Recipe processRecipe(JsonNode node, Map<String, Ingredient> ingredientMap) {
        // Validate required fields
        if (!node.has("id") || node.get("id").asText().trim().isEmpty()) {
            validationErrors.add("Recipe missing required field: id");
            recipesSkipped++;
            return null;
        }
        
        if (!node.has("title") || node.get("title").asText().trim().isEmpty()) {
            validationErrors.add("Recipe missing required field: title");
            recipesSkipped++;
            return null;
        }
        
        String stableId = node.get("id").asText();
        String title = node.get("title").asText().trim();
        
        // Extract fields with defaults
        String cuisine = node.has("cuisine") ? node.get("cuisine").asText() : "Other";
        String mealStr = node.has("meal") ? node.get("meal").asText() : "dinner";
        Integer servings = node.has("servings") ? node.get("servings").asInt() : 4;
        String summary = node.has("summary") ? node.get("summary").asText() : "";
        Integer time = node.has("time") ? node.get("time").asInt() : 30;
        String difficultyStr = node.has("difficulty_level") ? node.get("difficulty_level").asText() : "medium";
        String source = node.has("source") ? node.get("source").asText() : "food-com";
        String img = node.has("img") ? node.get("img").asText() : "/images/default-recipe.jpg";
        
        // Convert strings to enums with safe conversion
        MealType meal = MealType.fromString(mealStr);
        DifficultyLevel difficultyLevel = DifficultyLevel.fromString(difficultyStr);
        
        // Create recipe entity with enums
        Recipe recipe = new Recipe(stableId, title, cuisine, meal, servings, summary, 
                                   time, difficultyLevel, source, img);
        
        // Process dietary tags
        if (node.has("dietary_tags") && node.get("dietary_tags").isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode tagNode : node.get("dietary_tags")) {
                tags.add(tagNode.asText());
            }
            recipe.setDietaryTags(tags);
        }
        
        // Process ingredient references from ingredients array
        if (node.has("ingredients") && node.get("ingredients").isArray()) {
            List<String> missingForThisRecipe = new ArrayList<>();
            
            for (JsonNode ingredientNode : node.get("ingredients")) {
                String ingredientId = ingredientNode.has("id") ? ingredientNode.get("id").asText() : null;
                String ingredientName = ingredientNode.has("name") ? ingredientNode.get("name").asText() : null;
                Double ingredientQuantity = ingredientNode.has("quantity") ? ingredientNode.get("quantity").asDouble() : 100.0;
                
                if (ingredientId == null || ingredientId.isEmpty()) {
                    missingForThisRecipe.add("null");
                    missingIngredients.add("null");
                    continue;
                }
                
                Ingredient ingredient = ingredientMap.get(ingredientId);
                if (ingredient == null) {
                    missingForThisRecipe.add(ingredientName != null ? ingredientName : ingredientId);
                    missingIngredients.add(ingredientName != null ? ingredientName : ingredientId);
                    continue;
                }
                
                RecipeIngredient recipeIngredient = new RecipeIngredient();
                recipeIngredient.setRecipe(recipe);
                recipeIngredient.setIngredient(ingredient);
                recipeIngredient.setQuantity(ingredientQuantity.doubleValue());
                recipe.getIngredients().add(recipeIngredient);
            }
            
            // Skip recipe if any ingredients are missing
            if (!missingForThisRecipe.isEmpty()) {
                String error = String.format("Recipe '%s' missing ingredients: %s", title, String.join(", ", missingForThisRecipe));
                validationErrors.add(error);
                recipesSkipped++;
                return null;
            }
        }
        
        // Process preparation steps with new structure
        if (node.has("preparation") && node.get("preparation").isArray()) {
            int stepIndex = 1;
            for (JsonNode stepNode : node.get("preparation")) {
                String stepTitle = stepNode.has("step") ? stepNode.get("step").asText() : "Step " + stepIndex;
                String stepDescription = stepNode.has("description") ? stepNode.get("description").asText() : "";
                
                PreparationStep step = new PreparationStep();
                step.setRecipe(recipe);
                step.setOrderNumber(stepIndex);
                step.setStepTitle(stepTitle);  // Set the step name
                step.setDescription(stepDescription);
                
                // Populate ingredients list from preparation step
                if (stepNode.has("ingredients") && stepNode.get("ingredients").isArray()) {
                    List<String> stepIngredients = new ArrayList<>();
                    for (JsonNode ingNode : stepNode.get("ingredients")) {
                        stepIngredients.add(ingNode.asText());
                    }
                    step.setIngredientIds(stepIngredients);
                }
                
                recipe.getPreparationSteps().add(step);
                stepIndex++;
            }
        }
        
        return recipe;
    }

    /**
     * Print comprehensive startup report
     */
    private void printStartupReport() {
        log.info("");
        log.info("=".repeat(80));
        log.info("STARTUP REPORT - RECIPE DATA LOADER");
        log.info("=".repeat(80));
        log.info("");
        
        log.info("LOAD STATUS: {}", loadStatus);
        log.info("");
        
        log.info("INGREDIENTS:");
        log.info("  Before: {} records", ingredientsBeforeCount);
        log.info("  Inserted: {}", ingredientsInserted);
        log.info("  Skipped: {}", ingredientsSkipped);
        log.info("  After: {} records", ingredientsAfterCount);
        log.info("");
        
        log.info("RECIPES:");
        log.info("  Before: {} records", recipesBeforeCount);
        log.info("  Inserted: {}", recipesInserted);
        log.info("  Skipped: {}", recipesSkipped);
        log.info("  After: {} records", recipesAfterCount);
        log.info("");
        
        if (!missingIngredients.isEmpty()) {
            log.warn("MISSING INGREDIENT REFERENCES ({}):", missingIngredients.size());
            List<String> sortedMissing = new ArrayList<>(missingIngredients);
            sortedMissing.sort(String::compareTo);
            int count = 0;
            for (String missing : sortedMissing) {
                if (count++ < 20) {
                    log.warn("  - {}", missing);
                }
            }
            if (sortedMissing.size() > 20) {
                log.warn("  ... and {} more", sortedMissing.size() - 20);
            }
            log.warn("");
        }
        
        if (!validationErrors.isEmpty()) {
            log.warn("VALIDATION ERRORS ({}):", validationErrors.size());
            int count = 0;
            for (String error : validationErrors) {
                if (count++ < 20) {
                    log.warn("  - {}", error);
                }
            }
            if (validationErrors.size() > 20) {
                log.warn("  ... and {} more", validationErrors.size() - 20);
            }
            log.warn("");
        }
        
        log.info("=".repeat(80));
        
        // Final verdict
        if (!loadWasPerformed) {
            log.info("✓ STARTUP CHECK PASSED: Recipes already loaded ({} records)", recipesBeforeCount);
        } else if (recipesSkipped == 0 && ingredientsSkipped == 0 && validationErrors.isEmpty()) {
            log.info("✓ LOAD SUCCESS: All data loaded without errors!");
        } else {
            log.warn("⚠ LOAD COMPLETED WITH WARNINGS: Check validation errors above");
        }
        
        log.info("=".repeat(80));
        log.info("");
    }
}
