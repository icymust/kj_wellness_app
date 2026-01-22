package com.ndl.numbers_dont_lie.ai.embedding;

/**
 * Interface for generating vector embeddings from text.
 * 
 * Implementations may use:
 * - External APIs (OpenAI, Cohere, etc.)
 * - Local models (sentence-transformers via Python bridge)
 * - Simple statistical methods (TF-IDF, word2vec)
 * 
 * For RAG pipeline: text → embedding → vector store → similarity search
 */
public interface EmbeddingService {
    /**
     * Generate a vector embedding for the given text.
     * 
     * @param text Input text to embed
     * @return Vector embedding (normalized to unit length for cosine similarity)
     */
    double[] embed(String text);

    /**
     * Get the dimensionality of embeddings produced by this service.
     */
    int getDimension();
}
