package com.ndl.numbers_dont_lie.dto;

import java.util.List;
import java.util.Map;

public class AiInsightsResponse {
  public List<AiInsightItemDto> items;
  public Map<String, Integer> summary; // by priority
  public boolean fromCache;
  public String generatedAt; // ISO string

  public AiInsightsResponse() {}
  public AiInsightsResponse(List<AiInsightItemDto> items, Map<String,Integer> summary, boolean fromCache, String generatedAt) {
    this.items = items; this.summary = summary; this.fromCache = fromCache; this.generatedAt = generatedAt;
  }
}
