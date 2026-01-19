package com.ndl.numbers_dont_lie.entity.nutrition;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "nutritional_preferences", uniqueConstraints = {
        @UniqueConstraint(name = "uk_nutritional_prefs_user", columnNames = "user_id")
})
public class NutritionalPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "dietary_preferences",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "preference")
    private Set<String> dietaryPreferences = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "allergies",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "allergen")
    private Set<String> allergies = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "disliked_ingredients",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "ingredient")
    private List<String> dislikedIngredients = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "cuisine_preferences",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "cuisine")
    private List<String> cuisinePreferences = new ArrayList<>();

    @Column(name = "calorie_target")
    private Integer calorieTarget;

    @Column(name = "protein_target")
    private Integer proteinTarget;

    @Column(name = "carbs_target")
    private Integer carbsTarget;

    @Column(name = "fats_target")
    private Integer fatsTarget;

    @Column(name = "breakfast_count")
    private Integer breakfastCount;

    @Column(name = "lunch_count")
    private Integer lunchCount;

    @Column(name = "dinner_count")
    private Integer dinnerCount;

    @Column(name = "snack_count")
    private Integer snackCount;

    @Column(name = "breakfast_time", length = 5)
    private String breakfastTime;

    @Column(name = "lunch_time", length = 5)
    private String lunchTime;

    @Column(name = "dinner_time", length = 5)
    private String dinnerTime;

    @Column(name = "snack_time", length = 5)
    private String snackTime;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public NutritionalPreferences() {
        this.updatedAt = LocalDateTime.now();
    }

    public NutritionalPreferences(Long userId) {
        this.userId = userId;
        this.updatedAt = LocalDateTime.now();
        this.dietaryPreferences = new HashSet<>();
        this.allergies = new HashSet<>();
        this.dislikedIngredients = new ArrayList<>();
        this.cuisinePreferences = new ArrayList<>();
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Set<String> getDietaryPreferences() { return dietaryPreferences; }
    public void setDietaryPreferences(Set<String> dietaryPreferences) {
        this.dietaryPreferences = dietaryPreferences != null ? dietaryPreferences : new HashSet<>();
        this.updatedAt = LocalDateTime.now();
    }

    public Set<String> getAllergies() { return allergies; }
    public void setAllergies(Set<String> allergies) {
        this.allergies = allergies != null ? allergies : new HashSet<>();
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getDislikedIngredients() { return dislikedIngredients; }
    public void setDislikedIngredients(List<String> dislikedIngredients) {
        this.dislikedIngredients = dislikedIngredients != null ? dislikedIngredients : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> getCuisinePreferences() { return cuisinePreferences; }
    public void setCuisinePreferences(List<String> cuisinePreferences) {
        this.cuisinePreferences = cuisinePreferences != null ? cuisinePreferences : new ArrayList<>();
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getCalorieTarget() { return calorieTarget; }
    public void setCalorieTarget(Integer calorieTarget) {
        this.calorieTarget = calorieTarget;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getProteinTarget() { return proteinTarget; }
    public void setProteinTarget(Integer proteinTarget) {
        this.proteinTarget = proteinTarget;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getCarbsTarget() { return carbsTarget; }
    public void setCarbsTarget(Integer carbsTarget) {
        this.carbsTarget = carbsTarget;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getFatsTarget() { return fatsTarget; }
    public void setFatsTarget(Integer fatsTarget) {
        this.fatsTarget = fatsTarget;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getBreakfastCount() { return breakfastCount; }
    public void setBreakfastCount(Integer breakfastCount) {
        this.breakfastCount = breakfastCount;
        // Auto-clear time when count is 0
        if (breakfastCount != null && breakfastCount == 0) {
            this.breakfastTime = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getLunchCount() { return lunchCount; }
    public void setLunchCount(Integer lunchCount) {
        this.lunchCount = lunchCount;
        // Auto-clear time when count is 0
        if (lunchCount != null && lunchCount == 0) {
            this.lunchTime = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getDinnerCount() { return dinnerCount; }
    public void setDinnerCount(Integer dinnerCount) {
        this.dinnerCount = dinnerCount;
        // Auto-clear time when count is 0
        if (dinnerCount != null && dinnerCount == 0) {
            this.dinnerTime = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getSnackCount() { return snackCount; }
    public void setSnackCount(Integer snackCount) {
        this.snackCount = snackCount;
        // Auto-clear time when count is 0
        if (snackCount != null && snackCount == 0) {
            this.snackTime = null;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public String getBreakfastTime() { return breakfastTime; }
    public void setBreakfastTime(String breakfastTime) {
        this.breakfastTime = breakfastTime;
        this.updatedAt = LocalDateTime.now();
    }

    public String getLunchTime() { return lunchTime; }
    public void setLunchTime(String lunchTime) {
        this.lunchTime = lunchTime;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDinnerTime() { return dinnerTime; }
    public void setDinnerTime(String dinnerTime) {
        this.dinnerTime = dinnerTime;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSnackTime() { return snackTime; }
    public void setSnackTime(String snackTime) {
        this.snackTime = snackTime;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
