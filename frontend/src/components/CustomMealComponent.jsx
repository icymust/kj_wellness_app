/**
 * CustomMealComponent - Isolated custom meals section
 * 
 * Design intent:
 * - Fully isolated from generated meals display
 * - Allows users to add and delete custom meals
 * - Does NOT count toward meal limits
 * - No replace/regenerate buttons
 */

import React, { useState } from 'react';
import '../styles/CustomMeals.css';

export function CustomMealComponent({ dayPlan, userId, onMealAdded, onMealDeleted }) {
  const [mealName, setMealName] = useState('');
  const [mealType, setMealType] = useState('breakfast');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Filter custom meals from the day plan
  const customMeals = dayPlan?.meals?.filter(meal => meal.is_custom) || [];

  const handleAddCustomMeal = async (e) => {
    e.preventDefault();
    
    if (!mealName.trim()) {
      setError('Meal name is required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const today = new Date();
      const dateStr = today.toLocaleDateString('en-CA'); // YYYY-MM-DD
      
      const response = await fetch(
        `http://localhost:5173/api/meal-plans/meals/custom?userId=${userId}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            date: dateStr,
            meal_type: mealType,
            name: mealName,
          }),
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.error || 'Failed to add custom meal');
      }

      const newMeal = await response.json();
      console.log('[CUSTOM_MEAL_COMPONENT] Custom meal added:', newMeal);
      
      // Clear form
      setMealName('');
      setMealType('breakfast');
      
      // Notify parent to reload day plan
      onMealAdded?.(newMeal);
      
    } catch (err) {
      console.error('[CUSTOM_MEAL_COMPONENT] Error adding custom meal:', err);
      setError(err.message || 'Failed to add custom meal');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteCustomMeal = async (mealId) => {
    if (!window.confirm('Are you sure you want to delete this custom meal?')) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const response = await fetch(
        `http://localhost:5173/api/meal-plans/meals/custom/${mealId}?userId=${userId}`,
        {
          method: 'DELETE',
        }
      );

      if (!response.ok) {
        throw new Error('Failed to delete custom meal');
      }

      console.log('[CUSTOM_MEAL_COMPONENT] Custom meal deleted:', mealId);
      
      // Notify parent to reload day plan
      onMealDeleted?.(mealId);
      
    } catch (err) {
      console.error('[CUSTOM_MEAL_COMPONENT] Error deleting custom meal:', err);
      setError(err.message || 'Failed to delete custom meal');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="custom-meals-section">
      <h3>🍽️ Custom Meals</h3>
      
      {error && <div className="custom-meals-error">{error}</div>}
      
      <form className="custom-meal-form" onSubmit={handleAddCustomMeal}>
        <div className="form-group">
          <label htmlFor="custom-meal-name">Meal Name:</label>
          <input
            id="custom-meal-name"
            type="text"
            placeholder="e.g., Homemade Pizza"
            value={mealName}
            onChange={(e) => setMealName(e.target.value)}
            disabled={loading}
            className="form-input"
          />
        </div>
        
        <div className="form-group">
          <label htmlFor="custom-meal-type">Meal Type:</label>
          <select
            id="custom-meal-type"
            value={mealType}
            onChange={(e) => setMealType(e.target.value)}
            disabled={loading}
            className="form-select"
          >
            <option value="breakfast">Breakfast</option>
            <option value="lunch">Lunch</option>
            <option value="dinner">Dinner</option>
            <option value="snack">Snack</option>
          </select>
        </div>
        
        <button 
          type="submit" 
          disabled={loading}
          className="btn-add-meal"
        >
          {loading ? 'Adding...' : '+ Add Meal'}
        </button>
      </form>
      
      {customMeals.length > 0 && (
        <div className="custom-meals-list">
          <h4>Your Custom Meals ({customMeals.length})</h4>
          {customMeals.map((meal) => (
            <div key={meal.id} className="custom-meal-item">
              <div className="custom-meal-info">
                <span className="custom-meal-name">{meal.custom_meal_name}</span>
                <span className="custom-meal-type-tag">
                  {meal.meal_type.charAt(0).toUpperCase() + meal.meal_type.slice(1)}
                </span>
                <span className="custom-meal-badge">Custom</span>
              </div>
              <button
                className="btn-delete-meal"
                onClick={() => handleDeleteCustomMeal(meal.id)}
                disabled={loading}
                title="Delete this custom meal"
              >
                🗑
              </button>
            </div>
          ))}
        </div>
      )}
      
      {customMeals.length === 0 && (
        <div className="custom-meals-empty">
          <p>No custom meals yet. Add one above to get started!</p>
        </div>
      )}
    </div>
  );
}
