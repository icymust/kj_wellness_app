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
import '../styles/MealPlan.css';
import { useUser } from '../contexts/UserContext';

export function MealPlanPage() {
  const [dayPlan, setDayPlan] = useState(null);
  const [nutritionSummary, setNutritionSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Get userId from shared context (persisted in localStorage)
  // TODO: Replace with actual userId from auth when authentication is implemented
  const { userId } = useUser();

  useEffect(() => {
    const loadMealPlan = async () => {
      try {
        setLoading(true);
        setError(null);

        // Use local date to avoid UTC off-by-one issues
        const now = new Date();
        const today = now.toLocaleDateString('en-CA'); // YYYY-MM-DD in local TZ

        // Fetch day plan from production API
        const dayResponse = await fetch(
          `http://localhost:5173/api/meal-plans/day?userId=${userId}&date=${today}`
        );
        if (!dayResponse.ok) throw new Error('Failed to load meal plan');
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

    loadMealPlan();
  }, [userId]);

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
      {/* Header */}
      <div className="meal-plan-header">
        <h1>Today's Meal Plan</h1>
        <p className="meal-plan-date">{formatDate(dayPlan.date)}</p>
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
        </div>
      )}

      {/* Meals List */}
      <div className="meals-section">
        <h2>Your Meals</h2>
        <div className="meals-list">
          {dayPlan.meals.map((meal, index) => (
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
          ))}
        </div>
      </div>
    </div>
  );
}
