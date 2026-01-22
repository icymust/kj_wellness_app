package com.ndl.numbers_dont_lie.ai;

import com.ndl.numbers_dont_lie.ai.dto.RecipeQuery;
import com.ndl.numbers_dont_lie.ai.dto.RetrievedRecipe;
import com.ndl.numbers_dont_lie.ai.embedding.EmbeddingService;
import com.ndl.numbers_dont_lie.ai.vector.VectorStore;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG-based recipe retrieval service.
 * 
 * STEP 4.3.1: Retrieval-Augmented Generation Infrastructure
 * 
 * Pipeline:
 * 1. Database → Embeddings (via RecipeEmbeddingService)
 * 2. Query → Embedding (via EmbeddingService)
 * 3. Vector Search → Top-N similar recipes (via VectorStore)
 * 4. Retrieval → Recipe metadata (via RecipeRepository)
 * 5. [Future] Augmentation → Context for AI generation (STEP 4.3.2)
 * 
 * This service handles stages 2-4 (query embedding, search, retrieval).
 * Does NOT generate recipes or calculate nutrition.
 */
@Service
public class RecipeRetrievalService {
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final RecipeRepository recipeRepository;

    public RecipeRetrievalService(
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            RecipeRepository recipeRepository) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.recipeRepository = recipeRepository;
    }

    /**
     * Retrieve top-N most relevant recipes for a structured query.
     * 
     * Process:
     * 1. Convert query to search text
     * 2. Generate embedding for query
     * 3. Search vector store for similar embeddings
     * 4. Fetch recipe metadata
     * 5. Return scored results
     * 
     * @param query Structured recipe query (cuisine, dietary restrictions, etc.)
     * @param topN Number of results to return
     * @return List of retrieved recipes with relevance scores
     */
    public List<RetrievedRecipe> retrieve(RecipeQuery query, int topN) {
        // Step 1: Convert query to searchable text
        String queryText = buildQueryText(query);

        // Step 2: Generate embedding
        double[] queryEmbedding = embeddingService.embed(queryText);

        // Step 3: Vector similarity search
        List<VectorStore.SearchResult> searchResults = vectorStore.search(queryEmbedding, topN);

        // Step 4: Fetch recipe metadata
        List<Long> recipeIds = searchResults.stream()
                .map(VectorStore.SearchResult::getRecipeId)
                .collect(Collectors.toList());

        List<Recipe> recipes = recipeRepository.findAllById(recipeIds);

        // Step 5: Build result DTOs with scores
        List<RetrievedRecipe> results = new ArrayList<>();
        for (VectorStore.SearchResult sr : searchResults) {
            Recipe recipe = recipes.stream()
                    .filter(r -> r.getId().equals(sr.getRecipeId()))
                    .findFirst()
                    .orElse(null);

            if (recipe != null) {
                results.add(new RetrievedRecipe(
                        recipe.getId(),
                        recipe.getTitle(),
                        recipe.getCuisine(),
                        sr.getScore()
                ));
            }
        }

        return results;
    }

    /**
     * Build searchable text from structured query fields.
     * Weight fields by repetition (similar to recipe embedding).
     */
    private String buildQueryText(RecipeQuery query) {
        StringBuilder sb = new StringBuilder();

        // Cuisine preferences (repeated 2x for weight)
        if (query.getCuisinePreferences() != null && !query.getCuisinePreferences().isEmpty()) {
            String cuisines = String.join(" ", query.getCuisinePreferences());
            sb.append(cuisines).append(" ");
            sb.append(cuisines).append(" ");
        }

        // Dietary restrictions (repeated 2x)
        if (query.getDietaryRestrictions() != null && !query.getDietaryRestrictions().isEmpty()) {
            String restrictions = String.join(" ", query.getDietaryRestrictions());
            sb.append(restrictions).append(" ");
            sb.append(restrictions).append(" ");
        }

        // Meal type
        if (query.getMealType() != null) {
            sb.append(query.getMealType()).append(" ");
        }

        // Macro focus (convert to descriptive terms)
        if (query.getMacroFocus() != null) {
            query.getMacroFocus().forEach((macro, weight) -> {
                if (weight > 0.3) { // High focus on this macro
                    sb.append(macro).append(" ");
                    sb.append("high_").append(macro).append(" ");
                }
            });
        }

        // Free text query
        if (query.getFreeTextQuery() != null && !query.getFreeTextQuery().isBlank()) {
            sb.append(query.getFreeTextQuery());
        }

        return sb.toString();
    }
}
