/**
 * STEP 5.5.2: WeekPlanView Component
 * 
 * Debug viewer for 7-day weekly meal plan.
 * This is a TEMPORARY READ-ONLY component for visual inspection.
 * NOT intended for production use.
 * 
 * Features:
 * - Fetch week plan on mount
 * - Display week range (startDate - endDate)
 * - Iterate through 7 days
 * - Reuse DayPlanView layout for each day (simplified inline rendering)
 * - Show loading/error states
 * - Use JSON.stringify for clarity over beauty
 */

import React, { useState, useEffect } from 'react';
import { fetchWeekPlan } from '../lib/debugApi';

export function WeekPlanView({ startDate }) {
  const [weekPlan, setWeekPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadWeekPlan = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await fetchWeekPlan(startDate);
        setWeekPlan(data);
      } catch (err) {
        setError(err.message);
        console.error('Error loading week plan:', err);
      } finally {
        setLoading(false);
      }
    };

    loadWeekPlan();
  }, [startDate]);

  if (loading) {
    return (
      <div style={{ padding: '20px', border: '1px solid #ccc', margin: '10px 0' }}>
        <h2>Weekly Meal Plan - Week of {startDate}</h2>
        <p>Loading...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{ padding: '20px', border: '1px solid red', margin: '10px 0', backgroundColor: '#ffe0e0' }}>
        <h2>Weekly Meal Plan - Week of {startDate}</h2>
        <p style={{ color: 'red' }}><strong>Error:</strong> {error}</p>
        <p style={{ fontSize: '0.9em', color: '#666' }}>
          Make sure: 1) User with id=1 exists, 2) User profile is configured, 3) AI strategy is cached (STEP 4.1), 4) Backend is running on port 8080
        </p>
      </div>
    );
  }

  if (!weekPlan) {
    return (
      <div style={{ padding: '20px', border: '1px solid #ccc', margin: '10px 0' }}>
        <h2>Weekly Meal Plan - Week of {startDate}</h2>
        <p>No data received from API</p>
      </div>
    );
  }

  // Calculate end date (7 days from start)
  const startDateObj = new Date(startDate);
  const endDateObj = new Date(startDateObj);
  endDateObj.setDate(endDateObj.getDate() + 6);
  const endDateStr = endDateObj.toISOString().split('T')[0];

  return (
    <div style={{ padding: '20px', border: '2px solid #0066cc', margin: '20px 0', backgroundColor: '#f0f7ff' }}>
      <h2>Weekly Meal Plan</h2>
      
      {/* Summary */}
      <div style={{ marginBottom: '20px', padding: '15px', backgroundColor: '#e0f0ff', borderRadius: '4px' }}>
        <p><strong>Week:</strong> {startDate} to {endDateStr}</p>
        <p><strong>Total Days:</strong> {weekPlan.dayPlans?.length || 0}</p>
        <p><strong>MealPlan ID:</strong> {weekPlan.id}</p>
        <p><strong>User ID:</strong> {weekPlan.userId}</p>
        <p><strong>Created:</strong> {weekPlan.createdAt}</p>
        <p><strong>Current Version:</strong> {weekPlan.currentVersion}</p>
      </div>

      {/* Days List */}
      <div>
        {weekPlan.dayPlans && weekPlan.dayPlans.length > 0 ? (
          weekPlan.dayPlans.map((dayPlan, dayIdx) => (
            <div
              key={dayPlan.id || dayIdx}
              style={{
                marginBottom: '20px',
                padding: '15px',
                border: '1px solid #0066cc',
                borderRadius: '4px',
                backgroundColor: '#ffffff'
              }}
            >
              {/* Day Header */}
              <div style={{ marginBottom: '15px', paddingBottom: '10px', borderBottom: '2px solid #0066cc' }}>
                <h3 style={{ margin: 0, color: '#0066cc' }}>
                  {new Date(dayPlan.date).toLocaleDateString('en-US', { 
                    weekday: 'long', 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric' 
                  })}
                </h3>
                <p style={{ margin: '5px 0 0 0', color: '#666', fontSize: '0.95em' }}>
                  {dayPlan.date} • {dayPlan.meals?.length || 0} meals
                </p>
              </div>

              {/* Meals for this day */}
              {dayPlan.meals && dayPlan.meals.length > 0 ? (
                <div style={{ marginLeft: '10px' }}>
                  {dayPlan.meals.map((meal, mealIdx) => (
                    <div
                      key={meal.id || mealIdx}
                      style={{
                        marginBottom: '12px',
                        padding: '12px',
                        border: '1px solid #ddd',
                        borderRadius: '3px',
                        backgroundColor: '#fafafa'
                      }}
                    >
                      {/* Meal Type & Time */}
                      <div style={{ marginBottom: '8px' }}>
                        <strong style={{ color: '#0066cc' }}>
                          {meal.mealType}
                          {meal.plannedTime && ` @ ${meal.plannedTime}`}
                        </strong>
                      </div>

                      {/* Recipe Name */}
                      {(meal.recipeName || meal.customMealName) && (
                        <p style={{ margin: '5px 0', fontSize: '0.95em' }}>
                          <strong>Recipe:</strong> {meal.recipeName || meal.customMealName}
                        </p>
                      )}

                      {/* Recipe Basics */}
                      {meal.recipe && (
                        <div style={{ fontSize: '0.9em', color: '#555' }}>
                          {meal.recipe.calories && (
                            <p style={{ margin: '3px 0' }}>
                              <strong>Calories:</strong> {meal.recipe.calories} kcal
                            </p>
                          )}
                          {(meal.recipe.protein || meal.recipe.carbs || meal.recipe.fat) && (
                            <p style={{ margin: '3px 0' }}>
                              <strong>Macros:</strong> P: {meal.recipe.protein}g | C: {meal.recipe.carbs}g | F: {meal.recipe.fat}g
                            </p>
                          )}
                        </div>
                      )}

                      {/* JSON Details */}
                      <details style={{ marginTop: '8px', fontSize: '0.85em' }}>
                        <summary style={{ cursor: 'pointer', color: '#0066cc', fontSize: '0.9em' }}>
                          Details
                        </summary>
                        <pre style={{
                          marginTop: '8px',
                          padding: '8px',
                          backgroundColor: '#f5f5f5',
                          borderRadius: '3px',
                          overflow: 'auto',
                          fontSize: '0.8em',
                          lineHeight: '1.3'
                        }}>
                          {JSON.stringify(meal, null, 2)}
                        </pre>
                      </details>
                    </div>
                  ))}
                </div>
              ) : (
                <p style={{ color: '#999', marginLeft: '10px' }}>No meals for this day</p>
              )}

              {/* Day JSON */}
              <details style={{ marginTop: '12px', fontSize: '0.85em' }}>
                <summary style={{ cursor: 'pointer', color: '#0066cc' }}>
                  View Full Day JSON
                </summary>
                <pre style={{
                  marginTop: '8px',
                  padding: '8px',
                  backgroundColor: '#f5f5f5',
                  borderRadius: '3px',
                  overflow: 'auto',
                  fontSize: '0.8em',
                  lineHeight: '1.3'
                }}>
                  {JSON.stringify(dayPlan, null, 2)}
                </pre>
              </details>
            </div>
          ))
        ) : (
          <p style={{ color: '#999' }}>No days found in this week</p>
        )}
      </div>

      {/* Full Week Plan JSON */}
      <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f5f5f5', borderRadius: '4px' }}>
        <details>
          <summary style={{ cursor: 'pointer', fontWeight: 'bold', color: '#0066cc' }}>
            View Full Weekly Plan JSON
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
            {JSON.stringify(weekPlan, null, 2)}
          </pre>
        </details>
      </div>
    </div>
  );
}
