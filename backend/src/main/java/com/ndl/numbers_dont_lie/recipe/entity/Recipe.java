package com.ndl.numbers_dont_lie.recipe.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    private Long id; // database ID

    @Column(nullable = false, unique = true, length = 16)
    @JsonProperty("stable_id")
    private String stableId; // stable external ID (e.g., "r00001")

    @Column(nullable = false)
    @JsonProperty("title")
    private String title;

    @Column(length = 64)
    @JsonProperty("cuisine")
    private String cuisine; // e.g. Italian, Mexican, Asian, Mediterranean

    @Column(length = 32)
    @JsonProperty("meal")
    @Enumerated(EnumType.STRING)
    private MealType meal; // breakfast/lunch/dinner/snack

    @Column(nullable = false)
    @JsonProperty("servings")
    private Integer servings;

    @Column(length = 1024)
    @JsonProperty("summary")
    private String summary;

    @Column(name = "time_minutes")
    @JsonProperty("time")
    private Integer timeMinutes; // preparation time in minutes

    @Column(length = 32)
    @JsonProperty("difficulty_level")
    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficultyLevel; // easy/medium/hard

    @Column(length = 64)
    @JsonProperty("source")
    private String source; // e.g. food-com, ai-generated, user-created

    @Column(length = 512)
    @JsonProperty("img")
    private String imageUrl;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recipe_dietary_tags", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "tag", length = 64)
    @JsonProperty("dietary_tags")
    private List<String> dietaryTags = new ArrayList<>();

    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", columnDefinition = "real[]")
    private float[] embedding; // vector for RAG search (populated later)

    @OneToMany(mappedBy = "recipe", fetch = FetchType.EAGER, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("ingredients")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @OneToMany(mappedBy = "recipe", fetch = FetchType.EAGER, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @JsonProperty("preparation")
    private List<PreparationStep> preparationSteps = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Recipe() {
    }

    public Recipe(String stableId, String title, String cuisine, MealType meal, Integer servings, String summary,
                  Integer timeMinutes, DifficultyLevel difficultyLevel, String source, String imageUrl) {
        this.stableId = stableId;
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

    public String getStableId() {
        return stableId;
    }

    public void setStableId(String stableId) {
        this.stableId = stableId;
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

    public MealType getMeal() {
        return meal;
    }

    public void setMeal(MealType meal) {
        this.meal = meal;
    }

    public void setMealFromString(String mealValue) {
        this.meal = MealType.fromString(mealValue);
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

    public DifficultyLevel getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public void setDifficultyLevelFromString(String difficultyValue) {
        this.difficultyLevel = DifficultyLevel.fromString(difficultyValue);
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
