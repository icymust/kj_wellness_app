/**
 * STEP 6.5: Weekly Meal Plan Page
 * 
 * Displays a complete 7-day meal plan with nutrition summary.
 * Read-only interface showing all days sequentially.
 * 
 * Features:
 * - Date range display (startDate – endDate)
 * - Weekly nutrition summary
 * - 7 days listed sequentially
 * - Meals grouped by meal type per day
 * - Graceful handling of missing data
 */

import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/WeeklyMealPlan.css';
import { useUser } from '../contexts/UserContext';
import { getAccessToken } from '../lib/tokens';

export function WeeklyMealPlanPage() {
  const navigate = useNavigate();
  const { userId, setUserId } = useUser();
  
  const [weeklyPlan, setWeeklyPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [replacingMeal, setReplacingMeal] = useState(null);

  // Resolve userId from /protected/me if needed
  useEffect(() => {
    const fillUserId = async () => {
      if (userId) return;
      const token = getAccessToken();
      if (!token) return;
      try {
        const response = await fetch('http://localhost:5173/api/protected/me', {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        if (response.ok) {
          const data = await response.json();
          const resolvedId = data?.user?.id || data?.id;
          if (resolvedId) {
            setUserId(resolvedId);
          }
        }
      } catch (err) {
        console.warn('Auto-resolve userId failed', err);
      }
    };
    fillUserId();
  }, [userId, setUserId]);

  /**
   * Load weekly meal plan
   */
  const loadWeeklyPlan = useCallback(async () => {
    if (!userId) {
      setError('No active user. Sign in to see the plan.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);

      // Use today as start date
      const now = new Date();
      const today = now.toLocaleDateString('en-CA'); // YYYY-MM-DD in local TZ

      console.log(`[WEEK_PLAN_PAGE] Loading weekly plan for userId=${userId} startDate=${today}`);

      // Fetch weekly plan from backend
      const weekUrl = `http://localhost:5173/api/meal-plans/week?userId=${userId}&startDate=${today}`;
      const response = await fetch(weekUrl);
      
      if (!response.ok) {
        throw new Error(`Failed to load weekly plan (${response.status})`);
      }

      const data = await response.json();
      setWeeklyPlan(data);

      // Debug: log meal IDs
      if (data.days && data.days.length > 0) {
        const firstDay = data.days[0];
        if (firstDay.meals && firstDay.meals.length > 0) {
          console.log('[WEEK_PLAN_PAGE] Sample meal object:', firstDay.meals[0]);
          console.log('[WEEK_PLAN_PAGE] Meal ID field:', firstDay.meals[0].id);
        }
      }

      console.log(`[WEEK_PLAN_PAGE] Loaded weekly plan for userId=${userId} startDate=${today}`);
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Error loading weekly plan:', err);
      setError(err.message || 'Error loading weekly plan');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    if (!userId) return;
    loadWeeklyPlan();
  }, [userId, loadWeeklyPlan]);

  /**
   * Refresh weekly meal plan
   */
  const handleRefreshWeeklyPlan = async () => {
    if (!userId) return;
    
    console.log('[WEEK_PLAN_PAGE] Manual refresh triggered by user');
    setRefreshing(true);
    try {
      const now = new Date();
      const today = now.toLocaleDateString('en-CA');
      
      const weekUrl = `http://localhost:5173/api/meal-plans/week?userId=${userId}&startDate=${today}`;
      const response = await fetch(weekUrl);
      
      if (response.ok) {
        const data = await response.json();
        setWeeklyPlan(data);
        console.log('[WEEK_PLAN_PAGE] Refresh SUCCESS!');
      } else {
        console.error('[WEEK_PLAN_PAGE] Refresh failed:', response.status);
        setError('Failed to update weekly plan');
      }
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Error during refresh:', err);
      setError('Error updating plan');
    } finally {
      setRefreshing(false);
    }
  };

  /**
   * Replace a meal with a new one
   * Calls backend endpoint to generate a fresh meal for the specific date and meal type
   */
  const handleReplaceMeal = async (date, mealType) => {
    console.log('[MEAL_REPLACE] Button clicked, date:', date, 'mealType:', mealType);
    
    if (!date || !mealType) {
      console.warn('[MEAL_REPLACE] Skip: date or mealType is missing');
      return;
    }
    
    try {
      const key = `${date}-${mealType}`;
      setReplacingMeal(key);
      console.log(`[MEAL_REPLACE] Replacing meal for ${date} ${mealType}`);
      
      // Call backend endpoint to generate a new meal
      const response = await fetch(
        `http://localhost:5173/api/meal-plans/week/meals/replace?userId=${userId}&date=${date}&mealType=${mealType}`,
        { method: 'POST' }
      );
      
      if (!response.ok) {
        throw new Error(`Failed to replace meal (${response.status})`);
      }
      
      const newMeal = await response.json();
      console.log(`[MEAL_REPLACE] Success: ${newMeal.custom_meal_name || newMeal.customMealName}`);
      
      // Update only the specific meal in the weekly plan
      setWeeklyPlan(prevPlan => ({
        ...prevPlan,
        days: prevPlan.days.map(day => {
          if (day.date !== date) return day;
          
          return {
            ...day,
            meals: day.meals.map(meal => {
              const currentMealType = (meal.meal_type || meal.mealType || '').toUpperCase();
              if (currentMealType === mealType.toUpperCase()) {
                return newMeal;
              }
              return meal;
            })
          };
        })
      }));
      
    } catch (err) {
      console.error(`[MEAL_REPLACE] Error: ${err.message}`);
      setError(`Failed to replace meal: ${err.message}`);
    } finally {
      setReplacingMeal(null);
    }
  };

  if (loading) {
    return (
      <div className="weekly-meal-plan-page">
        <div className="loading">Loading weekly plan...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="weekly-meal-plan-page">
        <div className="error-message">
          <p>{error}</p>
          <button onClick={() => navigate('/meals/today')}>
            Return to daily plan
          </button>
        </div>
      </div>
    );
  }

  if (!weeklyPlan || !weeklyPlan.days || weeklyPlan.days.length === 0) {
    return (
      <div className="weekly-meal-plan-page">
        <div className="no-data">
          <p>No data for weekly plan</p>
          <button onClick={() => navigate('/meals/today')}>
            Return to daily plan
          </button>
        </div>
      </div>
    );
  }

  const { startDate: responseStartDate, endDate, days, weeklyNutrition } = weeklyPlan;

  return (
    <div className="weekly-meal-plan-page">
      {/* Header */}
      <div className="weekly-header">
        <div className="weekly-header-content">
          <div>
            <h1>Weekly Meal Plan</h1>
            <p className="weekly-date-range">
              {responseStartDate} – {endDate}
            </p>
          </div>
          <div className="weekly-header-buttons">
            <button
              className="btn-refresh-weekly-plan"
              onClick={handleRefreshWeeklyPlan}
              disabled={refreshing || loading}
              title="Refresh weekly plan"
            >
              {refreshing ? '🔄 Refreshing...' : 'Refresh plan'}
            </button>
            <button
              className="btn-day-plan"
              onClick={() => navigate('/meals/today')}
              title="Daily plan"
            >
              📅 Daily Plan
            </button>
          </div>
        </div>
      </div>

      {/* Weekly Nutrition Summary */}
      {weeklyNutrition && (
        <div className="weekly-nutrition-summary">
          <div className="nutrition-card">
            <div className="nutrition-stat">
              <span className="label">Calories</span>
              <span className="value">{Math.round(weeklyNutrition.totalCalories)}</span>
              <span className="target">/ {Math.round(weeklyNutrition.targetCalories)}</span>
            </div>
            <div className="nutrition-bar">
              <div 
                className="nutrition-progress"
                style={{
                  width: `${Math.min(weeklyNutrition.caloriesPercentage, 100)}%`
                }}
              ></div>
            </div>
          </div>

          <div className="nutrition-card">
            <div className="nutrition-stat">
              <span className="label">Protein</span>
              <span className="value">{weeklyNutrition.totalProtein.toFixed(1)}g</span>
              <span className="target">/ {weeklyNutrition.targetProtein.toFixed(1)}g</span>
            </div>
            <div className="nutrition-bar">
              <div 
                className="nutrition-progress"
                style={{
                  width: `${Math.min(weeklyNutrition.proteinPercentage, 100)}%`
                }}
              ></div>
            </div>
          </div>

          <div className="nutrition-card">
            <div className="nutrition-stat">
              <span className="label">Carbs</span>
              <span className="value">{weeklyNutrition.totalCarbs.toFixed(1)}g</span>
              <span className="target">/ {weeklyNutrition.targetCarbs.toFixed(1)}g</span>
            </div>
            <div className="nutrition-bar">
              <div 
                className="nutrition-progress"
                style={{
                  width: `${Math.min(weeklyNutrition.carbsPercentage, 100)}%`
                }}
              ></div>
            </div>
          </div>

          <div className="nutrition-card">
            <div className="nutrition-stat">
              <span className="label">Fats</span>
              <span className="value">{weeklyNutrition.totalFats.toFixed(1)}g</span>
              <span className="target">/ {weeklyNutrition.targetFats.toFixed(1)}g</span>
            </div>
            <div className="nutrition-bar">
              <div 
                className="nutrition-progress"
                style={{
                  width: `${Math.min(weeklyNutrition.fatsPercentage, 100)}%`
                }}
              ></div>
            </div>
          </div>

          {weeklyNutrition.nutritionEstimated && (
            <p className="nutrition-note">* Nutrition estimated</p>
          )}
        </div>
      )}

      {/* Days List */}
      <div className="weekly-days">
        {days.map((day, index) => (
          <div key={index} className="day-card">
            <div className="day-header">
              <h2 className="day-date">{day.date}</h2>
              {day.context_hash && (
                <span className="day-hash">{day.context_hash.substring(0, 8)}</span>
              )}
            </div>

            {day.meals && day.meals.length > 0 ? (
              <div className="day-meals">
                {groupMealsByType(day.meals).map((mealGroup, groupIndex) => (
                  <div key={groupIndex} className="meal-group">
                    <h3 className="meal-type-label">{formatMealType(mealGroup.type)}</h3>
                    <div className="meals-list">
                      {mealGroup.meals.map((meal, mealIndex) => (
                        <div key={mealIndex} className="meal-item-wrapper">
                          <div className="meal-item">
                            <span className="meal-name">
                              {meal.custom_meal_name || meal.customMealName || meal.meal_name || '(Meal)'}
                            </span>
                            {(meal.calorie_target || meal.calorieTarget) && (
                              <span className="meal-calories">
                                {Math.round(meal.calorie_target || meal.calorieTarget)} kcal
                              </span>
                            )}
                          </div>
                          <div className="meal-actions">
                            <button
                              className="meal-btn meal-btn-info"
                              onClick={() => navigate(`/recipes/${meal.recipe_id || meal.recipeId || 'unknown'}`)}
                              title="View recipe details"
                            >
                              ?
                            </button>
                            <button
                              className="meal-btn meal-btn-replace"
                              onClick={() => handleReplaceMeal(day.date, meal.meal_type || meal.mealType)}
                              disabled={replacingMeal === `${day.date}-${meal.meal_type || meal.mealType}`}
                              title="Replace this meal"
                            >
                              ↻
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="no-meals">
                <p>No meals planned for this day</p>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

/**
 * Group meals by type for display
 */
function groupMealsByType(meals) {
  const grouped = {};
  
  meals.forEach(meal => {
    // Handle both camelCase and snake_case field names from API
    const mealType = (meal.mealType || meal.meal_type || 'OTHER').toUpperCase();
    if (!grouped[mealType]) {
      grouped[mealType] = { type: mealType, meals: [] };
    }
    grouped[mealType].meals.push(meal);
  });

  // Sort by meal order: breakfast, lunch, dinner, snack
  const mealOrder = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
  return Object.values(grouped).sort((a, b) => {
    const indexA = mealOrder.indexOf(a.type);
    const indexB = mealOrder.indexOf(b.type);
    return (indexA >= 0 ? indexA : 999) - (indexB >= 0 ? indexB : 999);
  });
}

/**
 * Format meal type for display
 */
function formatMealType(mealType) {
  const translations = {
    'BREAKFAST': 'Breakfast',
    'LUNCH': 'Lunch',
    'DINNER': 'Dinner',
    'SNACK': 'Snack',
    'OTHER': 'Other'
  };
  return translations[mealType] || mealType;
}
