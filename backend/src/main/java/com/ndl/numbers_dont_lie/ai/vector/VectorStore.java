package com.ndl.numbers_dont_lie.ai.vector;

import java.util.List;

/**
 * Vector store abstraction for recipe embeddings.
 * 
 * Supports:
 * - Storage: recipeId → embedding
 * - Similarity search: query embedding → top-N similar recipes
 * 
 * RAG Pipeline Stage 2: Embeddings → Retrieval
 */
public interface VectorStore {
    
    /**
     * Store an embedding for a recipe.
     * 
     * @param recipeId Recipe identifier
     * @param embedding Vector embedding (normalized)
     */
    void store(Long recipeId, double[] embedding);

    /**
     * Search for most similar recipes using cosine similarity.
     * 
     * @param queryEmbedding Query vector (should be normalized)
     * @param topN Number of results to return
     * @return List of (recipeId, similarity score) pairs, ordered by descending score
     */
    List<SearchResult> search(double[] queryEmbedding, int topN);

    /**
     * Get total number of stored embeddings.
     */
    int size();

    /**
     * Clear all stored embeddings.
     */
    void clear();

    /**
     * Search result pairing recipe ID with similarity score.
     */
    class SearchResult {
        private final Long recipeId;
        private final double score;

        public SearchResult(Long recipeId, double score) {
            this.recipeId = recipeId;
            this.score = score;
        }

        public Long getRecipeId() { return recipeId; }
        public double getScore() { return score; }
    }
}
