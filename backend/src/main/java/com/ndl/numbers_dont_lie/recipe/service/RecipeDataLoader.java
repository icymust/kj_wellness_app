package com.ndl.numbers_dont_lie.recipe.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
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
 * 
 * On application startup, this component:
 * 1. Checks if data already exists in the database
 * 2. Only loads JSON data if the database is empty (recipes count == 0)
 * 3. Ensures idempotency - safe to restart application without duplicating data
 * 4. Validates all data before persistence
 * 5. Generates startup report with statistics
 * 
 * This approach ensures:
 * - Data is loaded once on first startup
 * - Subsequent restarts skip loading (data already present)
 * - No duplicate records
 * - Clean transaction handling
 * - Production-safe execution
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
                log.info("No recipes found. Loading ingredients and recipes...");
                log.info("");
                
                // Load ingredients first (required for foreign key references)
                Map<String, Ingredient> ingredientMap = loadIngredients();
                
                // Load recipes with ingredient references
                loadRecipes(ingredientMap);
                
                loadWasPerformed = true;
                loadStatus = "LOADED";
            } else {
                log.info("Recipes already present. Skipping data load.");
                loadWasPerformed = false;
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
     * Returns a map of normalized label -> Ingredient for recipe processing
     * 
     * IMPORTANT: Only loads if ingredient table is empty
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
                    // Check for duplicates
                    if (ingredientMap.containsKey(ingredient.getLabel())) {
                        log.warn("Duplicate ingredient found: '{}' - skipping", ingredient.getLabel());
                        ingredientsSkipped++;
                        continue;
                    }
                    
                    // Save to database
                    Ingredient saved = ingredientRepository.save(ingredient);
                    ingredientMap.put(saved.getLabel(), saved);
                    ingredientsInserted++;
                    
                    if (ingredientsInserted % 100 == 0) {
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
            ingredientMap.put(ingredient.getLabel(), ingredient)
        );
        return ingredientMap;
    }

    /**
     * Process and validate a single ingredient from JSON
     */
    private Ingredient processIngredient(JsonNode node) {
        // Validate required field: label
        if (!node.has("label") || node.get("label").asText().trim().isEmpty()) {
            validationErrors.add("Ingredient missing required field: label");
            ingredientsSkipped++;
            return null;
        }
        
        // Normalize label to lowercase and trim
        String originalLabel = node.get("label").asText();
        String normalizedLabel = originalLabel.toLowerCase().trim();
        
        // Extract fields with defaults
        String unit = node.has("unit") ? node.get("unit").asText() : "gram";
        Double quantityPer100 = node.has("quantityPer100") ? node.get("quantityPer100").asDouble() : 100.0;
        Double calories = node.has("calories") ? node.get("calories").asDouble() : 0.0;
        Double protein = node.has("protein") ? node.get("protein").asDouble() : 0.0;
        Double carbs = node.has("carbs") ? node.get("carbs").asDouble() : 0.0;
        Double fats = node.has("fats") ? node.get("fats").asDouble() : 0.0;
        
        // Validate numeric fields are non-negative
        if (quantityPer100 < 0 || calories < 0 || protein < 0 || carbs < 0 || fats < 0) {
            String error = String.format("Ingredient '%s' has negative nutrition values", normalizedLabel);
            validationErrors.add(error);
            ingredientsSkipped++;
            return null;
        }
        
        return new Ingredient(normalizedLabel, unit, quantityPer100, calories, protein, carbs, fats);
    }

    /**
     * Load and persist recipes from recipes.json
     * 
     * IMPORTANT: Only loads if recipe table is empty
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
     * Process and validate a single recipe from JSON
     */
    private Recipe processRecipe(JsonNode node, Map<String, Ingredient> ingredientMap) {
        // Validate required field: title
        if (!node.has("title") || node.get("title").asText().trim().isEmpty()) {
            validationErrors.add("Recipe missing required field: title");
            recipesSkipped++;
            return null;
        }
        
        String title = node.get("title").asText().trim();
        
        // Extract fields with defaults
        String cuisine = node.has("cuisine") ? node.get("cuisine").asText() : "Other";
        String meal = node.has("meal") ? node.get("meal").asText() : "lunch";
        Integer servings = node.has("servings") ? node.get("servings").asInt() : 4;
        String summary = node.has("summary") ? node.get("summary").asText() : "";
        Integer timeMinutes = node.has("timeMinutes") ? node.get("timeMinutes").asInt() : 30;
        String difficultyLevel = node.has("difficultyLevel") ? node.get("difficultyLevel").asText() : "medium";
        String source = node.has("source") ? node.get("source").asText() : "food-com";
        String imageUrl = node.has("imageUrl") ? node.get("imageUrl").asText() : "/images/default-recipe.jpg";
        
        // Create recipe entity
        Recipe recipe = new Recipe(title, cuisine, meal, servings, summary, 
                                   timeMinutes, difficultyLevel, source, imageUrl);
        
        // Process dietary tags
        if (node.has("dietaryTags") && node.get("dietaryTags").isArray()) {
            List<String> tags = new ArrayList<>();
            for (JsonNode tagNode : node.get("dietaryTags")) {
                tags.add(tagNode.asText());
            }
            recipe.setDietaryTags(tags);
        }
        
        // Process ingredient references
        if (node.has("ingredientNames") && node.get("ingredientNames").isArray()) {
            List<String> missingForThisRecipe = new ArrayList<>();
            
            for (JsonNode ingredientNameNode : node.get("ingredientNames")) {
                String ingredientName = ingredientNameNode.asText().toLowerCase().trim();
                
                Ingredient ingredient = ingredientMap.get(ingredientName);
                if (ingredient == null) {
                    missingForThisRecipe.add(ingredientName);
                    missingIngredients.add(ingredientName);
                } else {
                    // Create recipe-ingredient junction with default quantity (100g)
                    RecipeIngredient recipeIngredient = new RecipeIngredient(recipe, ingredient, 100.0);
                    recipe.getIngredients().add(recipeIngredient);
                }
            }
            
            // Skip recipe if any ingredients are missing
            if (!missingForThisRecipe.isEmpty()) {
                String error = String.format("Recipe '%s' references missing ingredients: %s", 
                                            title, String.join(", ", missingForThisRecipe));
                validationErrors.add(error);
                log.warn(error);
                recipesSkipped++;
                return null;
            }
        }
        
        // Process preparation steps
        if (node.has("steps") && node.get("steps").isArray()) {
            int stepIndex = 1;
            for (JsonNode stepNode : node.get("steps")) {
                String stepDescription = stepNode.asText();
                PreparationStep step = new PreparationStep(recipe, stepIndex, null, stepDescription);
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
                log.warn("  - {}", missing);
                if (++count >= 20) {
                    log.warn("  ... and {} more", sortedMissing.size() - 20);
                    break;
                }
            }
            log.warn("");
        }
        
        if (!validationErrors.isEmpty()) {
            log.warn("VALIDATION ERRORS ({}):", validationErrors.size());
            int count = 0;
            for (String error : validationErrors) {
                log.warn("  - {}", error);
                if (++count >= 20) {
                    log.warn("  ... and {} more", validationErrors.size() - 20);
                    break;
                }
            }
            log.warn("");
        }
        
        log.info("=".repeat(80));
        
        // Final verdict
        if (!loadWasPerformed) {
            log.info("✓ STARTUP CHECK PASSED: Recipes already loaded ({} records)", recipesBeforeCount);
        } else if (recipesSkipped == 0 && ingredientsSkipped == 0 && validationErrors.isEmpty()) {
            log.info("✓ DATA LOADED SUCCESSFULLY");
        } else if (recipesInserted > 0 && ingredientsInserted > 0) {
            log.warn("⚠ DATA LOADED WITH WARNINGS");
        } else {
            log.error("✗ DATA LOADING FAILED");
        }
        
        log.info("=".repeat(80));
        log.info("");
    }
}
