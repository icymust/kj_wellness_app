/**
 * Recipe Details Page
 * 
 * This page fetches and displays recipe data from the database in a user-friendly format.
 * Shows ingredients table, preparation steps, and nutritional information.
 * 
 * Routes:
 * - /recipes/:recipeId - Display recipe by ID
 * - /recipes/unknown - Fallback for meals without stored recipes
 */

import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import '../styles/RecipePage.css';

export function RecipePage() {
  const { recipeId } = useParams();
  const navigate = useNavigate();
  const [recipe, setRecipe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    console.log('[RECIPE_PAGE_DEBUG] Page mounted with recipeId param:', recipeId);
    const fetchRecipe = async () => {
      console.log('[RECIPE_PAGE_DEBUG] fetchRecipe() called, recipeId=', recipeId);
      // Handle "unknown" recipe case
      if (recipeId === 'unknown') {
        console.log('[RECIPE_PAGE_DEBUG] recipeId is "unknown", showing fallback message');
        setLoading(false);
        setError('This meal was generated or added manually. No recipe exists in database.');
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const url = `http://localhost:5173/api/recipes/${recipeId}`;
        console.log('[RECIPE_PAGE_DEBUG] Fetching recipe from:', url);
        const response = await fetch(url);
        console.log('[RECIPE_PAGE_DEBUG] Response status:', response.status);

        if (response.status === 404) {
          console.log('[RECIPE_PAGE_DEBUG] Got 404 - recipe not found');
          setError('Recipe not found in database.');
          setRecipe(null);
          return;
        }

        if (!response.ok) {
          console.log('[RECIPE_PAGE_DEBUG] Response not OK:', response.status);
          throw new Error(`Failed to load recipe (HTTP ${response.status})`);
        }

        const data = await response.json();
        console.log('[RECIPE_PAGE_DEBUG] Successfully loaded recipe:', data);
        setRecipe(data);
        console.log('[RECIPE_PAGE] Loaded recipe:', data);
      } catch (err) {
        console.error('[RECIPE_PAGE_DEBUG] Error loading recipe:', err);
        setError(err.message || 'An error occurred while loading the recipe.');
      } finally {
        setLoading(false);
      }
    };

    fetchRecipe();
  }, [recipeId]);

  if (loading) {
    return (
      <div className="recipe-page">
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading recipe...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="recipe-page">
        <div className="recipe-header">
          <h1>Recipe Details</h1>
          <button onClick={() => navigate(-1)} className="back-button">
            ← Back
          </button>
        </div>
        <div className="error-state">
          <p>{error}</p>
        </div>
      </div>
    );
  }

  if (!recipe) {
    return (
      <div className="recipe-page">
        <div className="recipe-header">
          <h1>Recipe Details</h1>
          <button onClick={() => navigate(-1)} className="back-button">
            ← Back
          </button>
        </div>
        <div className="error-state">
          <p>No recipe data to display.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="recipe-page">
      {/* Header Section */}
      <div className="recipe-header">
        <div className="recipe-title-section">
          <h1>{recipe.title || 'Untitled Recipe'}</h1>
          <div className="recipe-meta">
            {recipe.servings && (
              <span className="meta-badge">
                <strong>Servings:</strong> {recipe.servings}
              </span>
            )}
            {recipe.time && (
              <span className="meta-badge">
                <strong>Time:</strong> {recipe.time} min
              </span>
            )}
            {recipe.difficulty_level && (
              <span className="meta-badge">
                <strong>Difficulty:</strong> {recipe.difficulty_level}
              </span>
            )}
            {recipe.cuisine && (
              <span className="meta-badge">
                <strong>Cuisine:</strong> {recipe.cuisine}
              </span>
            )}
            {recipe.meal && (
              <span className="meta-badge">
                <strong>Meal Type:</strong> {recipe.meal}
              </span>
            )}
          </div>
          {recipe.dietary_tags && recipe.dietary_tags.length > 0 && (
            <div className="dietary-tags">
              {recipe.dietary_tags.map((tag, index) => (
                <span key={index} className="dietary-tag">{tag}</span>
              ))}
            </div>
          )}
        </div>
        <button onClick={() => navigate(-1)} className="back-button">
          ← Back
        </button>
      </div>

      {/* Summary */}
      {recipe.summary && (
        <div className="recipe-section summary-section">
          <h2>Description</h2>
          <p>{recipe.summary}</p>
        </div>
      )}

      {/* Main Content Grid */}
      <div className="recipe-content">
        {/* Ingredients Section */}
        <div className="recipe-section">
          <h2>Ingredients</h2>
          {recipe.ingredients && recipe.ingredients.length > 0 ? (
            <table className="ingredients-table">
              <thead>
                <tr>
                  <th>Ingredient</th>
                  <th>Quantity</th>
                  <th>Unit</th>
                </tr>
              </thead>
              <tbody>
                {recipe.ingredients.map((ingredient, index) => (
                  <tr key={index}>
                    <td>{ingredient.label || 'Unknown'}</td>
                    <td className="quantity-cell">
                      {ingredient.quantity ? ingredient.quantity.toFixed(1) : '-'}
                    </td>
                    <td>{ingredient.unit || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="no-data">No ingredients available</p>
          )}
        </div>

        {/* Preparation Steps Section */}
        <div className="recipe-section">
          <h2>Preparation Steps</h2>
          {recipe.preparation && recipe.preparation.length > 0 ? (
            <ol className="preparation-steps">
              {recipe.preparation.map((step, index) => (
                <li key={index}>{step || 'No description'}</li>
              ))}
            </ol>
          ) : (
            <p className="no-data">No preparation steps available</p>
          )}
        </div>
      </div>

      {/* Additional Info */}
      {(recipe.source || recipe.stable_id) && (
        <div className="recipe-section">
          <h2>Additional Information</h2>
          <div className="recipe-meta">
            {recipe.stable_id && (
              <span className="meta-badge">
                <strong>Recipe ID:</strong> {recipe.stable_id}
              </span>
            )}
            {recipe.source && (
              <span className="meta-badge">
                <strong>Source:</strong> {recipe.source}
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

const styles = {
  container: {
    maxWidth: '1200px',
    margin: '0 auto',
    padding: '20px',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    backgroundColor: '#f5f5f5',
    minHeight: '100vh',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: '30px',
    backgroundColor: 'white',
    padding: '20px',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  recipeInfo: {
    fontSize: '14px',
    color: '#666',
    marginTop: '8px',
  },
  infoLabel: {
    fontWeight: 'bold',
    color: '#333',
  },
  backButton: {
    padding: '10px 20px',
    backgroundColor: '#4CAF50',
    color: 'white',
    border: 'none',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '14px',
    alignSelf: 'flex-start',
  },
  loadingState: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '400px',
    backgroundColor: 'white',
    borderRadius: '8px',
  },
  spinner: {
    width: '40px',
    height: '40px',
    border: '4px solid #f3f3f3',
    borderTop: '4px solid #4CAF50',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
    marginBottom: '20px',
  },
  errorState: {
    backgroundColor: '#ffebee',
    color: '#c62828',
    padding: '20px',
    borderRadius: '8px',
    border: '1px solid #ef5350',
  },
  jsonContainer: {
    backgroundColor: 'white',
    padding: '20px',
    borderRadius: '8px',
    boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
  },
  preBlock: {
    backgroundColor: '#f5f5f5',
    padding: '16px',
    borderRadius: '4px',
    overflow: 'auto',
    border: '1px solid #ddd',
    fontSize: '12px',
    lineHeight: '1.4',
    color: '#333',
  },
};
