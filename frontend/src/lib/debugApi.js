/**
 * STEP 5.5.2: Debug Frontend API Layer
 * 
 * Minimal fetch-based API client for the debug viewer.
 * This is a temporary debug-only API layer - NOT production-ready.
 * 
 * Used by DayPlanView and WeekPlanView to fetch AI-generated meal plans.
 */

const DEBUG_API_BASE = "http://localhost:8080/api/debug/meal-plans";

/**
 * Fetch a single-day meal plan from debug API
 * @param {string} date - ISO date string (YYYY-MM-DD)
 * @returns {Promise<Object>} - DayPlan entity
 * @throws {Error} - if fetch fails or API returns error
 */
export async function fetchDayPlan(date) {
  try {
    const url = `${DEBUG_API_BASE}/day?date=${date}`;
    console.log(`[Debug] Fetching day plan: ${url}`);
    
    const response = await fetch(url);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || 
        `HTTP ${response.status}: ${response.statusText}`
      );
    }
    
    const data = await response.json();
    console.log(`[Debug] Day plan fetched successfully for ${date}:`, data);
    return data;
  } catch (error) {
    console.error(`[Debug] Failed to fetch day plan for ${date}:`, error);
    throw error;
  }
}

/**
 * Fetch a 7-day weekly meal plan from debug API
 * @param {string} startDate - ISO date string (YYYY-MM-DD) for start of week
 * @returns {Promise<Object>} - MealPlan entity with 7 DayPlans
 * @throws {Error} - if fetch fails or API returns error
 */
export async function fetchWeekPlan(startDate) {
  try {
    const url = `${DEBUG_API_BASE}/week?startDate=${startDate}`;
    console.log(`[Debug] Fetching week plan: ${url}`);
    
    const response = await fetch(url);
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      throw new Error(
        errorData.message || 
        `HTTP ${response.status}: ${response.statusText}`
      );
    }
    
    const data = await response.json();
    console.log(`[Debug] Week plan fetched successfully for ${startDate}:`, data);
    return data;
  } catch (error) {
    console.error(`[Debug] Failed to fetch week plan for ${startDate}:`, error);
    throw error;
  }
}
