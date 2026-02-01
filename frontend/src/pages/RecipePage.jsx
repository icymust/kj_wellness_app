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

import React, { useEffect, useMemo, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import '../styles/RecipePage.css';

export function RecipePage() {
  const { recipeId } = useParams();
  const navigate = useNavigate();
  const [recipe, setRecipe] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [servingsInput, setServingsInput] = useState('');
  const [substituteState, setSubstituteState] = useState({
    ingredientKey: null,
    alternatives: [],
    loading: false,
    error: null,
  });
  const [availabilityInput, setAvailabilityInput] = useState('');

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
        setServingsInput(String(data.servings || 1));
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

  const nutritionTotals = useMemo(() => {
    const totals = { calories: 0, protein: 0, carbs: 0, fats: 0 };
    const ingredients = Array.isArray(recipe?.ingredients) ? recipe.ingredients : [];
    ingredients.forEach((ingredient) => {
      const nutrition = ingredient?.nutrition;
      const quantity = typeof ingredient?.quantity === 'number' ? ingredient.quantity : null;
      if (!nutrition || !quantity || quantity <= 0) return;
      const factor = quantity / 100;
      totals.calories += (nutrition.calories || 0) * factor;
      totals.protein += (nutrition.protein || 0) * factor;
      totals.carbs += (nutrition.carbs || 0) * factor;
      totals.fats += (nutrition.fats || 0) * factor;
    });
    return totals;
  }, [recipe]);

  const nutritionMax = Math.max(
    nutritionTotals.calories,
    nutritionTotals.protein,
    nutritionTotals.carbs,
    nutritionTotals.fats,
    1
  );

  const formatNumber = (value) => {
    if (typeof value !== 'number' || Number.isNaN(value)) return '0';
    return value % 1 === 0 ? String(value) : value.toFixed(1);
  };

  const nutritionAvailable =
    nutritionTotals.calories > 0 ||
    nutritionTotals.protein > 0 ||
    nutritionTotals.carbs > 0 ||
    nutritionTotals.fats > 0;

  const handleSuggestSubstitute = async (ingredientLabel) => {
    if (!recipe?.id) {
      return;
    }

    if (substituteState.ingredientKey === ingredientLabel && !substituteState.loading) {
      setSubstituteState({
        ingredientKey: null,
        alternatives: [],
        loading: false,
        error: null,
      });
      return;
    }

    setSubstituteState({
      ingredientKey: ingredientLabel,
      alternatives: [],
      loading: true,
      error: null,
    });

    try {
      const response = await fetch('http://localhost:5173/api/ai/ingredients/substitute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          recipeId: recipe.id,
          ingredientName: ingredientLabel,
          availableIngredients: availabilityInput,
        }),
      });

      if (!response.ok) {
        throw new Error('Failed to get substitutes');
      }

      const data = await response.json();
      setSubstituteState({
        ingredientKey: ingredientLabel,
        alternatives: Array.isArray(data.alternatives) ? data.alternatives : [],
        loading: false,
        error: null,
      });
    } catch (err) {
      setSubstituteState({
        ingredientKey: ingredientLabel,
        alternatives: [],
        loading: false,
        error: err.message || 'Failed to get substitutes',
      });
    }
  };

  const handleApplySubstitute = async (ingredientLabel, newIngredientName) => {
    if (!recipe?.id) {
      return;
    }

    setSubstituteState((prev) => ({
      ...prev,
      loading: true,
      error: null,
    }));

    try {
      const response = await fetch(
        `http://localhost:5173/api/recipes/${recipe.id}/ingredients/replace`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            ingredientName: ingredientLabel,
            newIngredientName,
          }),
        }
      );

      if (!response.ok) {
        throw new Error('Failed to apply substitute');
      }

      const data = await response.json();
      setRecipe(data);
      setSubstituteState({
        ingredientKey: null,
        alternatives: [],
        loading: false,
        error: null,
      });
    } catch (err) {
      setSubstituteState((prev) => ({
        ...prev,
        loading: false,
        error: err.message || 'Failed to apply substitute',
      }));
    }
  };

  const handleUpdateServings = async () => {
    if (!recipe?.id) {
      return;
    }
    const next = Number(servingsInput);
    if (!Number.isFinite(next) || next <= 0) {
      setError('Servings must be a positive number.');
      return;
    }

    try {
      setError(null);
      const response = await fetch(
        `http://localhost:5173/api/recipes/${recipe.id}/servings`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ servings: Math.floor(next) }),
        }
      );

      if (!response.ok) {
        throw new Error('Failed to update servings');
      }

      const data = await response.json();
      setRecipe(data);
    } catch (err) {
      setError(err.message || 'Failed to update servings.');
    }
  };

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
            {(recipe.is_ai_generated || recipe.isAiGenerated) && (
              <span className="meta-badge ai-badge">
                AI
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

      <div className="recipe-section servings-section">
        <h2>Serving Size</h2>
        <div className="servings-controls">
          <label>
            Servings
            <input
              type="number"
              min="1"
              step="1"
              value={servingsInput}
              onChange={(e) => setServingsInput(e.target.value)}
            />
          </label>
          <button type="button" className="servings-apply-btn" onClick={handleUpdateServings}>
            Update servings
          </button>
        </div>
        <p className="servings-note">
          Ingredient quantities and nutrition update based on serving size.
        </p>
      </div>

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
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {recipe.ingredients.map((ingredient, index) => (
                  <React.Fragment key={`${ingredient.label}-${index}`}>
                    <tr>
                      <td>{ingredient.label || 'Unknown'}</td>
                      <td className="quantity-cell">
                        {ingredient.quantity ? ingredient.quantity.toFixed(1) : '-'}
                      </td>
                      <td>{ingredient.unit || '-'}</td>
                      <td className="ingredient-actions-cell">
                        <button
                          className="substitute-button"
                          onClick={() => handleSuggestSubstitute(ingredient.label)}
                          disabled={substituteState.loading && substituteState.ingredientKey === ingredient.label}
                        >
                          ⚡ Suggest substitute
                        </button>
                      </td>
                    </tr>
                    {substituteState.ingredientKey === ingredient.label && (
                      <tr className="ingredient-substitute-row">
                        <td colSpan="4">
                          <div className="substitute-panel">
                            {substituteState.loading && (
                              <div className="substitute-loading">Loading suggestions...</div>
                            )}
                            {substituteState.error && (
                              <div className="substitute-error">{substituteState.error}</div>
                            )}
                            {!substituteState.loading && !substituteState.error && (
                              <>
                                <div className="substitute-availability">
                                  <label>Available ingredients (optional)</label>
                                  <input
                                    type="text"
                                    placeholder="e.g. tofu, mushrooms"
                                    value={availabilityInput}
                                    onChange={(e) => setAvailabilityInput(e.target.value)}
                                  />
                                </div>
                                {substituteState.alternatives.length > 0 ? (
                                  <div className="substitute-options">
                                    {substituteState.alternatives.map((alt, altIndex) => (
                                      <div className="substitute-option" key={`${alt.name}-${altIndex}`}>
                                        <div className="substitute-option-text">
                                          <span className="substitute-name">{alt.name}</span>
                                          <span className="substitute-reason">{alt.reason}</span>
                                        </div>
                                        <button
                                          className="substitute-apply-button"
                                          onClick={() => handleApplySubstitute(ingredient.label, alt.name)}
                                        >
                                          Apply
                                        </button>
                                      </div>
                                    ))}
                                  </div>
                                ) : (
                                  <div className="substitute-empty">No alternatives found.</div>
                                )}
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          ) : (
            <p className="no-data">No ingredients available</p>
          )}
        </div>

        {/* Nutrition Summary */}
        <div className="recipe-section nutrition-section">
          <h2>Nutrition Summary</h2>
          {nutritionAvailable ? (
            <>
              <div className="nutrition-stats">
                <div className="nutrition-stat">
                  <span className="stat-label">Calories</span>
                  <span className="stat-value">{formatNumber(nutritionTotals.calories)} kcal</span>
                </div>
                <div className="nutrition-stat">
                  <span className="stat-label">Protein</span>
                  <span className="stat-value">{formatNumber(nutritionTotals.protein)} g</span>
                </div>
                <div className="nutrition-stat">
                  <span className="stat-label">Carbs</span>
                  <span className="stat-value">{formatNumber(nutritionTotals.carbs)} g</span>
                </div>
                <div className="nutrition-stat">
                  <span className="stat-label">Fats</span>
                  <span className="stat-value">{formatNumber(nutritionTotals.fats)} g</span>
                </div>
              </div>
              <div className="nutrition-chart">
                {[
                  { key: 'calories', label: 'Calories', unit: 'kcal', color: '#f97316' },
                  { key: 'protein', label: 'Protein', unit: 'g', color: '#22c55e' },
                  { key: 'carbs', label: 'Carbs', unit: 'g', color: '#3b82f6' },
                  { key: 'fats', label: 'Fats', unit: 'g', color: '#a855f7' },
                ].map((item) => {
                  const value = nutritionTotals[item.key] || 0;
                  const height = Math.max(8, Math.round((value / nutritionMax) * 140));
                  return (
                    <div key={item.key} className="nutrition-bar">
                      <div className="bar-wrapper">
                        <div
                          className="bar-fill"
                          style={{ height: `${height}px`, backgroundColor: item.color }}
                        />
                      </div>
                      <div className="bar-label">{item.label}</div>
                      <div className="bar-value">
                        {formatNumber(value)} {item.unit}
                      </div>
                    </div>
                  );
                })}
              </div>
              <p className="nutrition-note">
                Estimated from ingredient data. Meal plan calories may differ based on plan targets and portioning.
              </p>
            </>
          ) : (
            <p className="no-data">Nutrition data unavailable for this recipe.</p>
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
