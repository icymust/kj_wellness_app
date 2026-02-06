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

import React, { useCallback, useEffect, useRef, useState } from 'react';
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
  const [aiNutritionError, setAiNutritionError] = useState(null);
  const [aiSuggestions, setAiSuggestions] = useState([]);
  const [aiSuggestionsLoading, setAiSuggestionsLoading] = useState(false);
  const [aiSuggestionsError, setAiSuggestionsError] = useState(null);
  const [aiRateLimitUntil, setAiRateLimitUntil] = useState(null);
  const [mealNutritionMap, setMealNutritionMap] = useState({});
  const [dietaryPreferences, setDietaryPreferences] = useState([]);
  const [userGoal, setUserGoal] = useState('general_fitness');
  const lastAiSummaryKeyRef = useRef(null);
  const lastAiSuggestionsKeyRef = useRef(null);

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
  const loadMealPlan = useCallback(async (dateToLoad = null) => {
    if (!userId) {
      setError('No active user. Sign in to view your plan.');
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
  }, [userId]);

  useEffect(() => {
    loadMealPlan();
  }, [loadMealPlan]);

  useEffect(() => {
    if (!dayPlan?.meals || dayPlan.meals.length === 0) {
      setMealNutritionMap({});
      return;
    }

    let cancelled = false;

    const loadMealNutrition = async () => {
      const updates = {};

      await Promise.all(
        dayPlan.meals.map(async (meal) => {
          const recipeId = meal?.recipe_id || meal?.recipeId;
          if (!recipeId) {
            return;
          }
          try {
            const response = await fetch(`http://localhost:5173/api/recipes/${recipeId}`);
            if (!response.ok) {
              return;
            }
            const data = await response.json();
            const ingredients = Array.isArray(data.ingredients) ? data.ingredients : [];
            const totals = ingredients.reduce(
              (acc, ingredient) => {
                const nutrition = ingredient?.nutrition;
                const quantity = typeof ingredient?.quantity === 'number' ? ingredient.quantity : null;
                if (!nutrition || !quantity || quantity <= 0) return acc;
                const factor = quantity / 100;
                acc.calories += (nutrition.calories || 0) * factor;
                acc.protein += (nutrition.protein || 0) * factor;
                acc.carbs += (nutrition.carbs || 0) * factor;
                acc.fats += (nutrition.fats || 0) * factor;
                return acc;
              },
              { calories: 0, protein: 0, carbs: 0, fats: 0 }
            );
            updates[meal.id] = totals;
          } catch (err) {
            console.warn('[MEAL_PLAN] Failed to load meal nutrition', err);
          }
        })
      );

      if (!cancelled) {
        setMealNutritionMap(updates);
      }
    };

    loadMealNutrition();

    return () => {
      cancelled = true;
    };
  }, [dayPlan?.meals]);

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
    const loadDietaryPreferences = async () => {
      const token = getAccessToken();
      if (!token) return;
      try {
        const data = await api.getNutritionalPreferences(token);
        const prefs = data?.nutritionalPreferences?.dietaryPreferences;
        if (Array.isArray(prefs)) {
          setDietaryPreferences(prefs);
        } else {
          setDietaryPreferences([]);
        }
      } catch {
        setDietaryPreferences([]);
      }
    };
    loadDietaryPreferences();
  }, [userId]);

  useEffect(() => {
    const generateInsights = async () => {
      if (!nutritionSummary || nutritionSummary.unavailable) {
        setAiNutritionSummary(null);
        setAiNutritionError(null);
        return;
      }
      if (!dayPlan?.date) {
        return;
      }
      const summaryKey = `${userId}|${dayPlan.date}|${nutritionSummary.total_calories ?? 0}|${nutritionSummary.total_protein ?? 0}|${nutritionSummary.total_carbs ?? 0}|${nutritionSummary.total_fats ?? 0}|${userGoal}`;
      if (lastAiSummaryKeyRef.current === summaryKey) {
        return;
      }
      if (aiRateLimitUntil && Date.now() < aiRateLimitUntil) {
        setAiNutritionSummary(null);
        setAiNutritionError('AI rate limit reached. Try again later.');
        return;
      }
      setAiNutritionLoading(true);

      try {
        setAiNutritionError(null);
        lastAiSummaryKeyRef.current = summaryKey;
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
          const errBody = await response.json().catch(() => ({}));
          const errorText = `${errBody?.error || ''}`.toLowerCase();
          if (response.status === 429 || errorText.includes('rate limit')) {
            setAiRateLimitUntil(Date.now() + 10 * 60 * 1000);
          }
          const msg = response.status === 429
            ? 'AI rate limit reached. Try again later.'
            : 'AI service unavailable. Please try again later.';
          throw new Error(msg);
        }

        const data = await response.json();
        setAiNutritionSummary(data?.summary || null);
      } catch (err) {
        setAiNutritionSummary(null);
        setAiNutritionError(err?.message || 'AI insights temporarily unavailable.');
      } finally {
        setAiNutritionLoading(false);
      }
    };

    generateInsights();
  }, [nutritionSummary, dayPlan?.date, userGoal, userId, aiRateLimitUntil]);

  useEffect(() => {
    const generateSuggestions = async () => {
      if (!nutritionSummary || nutritionSummary.unavailable) {
        setAiSuggestions([]);
        setAiSuggestionsError(null);
        return;
      }
      if (!dayPlan?.date || !dayPlan?.meals) {
        return;
      }
      const mealKey = (dayPlan.meals || [])
        .map((meal) => `${meal.id || ''}:${meal.recipe_id || meal.recipeId || ''}:${meal.meal_type || meal.mealType || ''}`)
        .join('|');
      const suggestionsKey = `${userId}|${dayPlan.date}|${mealKey}|${userGoal}|${(dietaryPreferences || []).join(',')}`;
      if (lastAiSuggestionsKeyRef.current === suggestionsKey) {
        return;
      }
      if (aiRateLimitUntil && Date.now() < aiRateLimitUntil) {
        setAiSuggestions([]);
        setAiSuggestionsError('AI rate limit reached. Try again later.');
        return;
      }

      setAiSuggestionsLoading(true);
      try {
        setAiSuggestionsError(null);
        lastAiSuggestionsKeyRef.current = suggestionsKey;
        const meals = dayPlan.meals.map((meal) => ({
          mealType: (meal.meal_type || meal.mealType || '').toString().toUpperCase(),
          name: meal.custom_meal_name || meal.customMealName || meal.title || 'Meal',
        }));

        const response = await fetch('http://localhost:5173/api/ai/nutrition/suggestions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId,
            date: dayPlan.date,
            userGoal,
            dietaryPreferences,
            meals,
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
          const errBody = await response.json().catch(() => ({}));
          const errorText = `${errBody?.error || ''}`.toLowerCase();
          if (response.status === 429 || errorText.includes('rate limit')) {
            setAiRateLimitUntil(Date.now() + 10 * 60 * 1000);
          }
          const msg = response.status === 429
            ? 'AI rate limit reached. Try again later.'
            : 'AI service unavailable. Please try again later.';
          throw new Error(msg);
        }

        const data = await response.json();
        const suggestions = Array.isArray(data?.suggestions) ? data.suggestions : [];
        setAiSuggestions(suggestions);
      } catch (err) {
        setAiSuggestions([]);
        setAiSuggestionsError(err?.message || 'AI suggestions temporarily unavailable.');
      } finally {
        setAiSuggestionsLoading(false);
      }
    };

    generateSuggestions();
  }, [nutritionSummary, dayPlan?.date, dayPlan?.meals, userGoal, dietaryPreferences, userId]);

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
        setError('Failed to refresh the meal plan');
      }
    } catch (err) {
      console.error('[MEAL_PLAN] Error during refresh:', err);
      setError('Error refreshing the meal plan');
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
    if (!meal) {
      console.warn('[AI_RECIPE] Missing meal payload');
      return;
    }
    const resolvedUserId = userId || meal.user_id || dayPlan?.user_id;
    if (!resolvedUserId) {
      console.warn('[AI_RECIPE] Missing userId for generation');
      alert('Unable to generate recipe: user not resolved.');
      return;
    }
    if (!userId && resolvedUserId) {
      setUserId(resolvedUserId);
    }
    const mealType = (meal.meal_type || meal.mealType || meal.type || '').toString().toUpperCase();
    if (!mealType) {
      console.warn('[AI_RECIPE] Missing meal type', meal);
      alert('Unable to generate recipe: meal type missing.');
      return;
    }

    try {
      setGeneratingMealId(meal.id || mealType);
      console.log('[AI_RECIPE] Requesting AI recipe', { userId: resolvedUserId, mealType, mealId: meal.id });
      const response = await fetch('http://localhost:5173/api/ai/recipes/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: resolvedUserId, mealType, mealId: meal.id })
      });

      if (!response.ok) {
        const errBody = await response.json().catch(() => ({}));
        throw new Error(errBody.error || `Failed to generate AI recipe (${response.status})`);
      }

      const generatedRecipe = await response.json().catch(() => null);
      if (generatedRecipe && meal?.id) {
        const recipeId =
          generatedRecipe.stable_id ??
          generatedRecipe.stableId ??
          generatedRecipe.id ??
          null;
        const recipeName = generatedRecipe.title || generatedRecipe.name || meal.custom_meal_name;
        setDayPlan(prevPlan => ({
          ...prevPlan,
          meals: prevPlan.meals.map(m => (
            m.id === meal.id
              ? {
                  ...m,
                  recipe_id: recipeId ?? m.recipe_id,
                  custom_meal_name: recipeName,
                  is_ai_generated: true
                }
              : m
          ))
        }));
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

  const getMealCalories = (meal) => {
    const value =
      meal?.planned_calories ??
      meal?.plannedCalories ??
      meal?.calories ??
      meal?.calorie_target ??
      meal?.calorieTarget ??
      null;
    return value != null ? Math.round(value) : null;
  };

  const formatMacroLine = (mealId) => {
    const totals = mealNutritionMap[mealId];
    if (!totals) {
      return null;
    }
    const targetProtein = nutritionSummary?.target_protein ?? 0;
    const targetCarbs = nutritionSummary?.target_carbs ?? 0;
    const targetFats = nutritionSummary?.target_fats ?? 0;

    const pct = (value, target) => (target > 0 ? Math.round((value / target) * 100) : 0);

    return (
      <div className="meal-macro-line">
        <span>Protein {totals.protein.toFixed(1)}g ({pct(totals.protein, targetProtein)}%)</span>
        <span>Carbs {totals.carbs.toFixed(1)}g ({pct(totals.carbs, targetCarbs)}%)</span>
        <span>Fats {totals.fats.toFixed(1)}g ({pct(totals.fats, targetFats)}%)</span>
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
              className="btn-shopping-list"
              onClick={() => {
                const now = new Date();
                const today = now.toLocaleDateString('en-CA');
                navigate(`/shopping-list/day?date=${today}`);
              }}
              title="View shopping list"
            >
              🧺 Shopping List
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
              <div className="calorie-progress">
                <div className="calorie-progress-header">
                  <span className="calorie-progress-label">Calories</span>
                  <span className="calorie-progress-value">
                    {Math.round(nutritionSummary.total_calories ?? 0)} / {Math.round(nutritionSummary.target_calories ?? 0)} kcal
                  </span>
                </div>
                <div
                  className={`calorie-progress-bar ${
                    (nutritionSummary.total_calories ?? 0) > (nutritionSummary.target_calories ?? 0)
                      ? 'surplus'
                      : (nutritionSummary.total_calories ?? 0) === (nutritionSummary.target_calories ?? 0)
                        ? 'on-target'
                        : 'deficit'
                  }`}
                >
                  <div
                    className="calorie-progress-fill"
                    style={{
                      width: `${Math.min(
                        ((nutritionSummary.total_calories ?? 0) /
                          Math.max(nutritionSummary.target_calories ?? 1, 1)) *
                          100,
                        100
                      )}%`,
                    }}
                  ></div>
                </div>
                <div className="calorie-progress-note">
                  {(nutritionSummary.total_calories ?? 0) > (nutritionSummary.target_calories ?? 0)
                    ? 'Surplus'
                    : (nutritionSummary.total_calories ?? 0) === (nutritionSummary.target_calories ?? 0)
                      ? 'On target'
                      : 'Deficit'}
                </div>
              </div>
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
          {(aiNutritionLoading || aiNutritionSummary || aiNutritionError) && !nutritionSummary.unavailable && (
            <div className="ai-nutrition-summary">
              <h3>AI Nutrition Insights</h3>
              {aiNutritionLoading ? (
                <p className="ai-nutrition-loading">Generating nutrition insights…</p>
              ) : aiNutritionError ? (
                <p className="ai-nutrition-loading">{aiNutritionError}</p>
              ) : (
                <p className="ai-nutrition-text">{aiNutritionSummary}</p>
              )}
            </div>
          )}
          {(aiSuggestionsLoading || aiSuggestions.length > 0 || aiSuggestionsError) && !nutritionSummary.unavailable && (
            <div className="ai-nutrition-suggestions">
              <h3>AI Nutrition Suggestions</h3>
              {aiSuggestionsLoading ? (
                <p className="ai-nutrition-loading">Generating suggestions…</p>
              ) : aiSuggestionsError ? (
                <p className="ai-nutrition-loading">{aiSuggestionsError}</p>
              ) : (
                <ul className="ai-nutrition-list">
                  {aiSuggestions.map((suggestion, idx) => (
                    <li key={idx}>{suggestion}</li>
                  ))}
                </ul>
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
                <span className="meal-calories">
                  {getMealCalories(meal) != null ? `${getMealCalories(meal)} kcal` : '—'}
                </span>
              </div>

              {meal.custom_meal_name && (
                <p className="meal-name">{meal.custom_meal_name}</p>
              )}

              {formatMacroLine(meal.id)}
              
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
                    🔒 Unavailable (no ID)
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

                {/* Meal Shopping List Button */}
                {meal.id ? (
                  <button
                    className="meal-shopping-list-button"
                    onClick={() => navigate(`/shopping-list/meal?mealId=${meal.id}`)}
                    style={{
                      padding: '8px 16px',
                      backgroundColor: '#3B82F6',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '14px',
                      flex: 1,
                      minWidth: '160px'
                    }}
                  >
                    Shopping List
                  </button>
                ) : null}

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
