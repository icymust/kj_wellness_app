package com.ndl.numbers_dont_lie.ai.dto;

import java.util.List;

public class AiInsightItemDto {
  // Legacy fields used by existing UI
  public String title;
  public String detail;
  public String priority; // legacy: "high" | "medium" | "low"
  public List<String> tags;

  // New fields required by tests/spec
  public String type;            // "GOAL" | "ACTIVITY" | "PROGRESS"
  public String priorityLevel;   // "HIGH" | "MEDIUM" | "LOW"
  public String message;         // human-readable recommendation
  public String createdAt;       // ISO timestamp
  public Boolean cached;         // per-item cached flag (false when newly generated)

  public AiInsightItemDto() {}
  public AiInsightItemDto(String title, String detail, String priority) {
    this.title = title; this.detail = detail; this.priority = priority;
  }
  public AiInsightItemDto(String title, String detail, String priority, List<String> tags) {
    this.title = title; this.detail = detail; this.priority = priority; this.tags = tags;
  }
}
