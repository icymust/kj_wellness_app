package com.ndl.numbers_dont_lie.recipe.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "preparation_steps")
public class PreparationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @Column(name = "order_number", nullable = false)
    @JsonProperty("step")
    private Integer orderNumber;

    @Column(length = 256)
    private String stepTitle;

    @Column(length = 2048)
    @JsonProperty("description")
    private String description;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "preparation_step_ingredient_ids", joinColumns = @JoinColumn(name = "step_id"))
    @Column(name = "ingredient_id")
    @JsonProperty("ingredients")
    private List<String> ingredientIds = new ArrayList<>();

    public PreparationStep() {
    }

    public PreparationStep(Recipe recipe, Integer orderNumber, String stepTitle, String description) {
        this.recipe = recipe;
        this.orderNumber = orderNumber;
        this.stepTitle = stepTitle;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }

    public Integer getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(Integer orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getStepTitle() {
        return stepTitle;
    }

    public void setStepTitle(String stepTitle) {
        this.stepTitle = stepTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getIngredientIds() {
        return ingredientIds;
    }

    public void setIngredientIds(List<String> ingredientIds) {
        this.ingredientIds = ingredientIds;
    }
}
