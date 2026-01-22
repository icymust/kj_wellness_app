package com.ndl.numbers_dont_lie.ai.vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory vector store using cosine similarity.
 * 
 * Suitable for:
 * - MVP/demo with moderate recipe count (<10K)
 * - Development and testing
 * 
 * For production at scale, consider:
 * - pgvector extension for PostgreSQL
 * - Pinecone, Weaviate, or Milvus
 * - FAISS for local high-performance search
 * 
 * Cosine similarity formula:
 * similarity = (A · B) / (||A|| × ||B||)
 * Since embeddings are pre-normalized, this simplifies to dot product.
 */
public class InMemoryVectorStore implements VectorStore {
    private final Map<Long, double[]> embeddings = new ConcurrentHashMap<>();

    @Override
    public void store(Long recipeId, double[] embedding) {
        if (recipeId == null || embedding == null) {
            throw new IllegalArgumentException("recipeId and embedding must not be null");
        }
        embeddings.put(recipeId, embedding);
    }

    @Override
    public List<SearchResult> search(double[] queryEmbedding, int topN) {
        if (queryEmbedding == null) {
            throw new IllegalArgumentException("queryEmbedding must not be null");
        }
        if (topN <= 0) {
            return List.of();
        }

        List<SearchResult> results = new ArrayList<>();
        
        for (Map.Entry<Long, double[]> entry : embeddings.entrySet()) {
            double similarity = cosineSimilarity(queryEmbedding, entry.getValue());
            results.add(new SearchResult(entry.getKey(), similarity));
        }

        // Sort by descending score and take top N
        results.sort(Comparator.comparingDouble(SearchResult::getScore).reversed());
        
        return results.subList(0, Math.min(topN, results.size()));
    }

    @Override
    public int size() {
        return embeddings.size();
    }

    @Override
    public void clear() {
        embeddings.clear();
    }

    /**
     * Compute cosine similarity between two vectors.
     * Assumes vectors are already normalized to unit length.
     * Returns dot product (equivalent to cosine for normalized vectors).
     */
    private double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same dimension");
        }
        
        double dotProduct = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
        }
        
        return dotProduct;
    }
}
