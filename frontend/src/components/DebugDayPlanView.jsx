/**
 * STEP 5.5.2: DayPlanView Component
 * 
 * Debug viewer for single-day meal plan.
 * This is a TEMPORARY READ-ONLY component for visual inspection.
 * NOT intended for production use.
 * 
 * Features:
 * - Fetch day plan on mount
 * - Display date and list of meals
 * - For each meal: show type, recipe, calories, macros, ingredients, steps
 * - Show loading/error states
 * - Use JSON.stringify for clarity over beauty
 */

import React, { useState, useEffect } from 'react';
import { fetchDayPlan } from '../lib/debugApi';

export function DayPlanView({ date }) {
  const [dayPlan, setDayPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadDayPlan = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchDayPlan(date);
        setDayPlan(data);
      } catch (err) {
        setError(err.message);
        console.error('Error loading day plan:', err);
      } finally {
        setLoading(false);
      }
    };

    loadDayPlan();
  }, [date]);

  if (loading) {
    return (
      <div style={{ padding: '20px', border: '1px solid #ccc', margin: '10px 0' }}>
        <h3>Daily Meal Plan - {date}</h3>
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '20px', border: '1px solid red', margin: '10px 0', backgroundColor: '#ffe0e0' }}>
        <h3>Daily Meal Plan - {date}</h3>
        <p style={{ color: 'red' }}><strong>Error:</strong> {error}</p>
        <p style={{ fontSize: '0.9em', color: '#666' }}>
          Make sure: 1) User with id=1 exists, 2) User profile is configured, 3) AI strategy is cached (STEP 4.1), 4) Backend is running on port 8080
        </p>
      </div>
    );
  }

  if (!dayPlan) {
    return (
      <div style={{ padding: '20px', border: '1px solid #ccc', margin: '10px 0' }}>
        <h3>Daily Meal Plan - {date}</h3>
        <p>No data received from API</p>
      </div>
    );
  }

  return (
    <div style={{ padding: '20px', border: '1px solid #ddd', margin: '10px 0', backgroundColor: '#f9f9f9' }}>
      <h3>Daily Meal Plan - {date}</h3>
      
      {/* Summary */}
      <div style={{ marginBottom: '20px', padding: '10px', backgroundColor: '#e8f4f8', borderRadius: '4px' }}>
        <p><strong>Date:</strong> {dayPlan.date}</p>
        <p><strong>Total Meals:</strong> {dayPlan.meals?.length || 0}</p>
        <p><strong>ID:</strong> {dayPlan.id}</p>
      </div>

      {/* Meals List */}
      <div>
        <h4>Meals:</h4>
        {dayPlan.meals && dayPlan.meals.length > 0 ? (
          dayPlan.meals.map((meal, idx) => (
            <div
              key={meal.id || idx}
              style={{
                marginBottom: '15px',
                padding: '15px',
                border: '1px solid #e0e0e0',
                borderRadius: '4px',
                backgroundColor: '#fafafa'
              }}
            >
              {/* Meal Header */}
              <div style={{ marginBottom: '10px', borderBottom: '1px solid #eee', paddingBottom: '8px' }}>
                <strong style={{ fontSize: '1.1em' }}>
                  {meal.mealType} {meal.index ? `(Index: ${meal.index})` : ''}
                </strong>
                {meal.plannedTime && (
                  <span style={{ marginLeft: '15px', color: '#666' }}>
                    Scheduled: {meal.plannedTime}
                  </span>
                )}
              </div>

              {/* Recipe Name */}
              {(meal.recipeName || meal.customMealName) && (
                <p>
                  <strong>Recipe:</strong> {meal.recipeName || meal.customMealName}
                </p>
              )}

              {/* Recipe ID */}
              {meal.recipeId && (
                <p>
                  <strong>Recipe ID:</strong> {meal.recipeId}
                </p>
              )}

              {/* Calories & Macros (if available in recipe) */}
              {meal.recipe && (
                <div style={{ padding: '10px', backgroundColor: '#f0f0f0', borderRadius: '3px', marginBottom: '10px' }}>
                  {meal.recipe.calories && (
                    <p><strong>Calories:</strong> {meal.recipe.calories} kcal</p>
                  )}
                  {(meal.recipe.protein || meal.recipe.carbs || meal.recipe.fat) && (
                    <p>
                      <strong>Macros:</strong> P: {meal.recipe.protein}g | C: {meal.recipe.carbs}g | F: {meal.recipe.fat}g
                    </p>
                  )}
                </div>
              )}

              {/* Ingredients */}
              {meal.recipe?.ingredients && meal.recipe.ingredients.length > 0 && (
                <div style={{ marginBottom: '10px' }}>
                  <strong>Ingredients:</strong>
                  <ul style={{ marginTop: '5px', marginBottom: '10px' }}>
                    {meal.recipe.ingredients.map((ingredient, i) => (
                      <li key={i}>{ingredient}</li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Preparation Steps */}
              {meal.recipe?.preparationSteps && meal.recipe.preparationSteps.length > 0 && (
                <div style={{ marginBottom: '10px' }}>
                  <strong>Preparation:</strong>
                  <ol style={{ marginTop: '5px', marginBottom: '10px' }}>
                    {meal.recipe.preparationSteps.map((step, i) => (
                      <li key={i}>{step}</li>
                    ))}
                  </ol>
                </div>
              )}

              {/* Full JSON for inspection */}
              <details style={{ marginTop: '10px', padding: '10px', backgroundColor: '#f5f5f5', borderRadius: '3px' }}>
                <summary style={{ cursor: 'pointer', fontWeight: 'bold', color: '#0066cc' }}>
                  View Full JSON
                </summary>
                <pre style={{ 
                  marginTop: '10px',
                  padding: '10px',
                  backgroundColor: '#f0f0f0',
                  borderRadius: '3px',
                  overflow: 'auto',
                  fontSize: '0.85em',
                  lineHeight: '1.4'
                }}>
                  {JSON.stringify(meal, null, 2)}
                </pre>
              </details>
            </div>
          ))
        ) : (
          <p style={{ color: '#999' }}>No meals found for this day</p>
        )}
      </div>

      {/* Full Day Plan JSON */}
      <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f5f5f5', borderRadius: '4px' }}>
        <details>
          <summary style={{ cursor: 'pointer', fontWeight: 'bold', color: '#0066cc' }}>
            View Full Day Plan JSON
          </summary>
          <pre style={{ 
            marginTop: '10px',
            padding: '10px',
            backgroundColor: '#f0f0f0',
            borderRadius: '3px',
            overflow: 'auto',
            fontSize: '0.85em',
            lineHeight: '1.4'
          }}>
            {JSON.stringify(dayPlan, null, 2)}
          </pre>
        </details>
      </div>
    </div>
  );
}
