package com.ndl.numbers_dont_lie.entity.nutrition;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
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

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }
}
