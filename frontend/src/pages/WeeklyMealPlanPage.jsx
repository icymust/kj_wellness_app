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

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import '../styles/WeeklyMealPlan.css';
import { useUser } from '../contexts/UserContext';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';

export function WeeklyMealPlanPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { userId, setUserId } = useUser();
  
  const [weeklyPlan, setWeeklyPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [movingMealId, setMovingMealId] = useState(null);
  const [customMealFormDate, setCustomMealFormDate] = useState(null);
  const [customMealName, setCustomMealName] = useState('');
  const [customMealType, setCustomMealType] = useState('breakfast');
  const [customMealLoading, setCustomMealLoading] = useState(false);
  const [customMealError, setCustomMealError] = useState(null);
  const [trendData, setTrendData] = useState(null);
  const [trendError, setTrendError] = useState(null);
  const [monthlyTrendDays, setMonthlyTrendDays] = useState([]);
  const [monthlyTrendError, setMonthlyTrendError] = useState(null);
  const [generatingMealId, setGeneratingMealId] = useState(null);
  const lastReloadRef = useRef(0);
  const locationRefreshHandledRef = useRef(false);
  const initialLoadRef = useRef(false);
  const lastMonthlyTrendStartRef = useRef(null);
  const lastWeeklyTrendStartRef = useRef(null);
  const [weeklyInsights, setWeeklyInsights] = useState(null);
  const [weeklyInsightsLoading, setWeeklyInsightsLoading] = useState(false);
  const [weeklyInsightsError, setWeeklyInsightsError] = useState(null);

  const getWeekStart = useCallback((date) => {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    return d.toLocaleDateString('en-CA');
  }, []);

  const normalizeWeekStart = useCallback((startDate, fallbackDate = new Date()) => {
    const fallback = getWeekStart(fallbackDate);
    if (!startDate) return fallback;
    const normalized = getWeekStart(new Date(startDate));
    return startDate === normalized ? startDate : fallback;
  }, [getWeekStart]);
  const [userGoal, setUserGoal] = useState('maintenance');
  const [versionHistory, setVersionHistory] = useState(null);
  const [versionsLoading, setVersionsLoading] = useState(false);
  const [versionsError, setVersionsError] = useState(null);
  const [deletingVersion, setDeletingVersion] = useState(null);
  const [restoringVersion, setRestoringVersion] = useState(null);

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

  const fetchWeeklyVersions = useCallback(async (startDate) => {
    if (!userId || !startDate) return;
    try {
      setVersionsLoading(true);
      setVersionsError(null);
      const url = `http://localhost:5173/api/meal-plans/week/versions?userId=${userId}&startDate=${startDate}`;
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`Failed to load plan versions (${response.status})`);
      }
      const data = await response.json();
      setVersionHistory(data);
    } catch (err) {
      setVersionHistory(null);
      setVersionsError(err?.message || 'Failed to load plan versions.');
    } finally {
      setVersionsLoading(false);
    }
  }, [userId]);

  const fetchWeeklyTrends = useCallback(async (startDate) => {
    if (!userId || !startDate) return;
    if (lastWeeklyTrendStartRef.current === startDate) return;
    lastWeeklyTrendStartRef.current = startDate;
    try {
      setTrendError(null);
      const trendUrl = `http://localhost:5173/api/meal-plans/week/trends?userId=${userId}&startDate=${startDate}`;
      const response = await fetch(trendUrl);
      if (!response.ok) {
        throw new Error(`Failed to load trends (${response.status})`);
      }
      const data = await response.json();
      setTrendData(data);
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Trend load error:', err);
      setTrendError(err.message || 'Failed to load trends');
    }
  }, [userId]);

  const fetchMonthlyTrends = useCallback(async (startDate) => {
    if (!userId || !startDate) return;
    if (lastMonthlyTrendStartRef.current === startDate) return;
    lastMonthlyTrendStartRef.current = startDate;
    try {
      setMonthlyTrendError(null);
      const baseDate = new Date(startDate);
      const weekStarts = Array.from({ length: 4 }, (_, idx) => {
        const d = new Date(baseDate);
        d.setDate(baseDate.getDate() + idx * 7);
        return d.toLocaleDateString('en-CA');
      });

      const responses = await Promise.all(
        weekStarts.map((weekStart) =>
          fetch(`http://localhost:5173/api/meal-plans/week/trends?userId=${userId}&startDate=${weekStart}`)
        )
      );

      const payloads = await Promise.all(
        responses.map((response) => (response.ok ? response.json() : null))
      );

      const merged = [];
      payloads.forEach((data) => {
        if (data?.days?.length) {
          merged.push(...data.days);
        }
      });

      setMonthlyTrendDays(merged);
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Monthly trend load error:', err);
      setMonthlyTrendError(err.message || 'Failed to load monthly trends');
      setMonthlyTrendDays([]);
    }
  }, [userId]);


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

      const now = new Date();
      const weekStart = normalizeWeekStart(weeklyPlan?.startDate, now);

      console.log(`[WEEK_PLAN_PAGE] Loading weekly plan for userId=${userId} startDate=${weekStart}`);

      // Fetch weekly plan from backend
      const weekUrl = `http://localhost:5173/api/meal-plans/week?userId=${userId}&startDate=${weekStart}`;
      const response = await fetch(weekUrl);
      
      if (!response.ok) {
        throw new Error(`Failed to load weekly plan (${response.status})`);
      }

      const data = await response.json();
      setWeeklyPlan(data);
      await fetchWeeklyTrends(weekStart);
      await fetchMonthlyTrends(weekStart);
      await fetchWeeklyVersions(weekStart);

      // Debug: log meal IDs
      if (data.days && data.days.length > 0) {
        const firstDay = data.days[0];
        if (firstDay.meals && firstDay.meals.length > 0) {
          console.log('[WEEK_PLAN_PAGE] Sample meal object:', firstDay.meals[0]);
          console.log('[WEEK_PLAN_PAGE] Meal ID field:', firstDay.meals[0].id);
        }
      }

      console.log(`[WEEK_PLAN_PAGE] Loaded weekly plan for userId=${userId} startDate=${weekStart}`);
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Error loading weekly plan:', err);
      setError(err.message || 'Error loading weekly plan');
    } finally {
      setLoading(false);
    }
  }, [
    userId,
    normalizeWeekStart,
    weeklyPlan?.startDate,
    fetchWeeklyTrends,
    fetchMonthlyTrends,
    fetchWeeklyVersions,
  ]);

  useEffect(() => {
    if (!userId) return;
    if (initialLoadRef.current) return;
    initialLoadRef.current = true;
    loadWeeklyPlan();
  }, [userId, loadWeeklyPlan]);

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
        console.warn('[WEEK_PLAN_PAGE] Failed to load profile goal', err);
      }
    };
    loadGoal();
  }, [userId]);

  useEffect(() => {
    const generateWeeklyInsights = async () => {
      if (!weeklyPlan?.weeklyNutrition || !trendData?.days?.length || !weeklyPlan?.startDate || !weeklyPlan?.endDate) {
        setWeeklyInsights(null);
        setWeeklyInsightsError(null);
        return;
      }

      setWeeklyInsightsLoading(true);
      try {
        setWeeklyInsightsError(null);
        const dailySummaries = trendData.days.map((day) => ({
          date: day.date,
          calories: day.actualCalories ?? 0,
          targetCalories: day.targetCalories ?? 0,
        }));

        const response = await fetch('http://localhost:5173/api/ai/nutrition/weekly-insights', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId,
            startDate: weeklyPlan.startDate,
            endDate: weeklyPlan.endDate,
            userGoal,
            weeklyNutrition: {
              totalCalories: weeklyPlan.weeklyNutrition.totalCalories ?? 0,
              targetCalories: weeklyPlan.weeklyNutrition.targetCalories ?? 0,
              totalProtein: weeklyPlan.weeklyNutrition.totalProtein ?? 0,
              targetProtein: weeklyPlan.weeklyNutrition.targetProtein ?? 0,
              totalCarbs: weeklyPlan.weeklyNutrition.totalCarbs ?? 0,
              targetCarbs: weeklyPlan.weeklyNutrition.targetCarbs ?? 0,
              totalFats: weeklyPlan.weeklyNutrition.totalFats ?? 0,
              targetFats: weeklyPlan.weeklyNutrition.targetFats ?? 0,
              nutritionEstimated: !!weeklyPlan.weeklyNutrition.nutritionEstimated,
            },
            dailySummaries,
          }),
        });

        if (!response.ok) {
          const msg = response.status === 429
            ? 'AI rate limit reached. Try again later.'
            : 'AI service unavailable. Please try again later.';
          throw new Error(msg);
        }

        const data = await response.json();
        setWeeklyInsights(data?.summary || null);
      } catch (err) {
        setWeeklyInsights(null);
        setWeeklyInsightsError(err?.message || 'AI insights temporarily unavailable.');
      } finally {
        setWeeklyInsightsLoading(false);
      }
    };

    generateWeeklyInsights();
  }, [weeklyPlan, trendData, userGoal, userId]);

  const reloadWeeklyPlan = useCallback(async (startDateOverride) => {
    if (!userId) return;
    try {
      const nowMs = Date.now();
      if (nowMs - lastReloadRef.current < 800) {
        return;
      }
      lastReloadRef.current = nowMs;
      const now = new Date();
      const weekStart = normalizeWeekStart(weeklyPlan?.startDate, now);
      const startDate = normalizeWeekStart(startDateOverride || weeklyPlan?.startDate || weekStart, now);
      const weekUrl = `http://localhost:5173/api/meal-plans/week?userId=${userId}&startDate=${startDate}`;
      const response = await fetch(weekUrl);
      if (response.ok) {
        const data = await response.json();
        setWeeklyPlan(data);
        await fetchWeeklyTrends(startDate);
        await fetchMonthlyTrends(startDate);
        await fetchWeeklyVersions(startDate);
      } else {
        console.error('[WEEK_PLAN_PAGE] Reload failed:', response.status);
      }
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Reload error:', err);
    }
  }, [
    userId,
    weeklyPlan?.startDate,
    normalizeWeekStart,
    fetchWeeklyTrends,
    fetchMonthlyTrends,
    fetchWeeklyVersions,
  ]);

  useEffect(() => {
    if (!location.state?.refreshWeeklyPlan) {
      locationRefreshHandledRef.current = false;
      return;
    }
    if (locationRefreshHandledRef.current) return;
    locationRefreshHandledRef.current = true;
    const runRefresh = async () => {
      const now = new Date();
      const weekStart = normalizeWeekStart(weeklyPlan?.startDate, now);
      const startDate = normalizeWeekStart(weeklyPlan?.startDate || weekStart, now);
      lastWeeklyTrendStartRef.current = null;
      lastMonthlyTrendStartRef.current = null;
      await reloadWeeklyPlan(startDate);
      navigate(location.pathname, { replace: true, state: {} });
    };
    runRefresh();
  }, [location.state, weeklyPlan, reloadWeeklyPlan, navigate, location.pathname, normalizeWeekStart]);

  const handleRestoreVersion = async (versionNumber) => {
    if (!userId || !weeklyPlan?.startDate) return;
    setRestoringVersion(versionNumber);
    setVersionsError(null);
    try {
      const url = `http://localhost:5173/api/meal-plans/week/versions/restore?userId=${userId}&startDate=${weeklyPlan.startDate}&versionNumber=${versionNumber}`;
      const response = await fetch(url, { method: 'POST' });
      if (!response.ok) {
        throw new Error(`Failed to restore version (${response.status})`);
      }
      await reloadWeeklyPlan(weeklyPlan.startDate);
      await fetchWeeklyVersions(weeklyPlan.startDate);
    } catch (err) {
      setVersionsError(err?.message || 'Failed to restore plan version.');
    } finally {
      setRestoringVersion(null);
    }
  };

  const handleDeleteVersion = async (versionNumber) => {
    if (!userId || !weeklyPlan?.startDate) return;
    const confirmed = window.confirm(`Delete version v${versionNumber}? This cannot be undone.`);
    if (!confirmed) return;
    setDeletingVersion(versionNumber);
    setVersionsError(null);
    try {
      const url = `http://localhost:5173/api/meal-plans/week/versions?userId=${userId}&startDate=${weeklyPlan.startDate}&versionNumber=${versionNumber}`;
      const response = await fetch(url, { method: 'DELETE' });
      if (!response.ok) {
        throw new Error(`Failed to delete version (${response.status})`);
      }
      await fetchWeeklyVersions(weeklyPlan.startDate);
    } catch (err) {
      setVersionsError(err?.message || 'Failed to delete plan version.');
    } finally {
      setDeletingVersion(null);
    }
  };

  /**
   * Refresh weekly meal plan
   */
  const handleRefreshWeeklyPlan = async () => {
    if (!userId) return;
    
    console.log('[WEEK_PLAN_PAGE] Manual refresh triggered by user');
    setRefreshing(true);
    try {
      const now = new Date();
      const weekStart = normalizeWeekStart(weeklyPlan?.startDate, now);
      const startDate = normalizeWeekStart(weeklyPlan?.startDate || weekStart, now);
      
      const weekUrl = `http://localhost:5173/api/meal-plans/week/refresh?userId=${userId}&startDate=${startDate}`;
      const response = await fetch(weekUrl, { method: 'POST' });

      if (response.ok) {
        await reloadWeeklyPlan(startDate);
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

  const handleChooseRecipe = (mealId) => {
    if (!mealId) return;
    navigate(`/meals/replace/${mealId}?returnTo=weekly`);
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

  const handleMoveMeal = async (dayDate, mealId, direction) => {
    if (!mealId || !dayDate) return;
    try {
      setMovingMealId(mealId);
      const token = getAccessToken();
      const updatedDay = await api.moveMeal(token, mealId, direction);

      setWeeklyPlan(prevPlan => ({
        ...prevPlan,
        days: prevPlan.days.map(day => {
          if (day.date !== dayDate) return day;
          return updatedDay;
        })
      }));
    } catch (err) {
      console.error('[MEAL_MOVE] Error:', err.message);
      setError(`Failed to move meal: ${err.message}`);
    } finally {
      setMovingMealId(null);
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
        body: JSON.stringify({ userId, mealType, mealId: meal.id })
      });

      if (!response.ok) {
        const errBody = await response.json().catch(() => ({}));
        throw new Error(errBody.error || `Failed to generate AI recipe (${response.status})`);
      }

      await reloadWeeklyPlan(weeklyPlan?.startDate);
    } catch (err) {
      console.error('[AI_RECIPE] Error:', err);
      setError(`Failed to generate AI recipe: ${err.message}`);
    } finally {
      setGeneratingMealId(null);
    }
  };

  const handleOpenCustomMealForm = (dayDate) => {
    setCustomMealError(null);
    setCustomMealName('');
    setCustomMealType('breakfast');
    setCustomMealFormDate(dayDate);
  };

  const handleAddCustomMeal = async (dayDate) => {
    if (!userId || !dayDate) return;
    if (!customMealName.trim()) {
      setCustomMealError('Meal name is required');
      return;
    }

    setCustomMealLoading(true);
    setCustomMealError(null);
    try {
      const response = await fetch(
        `http://localhost:5173/api/meal-plans/meals/custom?userId=${userId}&duration=weekly`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            date: dayDate,
            meal_type: customMealType,
            name: customMealName,
          }),
        }
      );

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.error || 'Failed to add custom meal');
      }

      await reloadWeeklyPlan(weeklyPlan?.startDate);
      setCustomMealName('');
      setCustomMealFormDate(null);
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Custom meal add error:', err);
      setCustomMealError(err.message || 'Failed to add custom meal');
    } finally {
      setCustomMealLoading(false);
    }
  };

  const handleDeleteCustomMeal = async (mealId) => {
    if (!userId || !mealId) return;
    if (!window.confirm('Delete this custom meal?')) return;

    setCustomMealLoading(true);
    setCustomMealError(null);
    try {
      const response = await fetch(
        `http://localhost:5173/api/meal-plans/meals/custom/${mealId}?userId=${userId}`,
        { method: 'DELETE' }
      );
      if (!response.ok) {
        throw new Error('Failed to delete custom meal');
      }
      await reloadWeeklyPlan(weeklyPlan?.startDate);
    } catch (err) {
      console.error('[WEEK_PLAN_PAGE] Custom meal delete error:', err);
      setCustomMealError(err.message || 'Failed to delete custom meal');
    } finally {
      setCustomMealLoading(false);
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
  const trendDays = trendData?.days || [];
  const maxAbsDelta = trendDays.length > 0
    ? Math.max(...trendDays.map(day => Math.abs(day.delta)))
    : 0;

  const getNegativeTrendColor = (delta) => {
    if (delta >= 0) return null;
    const ratio = maxAbsDelta > 0 ? Math.min(1, Math.abs(delta) / maxAbsDelta) : 0;
    const start = [255, 152, 0]; // orange
    const end = [229, 57, 53]; // red
    const r = Math.round(start[0] + (end[0] - start[0]) * ratio);
    const g = Math.round(start[1] + (end[1] - start[1]) * ratio);
    const b = Math.round(start[2] + (end[2] - start[2]) * ratio);
    return `rgb(${r}, ${g}, ${b})`;
  };
  const monthlyMaxAbsDelta = monthlyTrendDays.length > 0
    ? Math.max(...monthlyTrendDays.map(day => Math.abs(day.delta)))
    : 0;

  const formatShortDate = (value) => {
    if (!value) return '';
    const parts = value.split('-');
    if (parts.length !== 3) return value;
    return `${parts[1]}.${parts[2]}`;
  };

  const formatVersionReason = (reason) => {
    if (!reason) return 'unknown';
    return String(reason).replace(/_/g, ' ');
  };

  const formatVersionTimestamp = (value) => {
    if (!value) return '';
    return String(value);
  };

  const buildWeekLabels = (daysList) => {
    if (!daysList.length) return [];
    const labels = [];
    for (let i = 0; i < daysList.length; i += 7) {
      const start = daysList[i];
      const end = daysList[Math.min(i + 6, daysList.length - 1)];
      if (start?.date && end?.date) {
        labels.push(`${formatShortDate(start.date)}–${formatShortDate(end.date)}`);
      }
    }
    return labels;
  };

  const renderTrendLine = (daysList, maxAbs, labelsOverride = null) => {
    if (!daysList.length) {
      return <div className="weekly-trend-empty">No trend data available.</div>;
    }

    const width = 600;
    const height = 160;
    const padding = 20;
    const mid = height / 2;
    const scale = maxAbs > 0 ? (mid - padding) / maxAbs : 1;

    const points = daysList.map((day, idx) => {
      const x = padding + (idx / Math.max(daysList.length - 1, 1)) * (width - padding * 2);
      const y = mid - (day.delta || 0) * scale;
      return `${x},${y}`;
    });

    return (
      <div className="trend-line-wrapper">
        <svg viewBox={`0 0 ${width} ${height}`} className="trend-line-svg">
          <line x1={padding} y1={mid} x2={width - padding} y2={mid} className="trend-line-axis" />
          <polyline points={points.join(' ')} className="trend-line-path" />
        </svg>
        <div className="trend-line-labels">
          {(labelsOverride || []).length > 0
            ? labelsOverride.map((label, idx) => <span key={`${label}-${idx}`}>{label}</span>)
            : daysList.map((day) => <span key={day.date}>{formatShortDate(day.date)}</span>)}
        </div>
      </div>
    );
  };

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
              className="btn-shopping-list"
              onClick={() => {
                const startDate = weeklyPlan?.startDate;
                if (startDate) {
                  navigate(`/shopping-list/week?startDate=${startDate}`);
                } else {
                  navigate('/shopping-list/week');
                }
              }}
              title="Weekly shopping list"
            >
              🧺 Shopping List
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

      {(weeklyInsightsLoading || weeklyInsights || weeklyInsightsError) && (
        <div className="weekly-ai-insights">
          <h2>AI Weekly Nutrition Insights</h2>
          {weeklyInsightsLoading ? (
            <p className="weekly-ai-loading">Analyzing weekly nutrition…</p>
          ) : weeklyInsightsError ? (
            <p className="weekly-ai-loading">{weeklyInsightsError}</p>
          ) : (
            <p className="weekly-ai-text">{weeklyInsights}</p>
          )}
        </div>
      )}

      {(versionsLoading || versionsError || versionHistory) && (
        <div className="weekly-versions">
          <div className="weekly-versions-header">
            <h2>Plan Versions</h2>
            <p className="weekly-versions-subtitle">Select an earlier weekly snapshot</p>
          </div>

          {versionsLoading ? (
            <p className="weekly-versions-loading">Loading versions…</p>
          ) : versionsError ? (
            <p className="weekly-versions-error">{versionsError}</p>
          ) : versionHistory?.versions?.length ? (
            <div className="weekly-versions-list">
              {versionHistory.versions.map((version) => {
                const isCurrent = versionHistory.currentVersionNumber === version.versionNumber;
                return (
                  <div
                    key={version.versionNumber}
                    className={`weekly-version-row ${isCurrent ? 'current' : ''}`}
                  >
                    <div className="weekly-version-meta">
                      <span className="weekly-version-number">v{version.versionNumber}</span>
                      <span className="weekly-version-reason">{formatVersionReason(version.reason)}</span>
                      <span className="weekly-version-date">{formatVersionTimestamp(version.createdAt)}</span>
                      <span className="weekly-version-days">{version.dayCount} days</span>
                    </div>
                    <div className="weekly-version-actions">
                      {isCurrent ? (
                        <span className="weekly-version-current">Current</span>
                      ) : (
                        <>
                          <button
                            className="weekly-version-restore"
                            onClick={() => handleRestoreVersion(version.versionNumber)}
                            disabled={restoringVersion === version.versionNumber || deletingVersion === version.versionNumber}
                          >
                            {restoringVersion === version.versionNumber ? 'Selecting…' : 'Select'}
                          </button>
                          <button
                            className="weekly-version-delete"
                            onClick={() => handleDeleteVersion(version.versionNumber)}
                            disabled={deletingVersion === version.versionNumber || restoringVersion === version.versionNumber}
                          >
                            {deletingVersion === version.versionNumber ? 'Deleting…' : 'Delete'}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="weekly-versions-empty">No version history available.</p>
          )}
        </div>
      )}

      {/* Weekly Calorie Balance */}
      <div className="weekly-trend-section">
        <div className="weekly-trend-header">
          <h2>Weekly Calorie Balance</h2>
          <p className="weekly-trend-subtitle">Estimated based on your meal plan</p>
        </div>

        {trendError && (
          <div className="weekly-trend-error">
            {trendError}
          </div>
        )}

        {trendDays.length > 0 ? (
          <div className="weekly-trend-chart">
            {trendDays.map((day) => {
              const delta = day.delta || 0;
              const barHeight = maxAbsDelta > 0
                ? Math.max(32, Math.round((Math.abs(delta) / maxAbsDelta) * 240))
                : 48;
              const barClass = delta >= 0 ? 'trend-bar positive' : 'trend-bar negative';
              const barColor = delta < 0 ? getNegativeTrendColor(delta) : null;
              return (
                <div key={day.date} className="trend-day">
                  <div
                    className={barClass}
                    style={{ height: `${barHeight}px`, backgroundColor: barColor || undefined }}
                    title={`Actual: ${day.actualCalories} kcal / Target: ${day.targetCalories} kcal`}
                  ></div>
                  <div className="trend-date">{day.date}</div>
                  <div className="trend-delta">{delta > 0 ? `+${delta}` : delta}</div>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="weekly-trend-empty">
            No trend data available.
          </div>
        )}
      </div>

      {/* Monthly Calorie Trend */}
      <div className="weekly-trend-section">
        <div className="weekly-trend-header">
          <h2>Monthly Calorie Trend</h2>
          <p className="weekly-trend-subtitle">Last 4 weeks combined</p>
        </div>

        {monthlyTrendError && (
          <div className="weekly-trend-error">
            {monthlyTrendError}
          </div>
        )}

        {renderTrendLine(monthlyTrendDays, monthlyMaxAbsDelta, buildWeekLabels(monthlyTrendDays))}
      </div>

      {/* Days List */}
      <div className="weekly-days">
        {days.map((day, index) => (
          <div key={index} className="day-card">
            <div className="day-header">
              <div className="day-title-row">
                <h2 className="day-date">{day.date}</h2>
                <button
                  className="day-add-btn"
                  onClick={() => handleOpenCustomMealForm(day.date)}
                  title="Add custom meal"
                >
                  +
                </button>
              </div>
              {day.context_hash && (
                <span className="day-hash">{day.context_hash.substring(0, 8)}</span>
              )}
            </div>

            {customMealFormDate === day.date && (
              <div className="custom-meal-inline">
                {customMealError && <div className="custom-meal-error">{customMealError}</div>}
                <div className="custom-meal-row">
                  <input
                    type="text"
                    placeholder="Custom meal name"
                    value={customMealName}
                    onChange={(e) => setCustomMealName(e.target.value)}
                    disabled={customMealLoading}
                    className="custom-meal-input"
                  />
                  <select
                    value={customMealType}
                    onChange={(e) => setCustomMealType(e.target.value)}
                    disabled={customMealLoading}
                    className="custom-meal-select"
                  >
                    <option value="breakfast">Breakfast</option>
                    <option value="lunch">Lunch</option>
                    <option value="dinner">Dinner</option>
                    <option value="snack">Snack</option>
                  </select>
                  <button
                    className="custom-meal-btn"
                    onClick={() => handleAddCustomMeal(day.date)}
                    disabled={customMealLoading}
                  >
                    {customMealLoading ? 'Adding...' : 'Add'}
                  </button>
                  <button
                    className="custom-meal-btn secondary"
                    onClick={() => setCustomMealFormDate(null)}
                    disabled={customMealLoading}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}

            {day.meals && day.meals.length > 0 ? (
              <div className="day-meals">
                <div className="meals-list">
                  {sortMealsByTime(day.meals).map((meal, mealIndex, arr) => {
                    const isCustom = meal.is_custom || meal.isCustom;
                    const recipeId = meal.recipe_id || meal.recipeId;
                    return (
                      <div key={meal.id || mealIndex} className="meal-item-wrapper">
                        <div className="meal-item">
                          <span className="meal-type-inline">{formatMealType(meal.meal_type || meal.mealType)}</span>
                          <span className="meal-name">
                            {meal.custom_meal_name || meal.customMealName || meal.meal_name || '(Meal)'}
                          </span>
                      </div>
                      <div className="meal-actions">
                        <span className="meal-calories-inline">
                          {getMealCalories(meal) != null ? `${getMealCalories(meal)} kcal` : '—'}
                        </span>
                        <button
                          className="meal-btn meal-btn-move"
                          onClick={() => handleMoveMeal(day.date, meal.id, 'up')}
                            disabled={!meal.id || mealIndex === 0 || movingMealId === meal.id}
                            title="Move up"
                          >
                            ↑
                          </button>
                          <button
                            className="meal-btn meal-btn-move"
                            onClick={() => handleMoveMeal(day.date, meal.id, 'down')}
                            disabled={!meal.id || mealIndex === arr.length - 1 || movingMealId === meal.id}
                            title="Move down"
                          >
                            ↓
                          </button>
                          {!isCustom && (
                            <button
                              className="meal-btn meal-btn-info"
                              onClick={() => navigate(`/recipes/${recipeId || 'unknown'}`)}
                              disabled={!recipeId}
                              title="View recipe details"
                            >
                              ?
                            </button>
                          )}
                        {!isCustom && (
                          <button
                            className="meal-btn meal-btn-choose"
                            onClick={() => handleChooseRecipe(meal.id)}
                            disabled={!meal.id}
                            title={meal.id ? 'Choose another recipe' : 'Meal ID missing'}
                          >
                            ≡
                          </button>
                        )}
                        {!isCustom && (
                          <button
                            className="meal-btn meal-btn-ai"
                            onClick={() => handleGenerateAiRecipe(meal)}
                            disabled={generatingMealId === (meal.id || (meal.meal_type || meal.mealType))}
                            title="Generate AI recipe"
                          >
                            AI
                          </button>
                        )}
                        {meal.id && (
                          <button
                            className="meal-btn meal-btn-shopping"
                            onClick={() => navigate(`/shopping-list/meal?mealId=${meal.id}`)}
                            title="Meal shopping list"
                          >
                            🧺
                          </button>
                        )}
                          {isCustom && (
                            <button
                              className="meal-btn meal-btn-delete"
                              onClick={() => handleDeleteCustomMeal(meal.id)}
                              disabled={!meal.id || customMealLoading}
                              title="Delete custom meal"
                            >
                              🗑
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
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

/**
 * Sort meals by planned time (fallback to meal type order)
 */
function sortMealsByTime(meals) {
  const mealOrder = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
  return [...meals].sort((a, b) => {
    const aTime = a.planned_time || a.plannedTime;
    const bTime = b.planned_time || b.plannedTime;
    if (aTime && bTime) return aTime.localeCompare(bTime);
    const aType = (a.meal_type || a.mealType || 'OTHER').toUpperCase();
    const bType = (b.meal_type || b.mealType || 'OTHER').toUpperCase();
    return mealOrder.indexOf(aType) - mealOrder.indexOf(bType);
  });
}
