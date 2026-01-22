package com.ndl.numbers_dont_lie.ai.cache;

import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureResult;
import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for AI session data across multiple prompt steps.
 * Supports sequential prompting: STEP 4.1 → STEP 4.2 → STEP 4.3.
 * Not for persistence.
 */
public class AiSessionCache {
    
    public static class SessionData {
        public AiStrategyResult strategyResult; // STEP 4.1
        public AiMealStructureResult mealStructureResult; // STEP 4.2
        public final Instant createdAt = Instant.now();
    }

    private final Map<String, SessionData> byUser = new ConcurrentHashMap<>();

    // STEP 4.1: Strategy result
    public void putStrategyResult(String userId, AiStrategyResult result) {
        if (userId == null || result == null) return;
        byUser.computeIfAbsent(userId, k -> new SessionData()).strategyResult = result;
    }

    public AiStrategyResult getStrategyResult(String userId) {
        SessionData data = byUser.get(userId);
        return data == null ? null : data.strategyResult;
    }

    // STEP 4.2: Meal structure result
    public void putMealStructureResult(String userId, AiMealStructureResult result) {
        if (userId == null || result == null) return;
        byUser.computeIfAbsent(userId, k -> new SessionData()).mealStructureResult = result;
    }

    public AiMealStructureResult getMealStructureResult(String userId) {
        SessionData data = byUser.get(userId);
        return data == null ? null : data.mealStructureResult;
    }

    // Session management
    public SessionData getSession(String userId) {
        return byUser.get(userId);
    }

    public void clear(String userId) {
        byUser.remove(userId);
    }
}
