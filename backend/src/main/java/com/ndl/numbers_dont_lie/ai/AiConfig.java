package com.ndl.numbers_dont_lie.ai;

import com.ndl.numbers_dont_lie.ai.cache.AiSessionCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {
    @Bean
    public GroqClient groqClient() {
        return new GroqClient();
    }

    @Bean
    public AiSessionCache aiSessionCache() {
        return new AiSessionCache();
    }

    @Bean
    public AiStrategyService aiStrategyService(GroqClient groqClient, AiSessionCache cache) {
        return new AiStrategyService(groqClient, cache);
    }
}
