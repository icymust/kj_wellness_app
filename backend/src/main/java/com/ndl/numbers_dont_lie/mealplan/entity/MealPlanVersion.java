package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an immutable version of a meal plan.
 * 
 * Design intent:
 * - Each modification to a meal plan creates a new version
 * - Versions are never updated after creation (immutable)
 * - Enables version history and rollback capabilities
 * - Parent MealPlan tracks the current active version
 */
@Entity
@Table(name = "meal_plan_versions")
public class MealPlanVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_id", nullable = false)
    private MealPlan mealPlan;

    @Column(name = "version_number", nullable = false)
    @JsonProperty("version_number")
    private Integer versionNumber;

    @Column(name = "created_at", nullable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Column(length = 32, nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonProperty("reason")
    private VersionReason reason;

    @OneToMany(mappedBy = "mealPlanVersion", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("day_plans")
    private List<DayPlan> dayPlans = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public MealPlanVersion() {
    }

    public MealPlanVersion(MealPlan mealPlan, Integer versionNumber, VersionReason reason) {
        this.mealPlan = mealPlan;
        this.versionNumber = versionNumber;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public MealPlan getMealPlan() {
        return mealPlan;
    }

    public void setMealPlan(MealPlan mealPlan) {
        this.mealPlan = mealPlan;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public VersionReason getReason() {
        return reason;
    }

    public List<DayPlan> getDayPlans() {
        return dayPlans;
    }

    public void setDayPlans(List<DayPlan> dayPlans) {
        this.dayPlans = dayPlans;
    }

    /**
     * Add a day plan to this version.
     * Helper method to maintain bidirectional relationship.
     */
    public void addDayPlan(DayPlan dayPlan) {
        dayPlans.add(dayPlan);
        dayPlan.setMealPlanVersion(this);
    }

    public void setReason(VersionReason reason) {
        this.reason = reason;
    }
}
