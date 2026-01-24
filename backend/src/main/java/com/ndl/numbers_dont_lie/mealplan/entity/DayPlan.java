package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single calendar day within a meal plan version.
 * 
 * Design intent:
 * - Each DayPlan corresponds to exactly one calendar date (ISO 8601)
 * - Contains all meals for that specific day
 * - Number of meals per day is dynamic, based on user profile meal frequency
 * - Meals are created via factory methods or services that inject meal frequency
 * - Date is stored without timezone (interpreted using parent MealPlan's timezone)
 */
@Entity
@Table(name = "day_plans")
public class DayPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_plan_version_id", nullable = false)
    @JsonBackReference("version-dayplans")
    private MealPlanVersion mealPlanVersion;

    @Column(name = "plan_date", nullable = false)
    @JsonProperty("date")
    private LocalDate date; // ISO 8601 date (timezone-agnostic, interpreted via parent)

    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId; // Owner of this day plan

    @Column(name = "context_hash", length = 64)
    @JsonProperty("context_hash")
    private String contextHash; // SHA-256 hash of user preferences and meal structure

    @OneToMany(mappedBy = "dayPlan", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("meals")
    @JsonManagedReference("dayplan-meals")
    private List<Meal> meals = new ArrayList<>();

    public DayPlan() {
    }

    public DayPlan(MealPlanVersion mealPlanVersion, LocalDate date) {
        this.mealPlanVersion = mealPlanVersion;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public MealPlanVersion getMealPlanVersion() {
        return mealPlanVersion;
    }

    public void setMealPlanVersion(MealPlanVersion mealPlanVersion) {
        this.mealPlanVersion = mealPlanVersion;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContextHash() {
        return contextHash;
    }

    public void setContextHash(String contextHash) {
        this.contextHash = contextHash;
    }

    public List<Meal> getMeals() {
        return meals;
    }

    public void setMeals(List<Meal> meals) {
        this.meals = meals;
    }

    /**
     * Add a meal to this day plan.
     * Helper method to maintain bidirectional relationship.
     */
    public void addMeal(Meal meal) {
        meals.add(meal);
        meal.setDayPlan(this);
    }
}
