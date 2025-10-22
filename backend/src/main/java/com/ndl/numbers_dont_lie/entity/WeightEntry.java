package com.ndl.numbers_dont_lie.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "weight_entries",
       uniqueConstraints = @UniqueConstraint(name = "uk_weight_user_at", columnNames = {"user_id","at_ts"}))
public class WeightEntry {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "at_ts", nullable = false, updatable = false)
  private Instant at;               // уникально вместе с user

  @Column(nullable = false)
  private Double weightKg;          // 20..500

  public Long getId() { return id; }
  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }
  public Instant getAt() { return at; }
  public void setAt(Instant at) { this.at = at; }
  public Double getWeightKg() { return weightKg; }
  public void setWeightKg(Double weightKg) { this.weightKg = weightKg; }
}
