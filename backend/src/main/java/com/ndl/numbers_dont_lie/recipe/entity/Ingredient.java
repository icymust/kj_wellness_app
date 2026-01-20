package com.ndl.numbers_dont_lie.recipe.entity;

import jakarta.persistence.Column;
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
    private Long id;

    @Column(nullable = false)
    private String label; // ingredient name in lowercase

    @Column(nullable = false, length = 8)
    private String unit; // "gram" or "ml"

    @Column(nullable = false)
    private Double quantityPer100; // always 100.0 for standardization

    @Column(nullable = false)
    private Double calories; // kcal per 100g/ml

    @Column(nullable = false)
    private Double protein; // grams per 100g/ml

    @Column(nullable = false)
    private Double carbs; // grams per 100g/ml

    @Column(nullable = false)
    private Double fats; // grams per 100g/ml

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "embedding", columnDefinition = "real[]")
    private float[] embedding; // vector for RAG search (populated later)

    public Ingredient() {
    }

    public Ingredient(String label, String unit, Double quantityPer100, Double calories, Double protein,
                      Double carbs, Double fats) {
        this.label = label;
        this.unit = unit;
        this.quantityPer100 = quantityPer100;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fats = fats;
    }

    public Long getId() {
        return id;
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

    public Double getCalories() {
        return calories;
    }

    public void setCalories(Double calories) {
        this.calories = calories;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

    public Double getCarbs() {
        return carbs;
    }

    public void setCarbs(Double carbs) {
        this.carbs = carbs;
    }

    public Double getFats() {
        return fats;
    }

    public void setFats(Double fats) {
        this.fats = fats;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }
}
