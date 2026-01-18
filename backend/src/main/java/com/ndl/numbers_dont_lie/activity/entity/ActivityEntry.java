package com.ndl.numbers_dont_lie.activity.entity;
import com.ndl.numbers_dont_lie.entity.UserEntity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "activity_entries",
       uniqueConstraints = @UniqueConstraint(name = "uk_activity_user_at", columnNames = {"user_id","at_ts"}))
public class ActivityEntry {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false)
  private UserEntity user;

  @Column(name = "at_ts", nullable = false, updatable = false)
  private Instant at;                     // момент активности (UTC)

  @Column(length = 24, nullable = false)
  private String type;                    // cardio | strength | flexibility | sports | other

  @Column(nullable = false)
  private Integer minutes;                // 5..300

  @Column(length = 16)
  private String intensity;               // low | moderate | high (optional)

  public Long getId() { return id; }
  public UserEntity getUser() { return user; }
  public void setUser(UserEntity user) { this.user = user; }
  public Instant getAt() { return at; }
  public void setAt(Instant at) { this.at = at; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Integer getMinutes() { return minutes; }
  public void setMinutes(Integer minutes) { this.minutes = minutes; }
  public String getIntensity() { return intensity; }
  public void setIntensity(String intensity) { this.intensity = intensity; }
}
