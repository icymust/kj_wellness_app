package com.ndl.numbers_dont_lie.ai.dto;

/**
 * Result DTO for recipe retrieval via RAG.
 * 
 * Contains minimal recipe metadata plus relevance score.
 * Full recipe details can be fetched via recipeId if needed.
 * 
 * Part of RAG Pipeline Stage 3: Retrieval → Augmentation
 */
public class RetrievedRecipe {
    private Long recipeId;
    private String title;
    private String cuisine;
    private double relevanceScore; // cosine similarity score [0.0, 1.0]

    public RetrievedRecipe() {}

    public RetrievedRecipe(Long recipeId, String title, String cuisine, double relevanceScore) {
        this.recipeId = recipeId;
        this.title = title;
        this.cuisine = cuisine;
        this.relevanceScore = relevanceScore;
    }

    public Long getRecipeId() { return recipeId; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public double getRelevanceScore() { return relevanceScore; }
    public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
}
