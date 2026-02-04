package com.ndl.numbers_dont_lie.recipe.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "ingredients",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ingredient_label", columnNames = "label")
    }
)
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // database ID

    @Column(nullable = false, unique = true, length = 16)
    @JsonProperty("stable_id")
    private String stableId; // stable external ID (e.g., "ing0974b137")

    @Column(nullable = false)
    @JsonProperty("label")
    private String label; // ingredient name in lowercase

    @Column(nullable = false, length = 8)
    @JsonProperty("unit")
    private String unit; // "gram" or "milliliter"

    @Column(nullable = false)
    @JsonProperty("quantity")
    private Double quantityPer100; // always 100.0 for standardization

    @Embedded
    @JsonProperty("nutrition")
    private Nutrition nutrition;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", columnDefinition = "real[]")
    private float[] embedding; // vector for RAG search (populated later)

    public Ingredient() {
    }

    public Ingredient(String stableId, String label, String unit, Double quantityPer100, 
                      Double calories, Double protein, Double carbs, Double fats) {
        this.stableId = stableId;
        this.label = label;
        this.unit = unit;
        this.quantityPer100 = quantityPer100;
        this.nutrition = new Nutrition(calories, protein, carbs, fats);
    }

    public Ingredient(String stableId, String label, String unit, Double quantityPer100, 
                      Nutrition nutrition) {
        this.stableId = stableId;
        this.label = label;
        this.unit = unit;
        this.quantityPer100 = quantityPer100;
        this.nutrition = nutrition;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getQuantityPer100() {
        return quantityPer100;
    }

    public void setQuantityPer100(Double quantityPer100) {
        this.quantityPer100 = quantityPer100;
    }

    public Nutrition getNutrition() {
        return nutrition;
    }

    public void setNutrition(Nutrition nutrition) {
        this.nutrition = nutrition;
    }

    public Double getCalories() {
        return nutrition != null ? nutrition.getCalories() : null;
    }

    public void setCalories(Double calories) {
        if (nutrition == null) {
            nutrition = new Nutrition();
        }
        nutrition.setCalories(calories);
    }

    public Double getProtein() {
        return nutrition != null ? nutrition.getProtein() : null;
    }

    public void setProtein(Double protein) {
        if (nutrition == null) {
            nutrition = new Nutrition();
        }
        nutrition.setProtein(protein);
    }

    public Double getCarbs() {
        return nutrition != null ? nutrition.getCarbs() : null;
    }

    public void setCarbs(Double carbs) {
        if (nutrition == null) {
            nutrition = new Nutrition();
        }
        nutrition.setCarbs(carbs);
    }

    public Double getFats() {
        return nutrition != null ? nutrition.getFats() : null;
    }

    public void setFats(Double fats) {
        if (nutrition == null) {
            nutrition = new Nutrition();
        }
        nutrition.setFats(fats);
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
