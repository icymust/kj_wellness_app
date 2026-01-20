package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Root aggregate for meal planning functionality.
 * 
 * Design intent:
 * - Acts as logical container for meal plan lifecycle
 * - Does NOT contain meals directly (meals live in versions)
 * - Tracks current active version via currentVersion reference
 * - Maintains full version history via versions collection
 * - User reference stored as userId (no entity relationship needed yet)
 */
@Entity
@Table(name = "meal_plans")
public class MealPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId; // Reference to user without entity relationship

    @Column(length = 16, nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonProperty("duration")
    private PlanDuration duration;

    @Column(length = 64, nullable = false)
    @JsonProperty("timezone")
    private String timezone; // IANA timezone string (e.g., "Europe/Tallinn")

    @Column(name = "created_at", nullable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    @JsonProperty("current_version")
    private MealPlanVersion currentVersion; // Points to the active version

    @OneToMany(mappedBy = "mealPlan", fetch = FetchType.LAZY)
    private List<MealPlanVersion> versions = new ArrayList<>(); // Full version history

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public MealPlan() {
    }

    public MealPlan(Long userId, PlanDuration duration, String timezone) {
        this.userId = userId;
        this.duration = duration;
        this.timezone = timezone;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PlanDuration getDuration() {
        return duration;
    }

    public void setDuration(PlanDuration duration) {
        this.duration = duration;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public MealPlanVersion getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(MealPlanVersion currentVersion) {
        this.currentVersion = currentVersion;
    }

    public List<MealPlanVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<MealPlanVersion> versions) {
        this.versions = versions;
    }
}
