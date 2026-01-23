/**
 * STEP 5.5.2 + STEP 6.3: DayPlanView Component
 * 
 * Debug viewer for single-day meal plan with nutrition visualization.
 * This is a TEMPORARY READ-ONLY component for visual inspection.
 * NOT intended for production use.
 * 
 * Features:
 * - Fetch day plan on mount
 * - Display date and list of meals
 * - For each meal: show type, recipe, calories, macros, ingredients, steps
 * - STEP 6.3: Nutrition Summary section with progress bars
 * - Show loading/error states
 * - Use JSON.stringify for clarity over beauty
 */

import React, { useState, useEffect } from 'react';
import { fetchDayPlan, fetchDayNutrition } from '../lib/debugApi';

export function DayPlanView({ date }) {
  const [dayPlan, setDayPlan] = useState(null);
  const [nutritionSummary, setNutritionSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [nutritionError, setNutritionError] = useState(null);

  useEffect(() => {
    const loadDayData = async () => {
      try {
        setLoading(true);
        setError(null);
        setNutritionError(null);
        
        // Fetch day plan
        const dayPlanData = await fetchDayPlan(date);
        setDayPlan(dayPlanData);
        
        // Fetch nutrition summary (STEP 6.3)
        try {
          const nutritionData = await fetchDayNutrition(date);
          setNutritionSummary(nutritionData);
        } catch (nutritionErr) {
          console.warn('Failed to load nutrition summary:', nutritionErr);
          setNutritionError(nutritionErr.message);
        }
      } catch (err) {
        setError(err.message);
        console.error('Error loading day plan:', err);
      } finally {
        setLoading(false);
      }
    };

    loadDayData();
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

      {/* STEP 6.3: Nutrition Summary Section */}
      {nutritionSummary ? (
        <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#f0f8e8', border: '1px solid #d0e0c0', borderRadius: '4px' }}>
          <h4 style={{ marginTop: 0, marginBottom: '15px', color: '#2d5016' }}>📊 Nutrition Summary</h4>
          
          {/* Calories */}
          <NutritionProgress
            label="Calories"
            actual={nutritionSummary.total_calories}
            target={nutritionSummary.target_calories}
            percentage={nutritionSummary.calories_percentage}
            unit="kcal"
          />
          
          {/* Protein */}
          <NutritionProgress
            label="Protein"
            actual={nutritionSummary.total_protein}
            target={nutritionSummary.target_protein}
            percentage={nutritionSummary.protein_percentage}
            unit="g"
          />
          
          {/* Carbs */}
          <NutritionProgress
            label="Carbs"
            actual={nutritionSummary.total_carbs}
            target={nutritionSummary.target_carbs}
            percentage={nutritionSummary.carbs_percentage}
            unit="g"
          />
          
          {/* Fats */}
          <NutritionProgress
            label="Fats"
            actual={nutritionSummary.total_fats}
            target={nutritionSummary.target_fats}
            percentage={nutritionSummary.fats_percentage}
            unit="g"
          />
          
          {/* Metadata */}
          <div style={{ marginTop: '15px', fontSize: '0.85em', color: '#666', borderTop: '1px solid #d0e0c0', paddingTop: '10px' }}>
            <p style={{ margin: '5px 0' }}>
              <strong>Date:</strong> {nutritionSummary.date} | <strong>Meals:</strong> {nutritionSummary.meal_count}
            </p>
            <p style={{ margin: '5px 0', fontStyle: 'italic', color: '#888' }}>
              ℹ️ MVP Note: Nutrition data is placeholder (Meal entity doesn't store nutrition yet)
            </p>
          </div>
        </div>
      ) : nutritionError ? (
        <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#fff8e0', border: '1px solid #f0d080', borderRadius: '4px' }}>
          <h4 style={{ marginTop: 0, color: '#806000' }}>⚠️ Nutrition Summary Unavailable</h4>
          <p style={{ fontSize: '0.9em', color: '#666' }}>{nutritionError}</p>
        </div>
      ) : null}

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

/**
 * STEP 6.3: Nutrition Progress Bar Component
 * 
 * Displays a single macro nutrient with:
 * - Label, actual value, target value
 * - Progress bar showing percentage
 * - Color coding: green (80-100%), yellow (100-120%), red (>120%)
 */
function NutritionProgress({ label, actual, target, percentage, unit }) {
  // Handle missing data
  if (actual === null || actual === undefined) {
    return (
      <div style={{ marginBottom: '12px' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px', fontSize: '0.9em' }}>
          <strong>{label}:</strong>
          <span style={{ color: '#999' }}>No data</span>
        </div>
      </div>
    );
  }

  // Determine bar color based on percentage
  let barColor = '#4caf50'; // Green: within range (80-100%)
  let backgroundColor = '#e0f2e0';
  
  if (percentage === null || target === null || target === 0) {
    barColor = '#999'; // Gray: no target set
    backgroundColor = '#f0f0f0';
  } else if (percentage > 120) {
    barColor = '#f44336'; // Red: exceeded (>120%)
    backgroundColor = '#ffe0e0';
  } else if (percentage > 100) {
    barColor = '#ff9800'; // Orange/Yellow: near limit (100-120%)
    backgroundColor = '#fff8e0';
  }

  // Cap percentage display at 100% for bar width (but show actual % in text)
  const barWidth = percentage ? Math.min(percentage, 100) : 0;
  const displayPercentage = percentage ? Math.round(percentage) : 0;

  return (
    <div style={{ marginBottom: '12px' }}>
      {/* Label and values */}
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '5px', fontSize: '0.9em' }}>
        <strong>{label}:</strong>
        <span>
          {actual ? actual.toFixed(1) : '0.0'} / {target || '—'} {unit}
          {percentage !== null && (
            <span style={{ marginLeft: '10px', fontWeight: 'bold', color: barColor }}>
              ({displayPercentage}%)
            </span>
          )}
        </span>
      </div>
      
      {/* Progress bar */}
      <div style={{
        width: '100%',
        height: '20px',
        backgroundColor: backgroundColor,
        borderRadius: '10px',
        overflow: 'hidden',
        border: '1px solid #ddd'
      }}>
        <div style={{
          width: `${barWidth}%`,
          height: '100%',
          backgroundColor: barColor,
          transition: 'width 0.3s ease',
          borderRadius: '10px'
        }} />
      </div>
    </div>
  );
}
