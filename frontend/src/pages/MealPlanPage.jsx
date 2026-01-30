/**
 * STEP 6.3: Production Meal Plan Page
 * 
 * Displays today's meal plan with nutrition summary.
 * Clean, user-facing interface without debug elements.
 * 
 * Features:
 * - Daily meal plan display
 * - Nutrition summary with progress visualization
 * - Loading and error states
 * - Responsive design
 * - Uses temporary userId=1 (until auth is wired)
 */

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/MealPlan.css';
import { useUser } from '../contexts/UserContext';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';
import { CustomMealComponent } from '../components/CustomMealComponent';

export function MealPlanPage() {
  const navigate = useNavigate();
  const [dayPlan, setDayPlan] = useState(null);
  const [nutritionSummary, setNutritionSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [replacingMealId, setReplacingMealId] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [generatingMealId, setGeneratingMealId] = useState(null);
  const [aiNutritionSummary, setAiNutritionSummary] = useState(null);
  const [aiNutritionLoading, setAiNutritionLoading] = useState(false);
  const [userGoal, setUserGoal] = useState('general_fitness');

  // Get userId from shared context (persisted in localStorage)
  const { userId, setUserId } = useUser();

  // Resolve userId lazily via /protected/me if we have a token but no cached id
  useEffect(() => {
    const fillUserId = async () => {
      if (userId) return;
      const token = getAccessToken();
      if (!token) return;
      try {
        const data = await api.me(token);
        const payload = data?.user || data;
        const resolvedId = payload?.id;
        console.log('[USER_CONTEXT] /protected/me payload', payload);
        if (resolvedId) {
          setUserId(resolvedId);
          console.log(`[USER_CONTEXT] Resolved userId from /protected/me: ${resolvedId}`);
        }
      } catch (err) {
        console.warn('Auto-resolve userId failed', err);
      }
    };
    fillUserId();
  }, [userId, setUserId]);

  /**
   * Load meal plan for a specific date
   */
  const loadMealPlan = async (dateToLoad = null) => {
    if (!userId) {
      setError('Нет активного пользователя. Войдите, чтобы увидеть план.');
      setLoading(false);
      return;
    }
    try {
      setLoading(true);
      setError(null);

      // Use local date to avoid UTC off-by-one issues
      const now = new Date();
      const today = dateToLoad || now.toLocaleDateString('en-CA'); // YYYY-MM-DD in local TZ

      // Fetch day plan from production API
      const dayUrl = `http://localhost:5173/api/meal-plans/day?userId=${userId}&date=${today}`;
      console.log('[MEAL_PLAN] Fetching day plan', dayUrl);
      const dayResponse = await fetch(dayUrl);
      if (!dayResponse.ok) throw new Error(`Failed to load meal plan (${dayResponse.status})`);
      const dayData = await dayResponse.json();
      setDayPlan(dayData);

      // Log successful load
      console.log(`[MEAL_PLAN] Loaded plan for userId = ${userId}`);

      // Fetch nutrition summary from production API
      try {
        const nutritionResponse = await fetch(
          `http://localhost:5173/api/meal-plans/day/nutrition?userId=${userId}&date=${today}`
        );
        if (nutritionResponse.ok) {
          const nutritionData = await nutritionResponse.json();
          setNutritionSummary(nutritionData);
        } else if (nutritionResponse.status === 400) {
          console.warn('[MealPlanPage] Nutrition unavailable, using fallback');
          setNutritionSummary({ nutrition_estimated: true, unavailable: true });
        }
      } catch (nutritionErr) {
        console.warn('Nutrition summary unavailable', nutritionErr);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMealPlan();
  }, [userId]);

  useEffect(() => {
    const loadGoal = async () => {
      const token = getAccessToken();
      if (!token) return;
      try {
        const data = await api.getProfile(token);
        const goal = data?.profile?.goal;
        if (goal) {
          setUserGoal(goal);
        }
      } catch (err) {
        console.warn('[MEAL_PLAN] Failed to load profile goal', err);
      }
    };
    loadGoal();
  }, [userId]);

  useEffect(() => {
    const generateInsights = async () => {
      if (!nutritionSummary || nutritionSummary.unavailable) {
        setAiNutritionSummary(null);
        return;
      }
      if (!dayPlan?.date) {
        return;
      }
      setAiNutritionLoading(true);

      try {
        const response = await fetch('http://localhost:5173/api/ai/nutrition/summary', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId,
            date: dayPlan.date,
            userGoal,
            nutritionSummary: {
              calories: nutritionSummary.total_calories ?? 0,
              targetCalories: nutritionSummary.target_calories ?? 0,
              protein: nutritionSummary.total_protein ?? 0,
              targetProtein: nutritionSummary.target_protein ?? 0,
              carbs: nutritionSummary.total_carbs ?? 0,
              targetCarbs: nutritionSummary.target_carbs ?? 0,
              fats: nutritionSummary.total_fats ?? 0,
              targetFats: nutritionSummary.target_fats ?? 0,
              nutritionEstimated: !!nutritionSummary.nutrition_estimated,
            },
          }),
        });

        if (!response.ok) {
          throw new Error('Failed to generate nutrition insights');
        }

        const data = await response.json();
        setAiNutritionSummary(data?.summary || null);
      } catch (err) {
        setAiNutritionSummary(null);
      } finally {
        setAiNutritionLoading(false);
      }
    };

    generateInsights();
  }, [nutritionSummary, dayPlan?.date, userGoal, userId]);

  /**
   * Refresh meal plan - called manually by user when profile changes
   */
  const handleRefreshMealPlan = async () => {
    if (!userId) return;
    
    console.log('[MEAL_PLAN] Manual refresh triggered by user');
    setRefreshing(true);
    try {
      const now = new Date();
      const today = now.toLocaleDateString('en-CA'); // YYYY-MM-DD in local TZ
      
      // Call dedicated refresh endpoint that regenerates plan based on current profile
      const refreshResponse = await fetch(
        `http://localhost:5173/api/meal-plans/day/refresh?userId=${userId}&date=${today}`,
        { method: 'POST' }
      );
      
      if (refreshResponse.ok) {
        const refreshedPlan = await refreshResponse.json();
        setDayPlan(refreshedPlan);
        console.log('[MEAL_PLAN] Refresh SUCCESS! Got plan with', refreshedPlan?.meals?.length, 'meals');
        
        // Also refresh nutrition summary
        try {
          const nutritionResponse = await fetch(
            `http://localhost:5173/api/meal-plans/day/nutrition?userId=${userId}&date=${today}`
          );
          if (nutritionResponse.ok) {
            const nutritionData = await nutritionResponse.json();
            setNutritionSummary(nutritionData);
            console.log('[MEAL_PLAN] Nutrition summary refreshed');
          }
        } catch (nutritionErr) {
          console.warn('[MEAL_PLAN] Error refreshing nutrition:', nutritionErr);
        }
      } else {
        console.error('[MEAL_PLAN] Refresh failed:', refreshResponse.status);
        setError('Не удалось обновить план питания');
      }
    } catch (err) {
      console.error('[MEAL_PLAN] Error during refresh:', err);
      setError('Ошибка при обновлении плана');
    } finally {
      setRefreshing(false);
    }
  };

  /**
   * Refresh the meal plan - used after adding/deleting custom meals
   */
  const refreshMealPlan = async () => {
    if (!userId) return;
    
    try {
      const now = new Date();
      const today = now.toLocaleDateString('en-CA');
      
      // Fetch current day plan (without regeneration)
      const dayResponse = await fetch(
        `http://localhost:5173/api/meal-plans/day?userId=${userId}&date=${today}`
      );
      if (dayResponse.ok) {
        const dayData = await dayResponse.json();
        setDayPlan(dayData);
        console.log('[MEAL_PLAN] Refreshed day plan after custom meal action');
      }
      
      // Also refresh nutrition summary
      const nutritionResponse = await fetch(
        `http://localhost:5173/api/meal-plans/day/nutrition?userId=${userId}&date=${today}`
      );
      if (nutritionResponse.ok) {
        const nutritionData = await nutritionResponse.json();
        setNutritionSummary(nutritionData);
      }
    } catch (err) {
      console.error('[MEAL_PLAN] Error refreshing plan:', err);
    }
  };

  const handleCustomMealAdded = () => {
    console.log('[MEAL_PLAN] Custom meal added, refreshing...');
    refreshMealPlan();
  };

  const handleCustomMealDeleted = () => {
    console.log('[MEAL_PLAN] Custom meal deleted, refreshing...');
    refreshMealPlan();
  };

  const handleReplaceMeal = async (mealId) => {
    if (!mealId) {
      console.warn('[MEAL_REPLACE] Skip: mealId is missing');
      return;
    }
    try {
      setReplacingMealId(mealId);
      
      console.log(`[MEAL_REPLACE] Replacing meal ${mealId}`);
      const response = await fetch(`http://localhost:5173/api/meal-plans/meals/${mealId}/replace`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error('Meal not found');
        } else if (response.status === 409) {
          throw new Error('No alternative recipes available');
        } else {
          throw new Error(`Failed to replace meal (${response.status})`);
        }
      }
      
      const updatedMeal = await response.json();
      console.log(`[MEAL_REPLACE] Success: ${updatedMeal.custom_meal_name}`);
      
      // Update meal in current day plan
      setDayPlan(prevPlan => ({
        ...prevPlan,
        meals: prevPlan.meals.map(m => m.id === mealId ? updatedMeal : m)
      }));
      
      // Refetch nutrition summary
      const now = new Date();
      const today = now.toLocaleDateString('en-CA');
      const nutritionResponse = await fetch(
        `http://localhost:5173/api/meal-plans/day/nutrition?userId=${userId}&date=${today}`
      );
      if (nutritionResponse.ok) {
        const nutritionData = await nutritionResponse.json();
        setNutritionSummary(nutritionData);
      }
    } catch (err) {
      console.error('[MEAL_REPLACE] Error:', err);
      alert(`Failed to replace meal: ${err.message}`);
    } finally {
      setReplacingMealId(null);
    }
  };

  const handleGenerateAiRecipe = async (meal) => {
    if (!meal || !userId) return;
    const mealType = (meal.meal_type || meal.mealType || '').toString().toUpperCase();
    if (!mealType) return;

    try {
      setGeneratingMealId(meal.id || mealType);
      const response = await fetch('http://localhost:5173/api/ai/recipes/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId, mealType })
      });

      if (!response.ok) {
        const errBody = await response.json().catch(() => ({}));
        throw new Error(errBody.error || `Failed to generate AI recipe (${response.status})`);
      }

      await refreshMealPlan();
    } catch (err) {
      console.error('[AI_RECIPE] Error:', err);
      alert(`Failed to generate AI recipe: ${err.message}`);
    } finally {
      setGeneratingMealId(null);
    }
  };

  if (loading) {
    return (
      <div className="meal-plan-page">
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading your meal plan...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="meal-plan-page">
        <div className="error-state">
          <h2>Unable to Load Meal Plan</h2>
          <p>{error}</p>
          <button onClick={() => window.location.reload()}>
            Try Again
          </button>
        </div>
      </div>
    );
  }

  if (!dayPlan || !dayPlan.meals || dayPlan.meals.length === 0) {
    return (
      <div className="meal-plan-page">
        <div className="empty-state">
          <h2>No Meals Planned</h2>
          <p>Your meal plan for today is empty.</p>
        </div>
      </div>
    );
  }

  const formatDate = (dateStr) => {
    const date = new Date(dateStr + 'T00:00:00');
    return date.toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const renderNutritionBar = (actual, target, label) => {
    const percentage = target > 0 ? (actual / target) * 100 : 0;
    const isWarning = percentage > 110 || percentage < 90;

    return (
      <div key={label} className="nutrition-item">
        <div className="nutrition-header">
          <span className="nutrition-label">{label}</span>
          <span className="nutrition-value">
            {Math.round(actual)} / {Math.round(target)}
          </span>
        </div>
        <div className={`nutrition-bar ${isWarning ? 'warning' : ''}`}>
          <div
            className="nutrition-bar-fill"
            style={{ width: `${Math.min(percentage, 100)}%` }}
          ></div>
        </div>
        <span className="nutrition-percentage">{Math.round(percentage)}%</span>
      </div>
    );
  };

  return (
    <div className="meal-plan-page">
      {/* Header with Refresh Button */}
      <div className="meal-plan-header">
        <div className="meal-plan-header-content">
          <div>
            <h1>Today's Meal Plan</h1>
            <p className="meal-plan-date">{formatDate(dayPlan.date)}</p>
          </div>
          <div className="meal-plan-header-buttons">
            <button
              className="btn-refresh-meal-plan"
              onClick={handleRefreshMealPlan}
              disabled={refreshing || loading}
              title="Refresh meal plan based on profile changes"
            >
              {refreshing ? '🔄 Updating...' : '🔄 Refresh Plan'}
            </button>
            <button
              className="btn-weekly-plan"
              onClick={() => navigate('/meals/week')}
              title="View your weekly meal plan"
            >
              📅 Weekly Plan
            </button>
          </div>
        </div>
      </div>

      {/* Nutrition Summary */}
      {nutritionSummary && (
        <div className="nutrition-summary">
          <h2>Daily Nutrition Summary</h2>
          {nutritionSummary.unavailable && (
            <p className="nutrition-note">
              Nutrition data unavailable for this day (estimated later)
            </p>
          )}
          {nutritionSummary.nutrition_estimated && !nutritionSummary.unavailable && (
            <p className="nutrition-note">
              ℹ️ Nutrition values are estimated based on your daily targets
            </p>
          )}
          {!nutritionSummary.unavailable && (
            <div className="nutrition-grid">
              {renderNutritionBar(
                nutritionSummary.total_calories,
                nutritionSummary.target_calories,
                'Calories'
              )}
              {renderNutritionBar(
                nutritionSummary.total_protein,
                nutritionSummary.target_protein,
                'Protein (g)'
              )}
              {renderNutritionBar(
                nutritionSummary.total_carbs,
                nutritionSummary.target_carbs,
                'Carbs (g)'
              )}
              {renderNutritionBar(
                nutritionSummary.total_fats,
                nutritionSummary.target_fats,
                'Fats (g)'
              )}
            </div>
          )}
          {(aiNutritionLoading || aiNutritionSummary) && !nutritionSummary.unavailable && (
            <div className="ai-nutrition-summary">
              <h3>AI Nutrition Insights</h3>
              {aiNutritionLoading ? (
                <p className="ai-nutrition-loading">Generating nutrition insights…</p>
              ) : (
                <p className="ai-nutrition-text">{aiNutritionSummary}</p>
              )}
            </div>
          )}
        </div>
      )}

      {/* Meals List */}
      <div className="meals-section">
        <h2>Your Meals</h2>
        <div className="meals-list">
          {dayPlan.meals.map((meal, index) => {
            console.log(`[MEAL_DEBUG] index=${index}, recipe_id=${meal.recipe_id}, custom_meal_name=${meal.custom_meal_name}, full_meal=`, meal);
            return (
            <div key={index} className="meal-card">
              <div className="meal-header">
                <h3 className="meal-type">{meal.meal_type}</h3>
                {meal.calories && (
                  <span className="meal-calories">{Math.round(meal.calories)} cal</span>
                )}
              </div>

              {meal.custom_meal_name && (
                <p className="meal-name">{meal.custom_meal_name}</p>
              )}
              
              {/* Button Container */}
              <div style={{ display: 'flex', gap: '10px', marginTop: '10px', flexWrap: 'wrap' }}>
                {/* Replace Button */}
                {meal.id ? (
                  <button 
                    className="replace-meal-button"
                    onClick={() => handleReplaceMeal(meal.id)}
                    disabled={replacingMealId === meal.id}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: replacingMealId === meal.id ? '#ccc' : '#4CAF50',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: replacingMealId === meal.id ? 'not-allowed' : 'pointer',
                      fontSize: '14px',
                      flex: 1,
                      minWidth: '120px'
                    }}
                  >
                    {replacingMealId === meal.id ? 'Replacing...' : 'Regenerate'}
                  </button>
                ) : (
                  <button
                    className="replace-meal-button"
                    disabled
                    style={{
                      padding: '8px 16px',
                      backgroundColor: '#ccc',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'not-allowed',
                      fontSize: '14px',
                      flex: 1,
                      minWidth: '120px'
                    }}
                  >
                    🔒 Недоступно (нет ID)
                  </button>
                )}

                {/* Replace from List Button */}
                {meal.id ? (
                  <button
                    className="replace-from-list-button"
                    onClick={() => navigate(`/meals/replace/${meal.id}?returnTo=daily`)}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: '#FF9800',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '14px',
                      flex: 1,
                      minWidth: '120px'
                    }}
                  >
                    Choose Recipe
                  </button>
                ) : null}

                {/* Generate AI Recipe Button */}
                <button
                  className="generate-ai-recipe-button"
                  onClick={() => handleGenerateAiRecipe(meal)}
                  disabled={generatingMealId === (meal.id || (meal.meal_type || meal.mealType))}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#7B1FA2',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: generatingMealId ? 'not-allowed' : 'pointer',
                    fontSize: '14px',
                    flex: 1,
                    minWidth: '160px'
                  }}
                >
                  {generatingMealId === (meal.id || (meal.meal_type || meal.mealType)) ? 'Generating...' : 'Generate AI Recipe'}
                </button>

                {/* View Recipe Button */}
                <button
                  className="view-recipe-button"
                  onClick={() => {
                    console.log('[VIEW_RECIPE_CLICK] meal.recipe_id=', meal.recipe_id);
                    console.log('[VIEW_RECIPE_CLICK] Navigating to:', meal.recipe_id ? `/recipes/${meal.recipe_id}` : '/recipes/unknown');
                    if (meal.recipe_id) {
                      navigate(`/recipes/${meal.recipe_id}`);
                    } else {
                      navigate('/recipes/unknown');
                    }
                  }}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#2196F3',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '14px',
                    flex: 1,
                    minWidth: '120px'
                  }}
                >
                  View recipe
                </button>
              </div>

              {meal.ingredients && meal.ingredients.length > 0 && (
                <div className="meal-section">
                  <h4>Ingredients</h4>
                  <ul className="ingredients-list">
                    {meal.ingredients.map((ingredient, idx) => (
                      <li key={idx}>{ingredient.name}</li>
                    ))}
                  </ul>
                </div>
              )}

              {meal.preparation_steps && meal.preparation_steps.length > 0 && (
                <div className="meal-section">
                  <h4>Preparation Steps</h4>
                  <ol className="steps-list">
                    {meal.preparation_steps.map((step, idx) => (
                      <li key={idx}>{step.description}</li>
                    ))}
                  </ol>
                </div>
              )}
            </div>
            );
          })}
        </div>
        
        {/* Custom Meals Section - Isolated from generated meals */}
        <CustomMealComponent 
          dayPlan={dayPlan}
          userId={userId}
          onMealAdded={handleCustomMealAdded}
          onMealDeleted={handleCustomMealDeleted}
        />
      </div>
    </div>
  );
}
