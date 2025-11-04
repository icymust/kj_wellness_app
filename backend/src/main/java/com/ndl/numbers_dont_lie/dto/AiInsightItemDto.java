package com.ndl.numbers_dont_lie.dto;

import java.util.List;

public class AiInsightItemDto {
  public String title;
  public String detail;
  public String priority; // high | medium | low
  public List<String> tags;

  public AiInsightItemDto() {}
  public AiInsightItemDto(String title, String detail, String priority) {
    this.title = title; this.detail = detail; this.priority = priority;
  }
  public AiInsightItemDto(String title, String detail, String priority, List<String> tags) {
    this.title = title; this.detail = detail; this.priority = priority; this.tags = tags;
  }
}
