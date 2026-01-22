package com.ndl.numbers_dont_lie.ai.embedding;

import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

/**
 * Service to generate and store recipe embeddings for RAG retrieval.
 * 
 * Embeddings are created from recipe text fields:
 * - title (high weight)
 * - cuisine
 * - dietary_tags
 * - summary
 * 
 * The embedding is stored in the recipe's `embedding` field for persistence.
 * This enables fast similarity search via vector operations.
 * 
 * RAG Pipeline Stage 1: Database → Embeddings
 */
@Service
public class RecipeEmbeddingService {
    private final EmbeddingService embeddingService;
    private final RecipeRepository recipeRepository;

    public RecipeEmbeddingService(EmbeddingService embeddingService, RecipeRepository recipeRepository) {
        this.embeddingService = embeddingService;
        this.recipeRepository = recipeRepository;
    }

    /**
     * Generate embedding for a single recipe from its text fields.
     * Does NOT persist to database.
     */
    public double[] generateEmbedding(Recipe recipe) {
        String text = buildRecipeText(recipe);
        return embeddingService.embed(text);
    }

    /**
     * Generate and persist embedding for a single recipe.
     */
    @Transactional
    public void embedAndSave(Recipe recipe) {
        double[] embedding = generateEmbedding(recipe);
        recipe.setEmbedding(toFloatArray(embedding));
        recipeRepository.save(recipe);
    }

    /**
     * Bulk embed all recipes that don't have embeddings yet.
     * This is typically run once during setup or after data load.
     */
    @Transactional
    public int embedAllRecipes() {
        var recipes = recipeRepository.findAll();
        int count = 0;
        for (Recipe recipe : recipes) {
            if (recipe.getEmbedding() == null || recipe.getEmbedding().length == 0) {
                embedAndSave(recipe);
                count++;
            }
        }
        return count;
    }

    /**
     * Build searchable text from recipe fields.
     * Weight important fields by repetition.
     */
    private String buildRecipeText(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        
        // Title (repeated 3x for higher weight)
        if (recipe.getTitle() != null) {
            sb.append(recipe.getTitle()).append(" ");
            sb.append(recipe.getTitle()).append(" ");
            sb.append(recipe.getTitle()).append(" ");
        }
        
        // Cuisine (repeated 2x)
        if (recipe.getCuisine() != null) {
            sb.append(recipe.getCuisine()).append(" ");
            sb.append(recipe.getCuisine()).append(" ");
        }
        
        // Dietary tags
        if (recipe.getDietaryTags() != null && !recipe.getDietaryTags().isEmpty()) {
            String tags = recipe.getDietaryTags().stream()
                    .collect(Collectors.joining(" "));
            sb.append(tags).append(" ");
        }
        
        // Summary
        if (recipe.getSummary() != null) {
            sb.append(recipe.getSummary());
        }
        
        return sb.toString();
    }

    private float[] toFloatArray(double[] doubles) {
        float[] floats = new float[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            floats[i] = (float) doubles[i];
        }
        return floats;
    }

    private double[] toDoubleArray(float[] floats) {
        double[] doubles = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
            doubles[i] = floats[i];
        }
        return doubles;
    }
}
