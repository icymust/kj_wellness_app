package com.ndl.numbers_dont_lie.ai.cache;

import com.ndl.numbers_dont_lie.ai.dto.AiStrategyResult;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache for AI strategy session data. Not for persistence.
 */
public class AiSessionCache {
    public static class Entry {
        public final AiStrategyResult result;
        public final Instant createdAt = Instant.now();
        public Entry(AiStrategyResult result) { this.result = result; }
    }

    private final Map<String, Entry> byUser = new ConcurrentHashMap<>();

    public void put(String userId, AiStrategyResult result) {
        if (userId == null || result == null) return;
        byUser.put(userId, new Entry(result));
    }

    public AiStrategyResult get(String userId) {
        Entry e = byUser.get(userId);
        return e == null ? null : e.result;
    }

    public void clear(String userId) {
        byUser.remove(userId);
    }
}
