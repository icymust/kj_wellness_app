package com.ndl.numbers_dont_lie.mealplan.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Represents a single meal slot within a day plan.
 * 
 * Design intent:
 * - Meal is NOT a Recipe - it's a slot that may reference a recipe
 * - Multiple meals of same type (e.g., multiple snacks) are handled via index
 * - recipeId is nullable to support custom meals or future meal generation
 * - plannedTime is timezone-aware (stored as LocalDateTime, interpreted in DayPlan's timezone)
 * - Meal frequency is determined dynamically from user profile, not hardcoded
 */
@Entity
@Table(name = "meals")
public class Meal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_plan_id", nullable = false)
    @JsonBackReference("dayplan-meals")
    private DayPlan dayPlan;

    @Column(name = "meal_type", length = 16, nullable = false)
    @Enumerated(EnumType.STRING)
    @JsonProperty("meal_type")
    private MealType mealType;

    @Column(name = "meal_index", nullable = false)
    @JsonProperty("index")
    private Integer index; // For multiple meals of same type (e.g., snack 1, snack 2)

    @Column(name = "planned_time", nullable = false)
    @JsonProperty("planned_time")
    private LocalDateTime plannedTime; // Timezone-aware, interpreted using parent timezone

    @Column(name = "recipe_id", length = 16)
    @JsonProperty("recipe_id")
    private String recipeId; // Nullable - reference to Recipe stable ID

    @Column(name = "custom_meal_name", length = 256)
    @JsonProperty("custom_meal_name")
    private String customMealName; // Nullable - for meals without recipes

    @Column(name = "is_custom", nullable = false)
    @JsonProperty("is_custom")
    private Boolean isCustom = false; // Flag: true if this is a custom meal added by user

    @Column(name = "calorie_target", nullable = true)
    @JsonProperty("calorie_target")
    private Integer calorieTarget; // Target calories for this meal slot (used for nutrition summary)

    @Column(name = "planned_calories", nullable = true)
    @JsonProperty("planned_calories")
    private Integer plannedCalories; // Actual calories for this meal (sum of planned calories = daily total)

    public Meal() {
    }

    public Meal(DayPlan dayPlan, MealType mealType, Integer index, LocalDateTime plannedTime) {
        this.dayPlan = dayPlan;
        this.mealType = mealType;
        this.index = index;
        this.plannedTime = plannedTime;
    }

    public Long getId() {
        return id;
    }

    public DayPlan getDayPlan() {
        return dayPlan;
    }

    public void setDayPlan(DayPlan dayPlan) {
        this.dayPlan = dayPlan;
    }

    public MealType getMealType() {
        return mealType;
    }

    public void setMealType(MealType mealType) {
        this.mealType = mealType;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public LocalDateTime getPlannedTime() {
        return plannedTime;
    }

    public void setPlannedTime(LocalDateTime plannedTime) {
        this.plannedTime = plannedTime;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getCustomMealName() {
        return customMealName;
    }

    public void setCustomMealName(String customMealName) {
        this.customMealName = customMealName;
    }

    public Boolean getIsCustom() {
        return isCustom != null ? isCustom : false;
    }

    public void setIsCustom(Boolean isCustom) {
        this.isCustom = isCustom != null ? isCustom : false;
    }

    public Integer getCalorieTarget() {
        return calorieTarget;
    }

    public void setCalorieTarget(Integer calorieTarget) {
        this.calorieTarget = calorieTarget;
    }

    public Integer getPlannedCalories() {
        return plannedCalories;
    }

    public void setPlannedCalories(Integer plannedCalories) {
        this.plannedCalories = plannedCalories;
    }
}
