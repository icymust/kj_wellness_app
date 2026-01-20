package com.ndl.numbers_dont_lie.recipe.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "recipes",
    indexes = {
        @Index(name = "idx_recipe_cuisine", columnList = "cuisine"),
        @Index(name = "idx_recipe_meal", columnList = "meal"),
        @Index(name = "idx_recipe_time", columnList = "time_minutes")
    }
)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 64)
    private String cuisine; // e.g. Italian, Mexican, Asian, Mediterranean

    @Column(length = 32)
    private String meal; // breakfast/lunch/dinner/snack

    @Column(nullable = false)
    private Integer servings;

    @Column(length = 1024)
    private String summary;

    @Column(name = "time_minutes")
    private Integer timeMinutes; // preparation time in minutes

    @Column(length = 32)
    private String difficultyLevel; // easy/medium/hard

    @Column(length = 64)
    private String source; // e.g. food-com, ai-generated, user-created

    @Column(length = 512)
    private String imageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recipe_dietary_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag", length = 64)
    private List<String> dietaryTags = new ArrayList<>();

    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", columnDefinition = "real[]")
    private float[] embedding; // vector for RAG search (populated later)

    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<PreparationStep> preparationSteps = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Recipe() {
    }

    public Recipe(String title, String cuisine, String meal, Integer servings, String summary,
                  Integer timeMinutes, String difficultyLevel, String source, String imageUrl) {
        this.title = title;
        this.cuisine = cuisine;
        this.meal = meal;
        this.servings = servings;
        this.summary = summary;
        this.timeMinutes = timeMinutes;
        this.difficultyLevel = difficultyLevel;
        this.source = source;
        this.imageUrl = imageUrl;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getMeal() {
        return meal;
    }

    public void setMeal(String meal) {
        this.meal = meal;
    }

    public Integer getServings() {
        return servings;
    }

    public void setServings(Integer servings) {
        this.servings = servings;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getTimeMinutes() {
        return timeMinutes;
    }

    public void setTimeMinutes(Integer timeMinutes) {
        this.timeMinutes = timeMinutes;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<String> getDietaryTags() {
        return dietaryTags;
    }

    public void setDietaryTags(List<String> dietaryTags) {
        this.dietaryTags = dietaryTags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public List<RecipeIngredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<RecipeIngredient> ingredients) {
        this.ingredients = ingredients;
    }

    public List<PreparationStep> getPreparationSteps() {
        return preparationSteps;
    }

    public void setPreparationSteps(List<PreparationStep> preparationSteps) {
        this.preparationSteps = preparationSteps;
    }
}
