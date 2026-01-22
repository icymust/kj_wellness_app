package com.ndl.numbers_dont_lie.ai.embedding;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple TF-IDF based embedding service for recipe retrieval.
 * 
 * This is a lightweight implementation suitable for MVP/demo purposes.
 * For production, consider:
 * - OpenAI text-embedding-3-small (1536 dims, $0.02/1M tokens)
 * - Cohere embed-english-v3.0 (1024 dims)
 * - Local sentence-transformers (all-MiniLM-L6-v2, 384 dims)
 * 
 * Current approach:
 * - Tokenizes text into words
 * - Creates sparse vector with word frequencies
 * - Normalizes to unit length for cosine similarity
 * - Fixed dimension (128) with hashing to map words to indices
 */
public class SimpleTfIdfEmbedding implements EmbeddingService {
    private static final int DIMENSION = 128;

    @Override
    public double[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new double[DIMENSION];
        }

        // Tokenize and count word frequencies
        String[] words = text.toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .split("\\s+");

        Map<Integer, Double> sparseVector = new HashMap<>();
        for (String word : words) {
            if (word.length() < 2) continue; // Skip very short words
            int index = Math.abs(word.hashCode() % DIMENSION);
            sparseVector.merge(index, 1.0, Double::sum);
        }

        // Convert to dense vector
        double[] dense = new double[DIMENSION];
        sparseVector.forEach((idx, val) -> dense[idx] = val);

        // Normalize to unit length for cosine similarity
        double norm = 0.0;
        for (double v : dense) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm > 0) {
            for (int i = 0; i < dense.length; i++) {
                dense[i] /= norm;
            }
        }

        return dense;
    }

    @Override
    public int getDimension() {
        return DIMENSION;
    }
}
