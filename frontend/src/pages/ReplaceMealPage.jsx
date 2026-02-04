/**
 * STEP 6.6: Replace Meal Page
 * 
 * Allows user to replace an existing meal with a recipe selected from database.
 * Respects user dietary preferences and filters available recipes.
 * 
 * Flow:
 * 1. Load meal by mealId (from route params)
 * 2. Load available recipes from database
 * 3. Filter recipes based on user preferences
 * 4. Display recipes in a selectable list
 * 5. On selection, replace meal via backend
 * 6. Redirect back to origin page (daily or weekly)
 */

import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import '../styles/ReplaceMeal.css';
import { useUser } from '../contexts/UserContext';
import { api } from '../lib/api';

export function ReplaceMealPage() {
  const navigate = useNavigate();
  const { mealId } = useParams();
  const [searchParams] = useSearchParams();
  const { userId } = useUser();
  
  // Get returnTo param (e.g., 'daily' or 'weekly')
  const returnTo = searchParams.get('returnTo') || 'daily';
  
  const [meal, setMeal] = useState(null);
  const [recipes, setRecipes] = useState([]);
  const [filteredRecipes, setFilteredRecipes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [replacing, setReplacing] = useState(false);

  // Load meal and available recipes
  useEffect(() => {
    loadMealAndRecipes();
  }, [loadMealAndRecipes]);

  // Filter recipes based on search term
  useEffect(() => {
    if (searchTerm.trim() === '') {
      setFilteredRecipes(recipes);
    } else {
      const term = searchTerm.toLowerCase();
      setFilteredRecipes(recipes.filter(r => 
        r.title.toLowerCase().includes(term) ||
        r.summary?.toLowerCase().includes(term)
      ));
    }
  }, [searchTerm, recipes]);

  const loadMealAndRecipes = useCallback(async () => {
    if (!mealId || !userId) {
      setError('Invalid parameters');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Get access token
      const token = localStorage.getItem('accessToken');

      // Fetch meal
      const mealData = await api.getMealById(token, mealId);
      setMeal(mealData);

      // Fetch recipes filtered by meal type
      const mealType = mealData.meal_type || mealData.mealType;
      const recipesData = await api.getRecipes(token, mealType ? mealType.toLowerCase() : null);
      
      // Ensure recipesData is an array
      const recipesList = Array.isArray(recipesData) ? recipesData : [];
      setRecipes(recipesList);
      setFilteredRecipes(recipesList);

      console.log(`[REPLACE_MEAL] Loaded ${recipesList.length} available recipes for ${mealType}`);
    } catch (err) {
      console.error('[REPLACE_MEAL] Error loading data:', err);
      setError(err.message || 'Error loading data');
    } finally {
      setLoading(false);
    }
  }, [mealId, userId]);

  const handleSelectRecipe = async (recipe) => {8
    if (!mealId || replacing) return;

    try {
      setReplacing(true);
      console.log(`[REPLACE_MEAL] Replacing with recipe: ${recipe.stable_id}`);

      // Get access token
      const token = localStorage.getItem('accessToken');

      // Call backend to replace meal
      const updatedMeal = await api.replaceMeal(token, mealId, { recipeId: recipe.stable_id });

      console.log(`[REPLACE_MEAL] Replacement successful: ${updatedMeal.custom_meal_name}`);

      // Redirect back to origin page
      if (returnTo === 'weekly') {
        navigate('/meals/week', { state: { refreshWeeklyPlan: true } });
      } else {
        navigate('/meals/today');
      }
    } catch (err) {
      console.error('[REPLACE_MEAL] Error:', err.message);
      setError(`Failed to replace meal: ${err.message}`);
      setReplacing(false);
    }
  };

  const handleCancel = () => {
    if (returnTo === 'weekly') {
      navigate('/meals/week');
    } else {
      navigate('/meals/today');
    }
  };

  if (loading) {
    return (
      <div className="replace-meal-page">
        <div className="loading">Loading recipes...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="replace-meal-page">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={handleCancel}>Back</button>
        </div>
      </div>
    );
  }

  if (!meal) {
    return (
      <div className="replace-meal-page">
        <div className="error-message">
          <p>Meal not found</p>
          <button onClick={handleCancel}>Back</button>
        </div>
      </div>
    );
  }

  return (
    <div className="replace-meal-page">
      {/* Header */}
      <div className="replace-header">
        <div className="replace-header-content">
          <h1>Replace Meal</h1>
          <p className="current-meal">
            Current: <strong>{meal.custom_meal_name}</strong>
            <span className="meal-type"> ({meal.meal_type})</span>
          </p>
        </div>
        <button className="btn-cancel" onClick={handleCancel}>
          Cancel
        </button>
      </div>

      {/* Search */}
      <div className="replace-search">
        <input
          type="text"
          placeholder="Search recipes..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          disabled={replacing}
        />
        <span className="recipe-count">
          {filteredRecipes.length} recipe{filteredRecipes.length !== 1 ? 's' : ''}
        </span>
      </div>

      {/* Recipe List */}
      <div className="recipe-list">
        {filteredRecipes.length === 0 ? (
          <div className="no-recipes">
            <p>No recipes available</p>
          </div>
        ) : (
          filteredRecipes.map((recipe) => (
            <div key={recipe.id} className="recipe-card">
              <div className="recipe-content">
                <h3 className="recipe-title">
                  {recipe.title}
                  {(recipe.is_ai_generated || recipe.isAiGenerated) && (
                    <span className="ai-badge">AI</span>
                  )}
                </h3>
                {recipe.summary && (
                  <p className="recipe-summary">{recipe.summary}</p>
                )}
                <div className="recipe-info">
                  {recipe.cuisine && (
                    <span className="recipe-meta">🍴 {recipe.cuisine}</span>
                  )}
                  {recipe.time && (
                    <span className="recipe-meta">⏱ {recipe.time} min</span>
                  )}
                  {recipe.servings && (
                    <span className="recipe-meta">👥 {recipe.servings} servings</span>
                  )}
                </div>
              </div>
              <button
                className="btn-select"
                onClick={() => handleSelectRecipe(recipe)}
                disabled={replacing}
              >
                {replacing ? 'Replacing...' : 'Select'}
              </button>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
