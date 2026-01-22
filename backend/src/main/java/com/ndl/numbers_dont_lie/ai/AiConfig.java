package com.ndl.numbers_dont_lie.ai;

import com.ndl.numbers_dont_lie.ai.cache.AiSessionCache;
import com.ndl.numbers_dont_lie.ai.embedding.EmbeddingService;
import com.ndl.numbers_dont_lie.ai.embedding.SimpleTfIdfEmbedding;
import com.ndl.numbers_dont_lie.ai.vector.InMemoryVectorStore;
import com.ndl.numbers_dont_lie.ai.vector.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI configuration with optional Groq integration.
 * 
 * Groq-based services (strategy, meal structure, recipe generation) are only enabled
 * when GROQ_API_KEY environment variable is set.
 * 
 * RAG components (embeddings, vector store) are always available.
 */
@Configuration
public class AiConfig {
    
    /**
     * GroqClient bean - only created if GROQ_API_KEY is present.
     * This prevents application startup failure when API key is not configured.
     */
    @Bean
    @ConditionalOnProperty(name = "groq.api.key", matchIfMissing = false)
    public GroqClient groqClient() {
        return new GroqClient();
    }

    @Bean
    public AiSessionCache aiSessionCache() {
        return new AiSessionCache();
    }

    /**
     * AiStrategyService - requires GroqClient, so also conditional.
     */
    @Bean
    @ConditionalOnProperty(name = "groq.api.key", matchIfMissing = false)
    public AiStrategyService aiStrategyService(GroqClient groqClient, AiSessionCache cache) {
        return new AiStrategyService(groqClient, cache);
    }

    /**
     * RAG components - always available (don't require external API)
     */
    @Bean
    public EmbeddingService embeddingService() {
        return new SimpleTfIdfEmbedding();
    }

    @Bean
    public VectorStore vectorStore() {
        return new InMemoryVectorStore();
    }
}
