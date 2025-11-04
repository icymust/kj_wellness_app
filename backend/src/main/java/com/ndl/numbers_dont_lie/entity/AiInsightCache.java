package com.ndl.numbers_dont_lie.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ai_insight_cache", indexes = {
  @Index(name = "idx_ai_cache_user_scope_goal", columnList = "user_id,scope,goal_key")
})
public class AiInsightCache {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(nullable = false, length = 32)
  private String scope; // e.g. weekly | monthly

  @Column(name = "goal_key", nullable = false, length = 64)
  private String goalKey = "default";

  @Column(name = "generated_at", nullable = false)
  private Instant generatedAt = Instant.now();

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Lob
  @Column(name = "data_json", nullable = false)
  private String dataJson;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }
  public String getScope() { return scope; }
  public void setScope(String scope) { this.scope = scope; }
  public String getGoalKey() { return goalKey; }
  public void setGoalKey(String goalKey) { this.goalKey = goalKey; }
  public Instant getGeneratedAt() { return generatedAt; }
  public void setGeneratedAt(Instant generatedAt) { this.generatedAt = generatedAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
  public String getDataJson() { return dataJson; }
  public void setDataJson(String dataJson) { this.dataJson = dataJson; }
}
