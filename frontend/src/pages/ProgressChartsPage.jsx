import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/ProgressCharts.css';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';

export default function ProgressChartsPage() {
  const navigate = useNavigate();
  const [trendDays, setTrendDays] = useState([]);
  const [monthlyTrendDays, setMonthlyTrendDays] = useState([]);
  const [weeklyInsights, setWeeklyInsights] = useState(null);
  const [weeklyInsightsLoading, setWeeklyInsightsLoading] = useState(false);
  const [error, setError] = useState(null);

  const getWeekStart = (date) => {
    const d = new Date(date);
    const day = d.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    d.setDate(d.getDate() + diff);
    return d.toLocaleDateString('en-CA');
  };

  const formatShortDate = (value) => {
    if (!value) return '';
    const parts = value.split('-');
    if (parts.length !== 3) return value;
    return `${parts[1]}.${parts[2]}`;
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
      return <div className="progress-empty">No trend data available.</div>;
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

  useEffect(() => {
    const loadProgress = async () => {
      const token = getAccessToken();
      if (!token) return;
      setError(null);
      try {
        const me = await api.me(token);
        const user = me?.user || me;
        const userId = user?.id;
        if (!userId) {
          throw new Error('No userId');
        }

        const today = new Date();
        const weekStart = getWeekStart(today);
        const weekUrl = `http://localhost:5173/api/meal-plans/week/trends?userId=${userId}&startDate=${weekStart}`;
        const weekRes = await fetch(weekUrl);
        if (weekRes.ok) {
          const data = await weekRes.json();
          setTrendDays(data?.days || []);
        }

        const baseDate = new Date(weekStart);
        const weekStarts = Array.from({ length: 4 }, (_, idx) => {
          const d = new Date(baseDate);
          d.setDate(baseDate.getDate() + idx * 7);
          return d.toLocaleDateString('en-CA');
        });

        const monthResponses = await Promise.all(
          weekStarts.map((start) =>
            fetch(`http://localhost:5173/api/meal-plans/week/trends?userId=${userId}&startDate=${start}`)
          )
        );
        const payloads = await Promise.all(
          monthResponses.map((response) => (response.ok ? response.json() : null))
        );

        const merged = [];
        payloads.forEach((data) => {
          if (data?.days?.length) {
            merged.push(...data.days);
          }
        });
        setMonthlyTrendDays(merged);

        const weeklyPlanRes = await fetch(
          `http://localhost:5173/api/meal-plans/week?userId=${userId}&startDate=${weekStart}`
        );
        if (weeklyPlanRes.ok) {
          const weeklyPlan = await weeklyPlanRes.json();
          const dailySummaries = (weeklyPlan?.days || []).map((day) => ({
            date: day.date,
            calories: day?.nutrition?.total_calories ?? 0,
            targetCalories: weeklyPlan?.weeklyNutrition?.targetCalories ?? 0,
          }));

          setWeeklyInsightsLoading(true);
          try {
            const response = await fetch('http://localhost:5173/api/ai/nutrition/weekly-insights', {
              method: 'POST',
              headers: { 'Content-Type': 'application/json' },
              body: JSON.stringify({
                userId,
                startDate: weeklyPlan.startDate,
                endDate: weeklyPlan.endDate,
                userGoal: weeklyPlan?.goal || 'maintenance',
                weeklyNutrition: {
                  totalCalories: weeklyPlan?.weeklyNutrition?.totalCalories ?? 0,
                  targetCalories: weeklyPlan?.weeklyNutrition?.targetCalories ?? 0,
                  totalProtein: weeklyPlan?.weeklyNutrition?.totalProtein ?? 0,
                  targetProtein: weeklyPlan?.weeklyNutrition?.targetProtein ?? 0,
                  totalCarbs: weeklyPlan?.weeklyNutrition?.totalCarbs ?? 0,
                  targetCarbs: weeklyPlan?.weeklyNutrition?.targetCarbs ?? 0,
                  totalFats: weeklyPlan?.weeklyNutrition?.totalFats ?? 0,
                  targetFats: weeklyPlan?.weeklyNutrition?.targetFats ?? 0,
                  nutritionEstimated: !!weeklyPlan?.weeklyNutrition?.nutritionEstimated,
                },
                dailySummaries,
              }),
            });
          if (response.ok) {
            const data = await response.json();
            setWeeklyInsights(data?.summary || null);
          } else {
            setWeeklyInsights('AI insights temporarily unavailable.');
          }
        } finally {
          setWeeklyInsightsLoading(false);
        }
        }
      } catch (err) {
        console.warn('Progress charts load failed', err);
        setError('Failed to load progress charts');
      }
    };

    loadProgress();
  }, []);

  const weeklyAdjustedDays = useMemo(() => {
    const today = new Date();
    const cutoff = new Date(today.getFullYear(), today.getMonth(), today.getDate());
    return trendDays.map((day) => {
      const dayDate = new Date(`${day.date}T00:00:00`);
      if (dayDate > cutoff) {
        return { ...day, delta: 0 };
      }
      return day;
    });
  }, [trendDays]);

  const weeklyMaxAbs = useMemo(() => {
    return weeklyAdjustedDays.length
      ? Math.max(...weeklyAdjustedDays.map((day) => Math.abs(day.delta || 0)))
      : 0;
  }, [weeklyAdjustedDays]);

  const monthlyMaxAbs = useMemo(() => {
    return monthlyTrendDays.length
      ? Math.max(...monthlyTrendDays.map((day) => Math.abs(day.delta || 0)))
      : 0;
  }, [monthlyTrendDays]);

  return (
    <div className="progress-wrap">
      <div className="progress-header">
        <div>
          <h1>Progress Charts</h1>
          <p>Historical nutrition trends and insights</p>
        </div>
        <button onClick={() => navigate('/dashboard')}>Back to Dashboard</button>
      </div>

      {error && <div className="progress-error">{error}</div>}

      <section className="progress-card">
        <h2>Weekly Calorie Trend</h2>
        {renderTrendLine(weeklyAdjustedDays, weeklyMaxAbs, buildWeekLabels(weeklyAdjustedDays))}
      </section>

      <section className="progress-card">
        <h2>Monthly Calorie Trend</h2>
        {renderTrendLine(monthlyTrendDays, monthlyMaxAbs, buildWeekLabels(monthlyTrendDays))}
      </section>

      <section className="progress-card">
        <h2>AI Weekly Nutrition Insights</h2>
        {weeklyInsightsLoading ? (
          <p>Analyzing weekly nutrition…</p>
        ) : (
          <p>{weeklyInsights || 'AI insights temporarily unavailable.'}</p>
        )}
      </section>
    </div>
  );
}
