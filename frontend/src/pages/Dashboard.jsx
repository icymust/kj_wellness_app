import React, { useEffect, useMemo, useState } from 'react';
import '../styles/Dashboard.css';
import { getAccessToken } from '../lib/tokens';
import { api } from '../lib/api';

// Простой скелетон для предотвращения "дёрганья" текста загрузки.
function SkeletonBlock({ lines = 3, height = 14 }) {
  return (
    <div style={{ display:'grid', gap:6 }}>
      {Array.from({ length: lines }).map((_,i) => (
        <div key={i} style={{
          height,
          background: 'linear-gradient(90deg,#eee,#f5f5f5,#eee)',
          backgroundSize: '200% 100%',
          animation: 'ndl-skel 1.2s ease-in-out infinite'
        }} />
      ))}
    </div>
  );
}

function DashboardComparison({ summary, week, monthData, ai }) {
  if (!summary) return null;
  const current = summary.latestWeightKg;
  const target = summary.goal?.targetWeightKg;
  const initial = summary.goal?.initialWeightKg;
  const percent = summary.goal?.progress?.percent ?? null;
  const bmiVal = summary.bmi?.value;
  const bmiClass = summary.bmi?.classification;

  // derive weekly/month objects (lightweight) from existing week/monthData
  const weeklySummary = week ? {
    sessions: week.sessions,
    minutes: week.totalMinutes,
    daysActive: week.daysActive,
    trendText: week.sessions > 0 ? `Active ${week.sessions} sessions, ${week.totalMinutes} min` : 'No activity logged this week'
  } : null;
  const monthlySummary = monthData ? {
    sessions: monthData.daysActive, // approximation (sessions not tracked separately)
    minutes: monthData.total,
    daysActive: monthData.daysActive,
    trendText: monthData.total > 0 ? `Total ${monthData.total} min across ${monthData.daysActive} active days` : 'No activity logged this month'
  } : null;

  // simple weight deltas
  let weightDeltaWeek = null;
  let weightDeltaMonth = null;
  // For brevity we skip timeframe filtering; placeholder uses current vs initial/target heuristics.
  if (initial != null && current != null) {
    weightDeltaWeek = current - initial; // placeholder
    weightDeltaMonth = current - initial; // placeholder — could refine by timestamps
  }

  return (
    <section className="comparison-grid">
      <div className="card">
        <h2 style={{ marginTop:0 }}>Comparison view</h2>
        <div style={{ display:'grid', gap:12, marginTop:8 }}>
          <div>
            <strong>Weight</strong>
            <div style={{ fontSize:14 }}>
              Current: {current ?? '—'} kg · Target: {target ?? '—'} kg {initial != null && (<span>· Initial: {initial} kg</span>)}
            </div>
            {percent != null && percent !== 0 && (
              <div style={{ fontSize:13 }}>Progress: {percent.toFixed(1)}% toward goal</div>
            )}
          </div>
          <div>
            <strong>BMI</strong>
            <div style={{ fontSize:14 }}>{bmiVal ?? '—'} {bmiClass && (<span>({bmiClass})</span>)}</div>
          </div>
          <div>
            <strong>Weekly vs monthly</strong>
            <div style={{ fontSize:13 }}>
              <div>Week: {weeklySummary ? `${weeklySummary.sessions} sessions · ${weeklySummary.minutes} min` : '—'} {weightDeltaWeek != null && weightDeltaWeek !== 0 && (`· Δ weight ${weightDeltaWeek.toFixed(1)} kg`)}</div>
              <div>Month: {monthlySummary ? `${monthlySummary.sessions} active days · ${monthlySummary.minutes} min` : '—'} {weightDeltaMonth != null && weightDeltaMonth !== 0 && (`· Δ weight ${weightDeltaMonth.toFixed(1)} kg`)}</div>
            </div>
          </div>
          <div>
            <strong>Trends</strong>
            <div style={{ fontSize:13 }}>{weeklySummary?.trendText || monthlySummary?.trendText || 'Trend analysis pending data'}</div>
          </div>
        </div>
      </div>
      <div className="card">
        <h3 style={{ marginTop:0 }}>AI recommendations</h3>
        {ai?.items?.length ? (
          <ul style={{ listStyle:'none', padding:0, margin:0, marginTop:8 }}>
            {ai.items.slice(0,3).map(item => (
              <li key={item.id || item.title} style={{ marginBottom:8, padding:8, border:'1px solid #eee', borderRadius:6 }}>
                <div style={{ fontSize:11, opacity:0.6 }}>Priority: {item.priority ?? 'n/a'}</div>
                <div style={{ fontWeight:600 }}>{item.title || 'Recommendation'}</div>
                <div style={{ fontSize:13 }}>{item.detail || item.text || '—'}</div>
              </li>
            ))}
          </ul>
        ) : !dailyNutrition ? (
          <div style={{ fontSize: 13 }}>Daily nutrition unavailable</div>
        ) : (
          <div style={{ fontSize:13, marginTop:8 }}>No insights yet. Generate weekly/monthly AI insights.</div>
        )}
      </div>
    </section>
  );
}

export default function Dashboard({ ctx }) {
  const { summary, week, monthData, ai, rateLimitUntil, dashboardLoading, dashboardError, loadDashboard } = ctx;
  const hasData = !!(summary || week || monthData || ai);
  const isRateLimited = !!(rateLimitUntil && Date.now() < rateLimitUntil);
  const [dailyNutrition, setDailyNutrition] = useState(null);
  const [dailyLoading, setDailyLoading] = useState(false);
  const [dailyError, setDailyError] = useState(null);
  const [dailyAiSummary, setDailyAiSummary] = useState(null);
  const [dailyAiSuggestions, setDailyAiSuggestions] = useState([]);
  const [dailyAiLoading, setDailyAiLoading] = useState(false);
  const [dailyAiError, setDailyAiError] = useState(null);
  const [profileData, setProfileData] = useState(null);
  const [prefsData, setPrefsData] = useState(null);

  // Initial aggregated load
  useEffect(() => { loadDashboard(); }, [loadDashboard]);

  // Auto reload after rate-limit window expires
  useEffect(() => {
    if (!rateLimitUntil) return;
    const now = Date.now();
    if (now >= rateLimitUntil) { loadDashboard(true); return; }
    const id = setTimeout(() => loadDashboard(true), rateLimitUntil - now + 150);
    return () => clearTimeout(id);
  }, [rateLimitUntil, loadDashboard]);

  const comparisonAi = ai;

  useEffect(() => {
    const loadDailyNutrition = async () => {
      const token = getAccessToken();
      if (!token) return;
      setDailyLoading(true);
      setDailyError(null);
      try {
        const me = await api.me(token);
        const user = me?.user || me;
        const userId = user?.id;
        if (!userId) {
          throw new Error('No userId');
        }
        const today = new Date().toLocaleDateString('en-CA');
        const response = await fetch(
          `http://localhost:5173/api/meal-plans/day/nutrition?userId=${userId}&date=${today}`
        );
        if (!response.ok) {
          throw new Error('Failed to load daily nutrition');
        }
        const data = await response.json();
        setDailyNutrition({ ...data, date: today, userId });
      } catch (err) {
        setDailyError('Daily nutrition unavailable');
        setDailyNutrition(null);
      } finally {
        setDailyLoading(false);
      }
    };
    loadDailyNutrition();
  }, []);

  useEffect(() => {
    const loadProfileInputs = async () => {
      const token = getAccessToken();
      if (!token) return;
      try {
        const profileResp = await api.getProfile(token);
        setProfileData(profileResp?.profile || null);
        const prefsResp = await api.getNutritionalPreferences(token);
        setPrefsData(prefsResp?.nutritionalPreferences || null);
      } catch (err) {
        setProfileData(null);
        setPrefsData(null);
      }
    };
    loadProfileInputs();
  }, []);

  useEffect(() => {
    const loadAiDaily = async () => {
      if (!dailyNutrition || dailyNutrition.unavailable) {
        setDailyAiSummary(null);
        setDailyAiSuggestions([]);
        return;
      }
      setDailyAiLoading(true);
      setDailyAiError(null);
      try {
        const summaryRes = await fetch('http://localhost:5173/api/ai/nutrition/summary', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId: dailyNutrition.userId,
            date: dailyNutrition.date,
            userGoal: summary?.goal?.type || 'general_fitness',
            nutritionSummary: {
              calories: dailyNutrition.total_calories ?? 0,
              targetCalories: dailyNutrition.target_calories ?? 0,
              protein: dailyNutrition.total_protein ?? 0,
              targetProtein: dailyNutrition.target_protein ?? 0,
              carbs: dailyNutrition.total_carbs ?? 0,
              targetCarbs: dailyNutrition.target_carbs ?? 0,
              fats: dailyNutrition.total_fats ?? 0,
              targetFats: dailyNutrition.target_fats ?? 0,
              nutritionEstimated: !!dailyNutrition.nutrition_estimated,
            },
          }),
        });
        if (summaryRes.ok) {
          const data = await summaryRes.json();
          setDailyAiSummary(data?.summary || null);
        }

        const suggestionsRes = await fetch('http://localhost:5173/api/ai/nutrition/suggestions', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            userId: dailyNutrition.userId,
            date: dailyNutrition.date,
            userGoal: summary?.goal?.type || 'general_fitness',
            dietaryPreferences: [],
            meals: [],
            nutritionSummary: {
              calories: dailyNutrition.total_calories ?? 0,
              targetCalories: dailyNutrition.target_calories ?? 0,
              protein: dailyNutrition.total_protein ?? 0,
              targetProtein: dailyNutrition.target_protein ?? 0,
              carbs: dailyNutrition.total_carbs ?? 0,
              targetCarbs: dailyNutrition.target_carbs ?? 0,
              fats: dailyNutrition.total_fats ?? 0,
              targetFats: dailyNutrition.target_fats ?? 0,
              nutritionEstimated: !!dailyNutrition.nutrition_estimated,
            },
          }),
        });
        if (suggestionsRes.ok) {
          const data = await suggestionsRes.json();
          setDailyAiSuggestions(Array.isArray(data?.suggestions) ? data.suggestions : []);
        }
      } catch (err) {
        setDailyAiError('AI insights temporarily unavailable');
      } finally {
        setDailyAiLoading(false);
      }
    };
    loadAiDaily();
  }, [dailyNutrition, summary]);

  const calorieProgress = useMemo(() => {
    const total = dailyNutrition?.total_calories ?? 0;
    const target = dailyNutrition?.target_calories ?? 0;
    const pct = target > 0 ? Math.min((total / target) * 100, 100) : 0;
    return { total, target, pct, isSurplus: total > target };
  }, [dailyNutrition]);

  const estimatedTargets = useMemo(() => {
    if (!profileData) return null;
    const weight = Number(profileData.weightKg || profileData.weight_kg);
    const height = Number(profileData.heightCm || profileData.height_cm);
    const age = Number(profileData.age);
    const gender = (profileData.gender || 'male').toLowerCase();
    if (!weight || !height || !age) return null;

    const bmr =
      gender === 'female'
        ? 10 * weight + 6.25 * height - 5 * age - 161
        : 10 * weight + 6.25 * height - 5 * age + 5;

    const activityMap = {
      low: 1.2,
      sedentary: 1.2,
      moderate: 1.375,
      medium: 1.375,
      high: 1.55,
      very_high: 1.725,
    };
    const activityFactor = activityMap[profileData.activityLevel] || 1.2;
    let targetCalories = bmr * activityFactor;

    const goal = profileData.goal || 'general_fitness';
    if (goal === 'weight_loss') targetCalories -= 300;
    if (goal === 'weight_gain') targetCalories += 300;

    const proteinTarget = Math.round(weight * 1.6);
    const fatsTarget = Math.round((targetCalories * 0.3) / 9);
    const carbsTarget = Math.round((targetCalories * 0.4) / 4);

    return {
      targetCalories: Math.round(targetCalories),
      proteinTarget,
      carbsTarget,
      fatsTarget,
      activityFactor,
    };
  }, [profileData]);

  const wellnessScore = useMemo(() => {
    const total = dailyNutrition?.total_calories ?? 0;
    const target = dailyNutrition?.target_calories ?? 0;
    if (!target) return null;
    const diffPct = Math.min(Math.abs(total - target) / target, 1);
    const calorieScore = Math.round((1 - diffPct) * 70);
    const macroScore = [
      dailyNutrition?.total_protein,
      dailyNutrition?.total_carbs,
      dailyNutrition?.total_fats,
    ].every((v) => typeof v === 'number')
      ? 30
      : 10;
    return Math.min(100, Math.max(0, calorieScore + macroScore));
  }, [dailyNutrition]);

  return (
    <div className="dashboard-wrap">
      <h1 style={{ margin:0 }}>Dashboard</h1>
      {isRateLimited && (
        <div className="rate-limit">
          Rate limit exceeded. We will refresh data in {Math.ceil((rateLimitUntil - Date.now())/1000)}s.
        </div>
      )}
      {(!isRateLimited && dashboardLoading && !hasData) ? (
        <div className="skeleton">
          <div className="skeleton-note">Loading data…</div>
          <SkeletonBlock lines={4} />
        </div>
      ) : null}
      {dashboardError && !hasData && !dashboardLoading && !isRateLimited && (
        <div className="error-box">
          Load error: {dashboardError}
        </div>
      )}
      <div className="card">
        <h2 style={{ marginTop: 0 }}>Personalization Inputs</h2>
        <div style={{ fontSize: 13, color: '#555', marginBottom: 8 }}>
          These profile signals are used to personalize targets and meal planning.
        </div>
        <div className="daily-macro-grid">
          <div>BMI: {summary?.bmi?.value ?? '—'} {summary?.bmi?.classification ? `(${summary.bmi.classification})` : ''}</div>
          <div>Goal: {profileData?.goal ?? '—'}</div>
          <div>Activity level: {profileData?.activityLevel ?? '—'}</div>
          <div>Target weight: {profileData?.targetWeightKg ?? '—'} kg</div>
          <div>Calorie target: {prefsData?.calorieTarget ?? '—'} kcal</div>
          <div>Macro targets: P {prefsData?.proteinTarget ?? '—'}g · C {prefsData?.carbsTarget ?? '—'}g · F {prefsData?.fatsTarget ?? '—'}g</div>
          {estimatedTargets && (
            <>
              <div>Estimated calorie target: {estimatedTargets.targetCalories} kcal</div>
              <div>Estimated macros: P {estimatedTargets.proteinTarget}g · C {estimatedTargets.carbsTarget}g · F {estimatedTargets.fatsTarget}g</div>
            </>
          )}
          <div>Wellness score (today): {wellnessScore ?? '—'}</div>
        </div>
      </div>
      <div className="card">
        <h2 style={{ marginTop: 0 }}>Daily Nutrition Tracking</h2>
        {dailyLoading ? (
          <div style={{ fontSize: 13 }}>Loading daily nutrition…</div>
        ) : dailyError ? (
          <div style={{ fontSize: 13 }}>{dailyError}</div>
        ) : (
          <>
            <div className="daily-progress">
              <div className="daily-progress-header">
                <span>Calories</span>
                <span>{Math.round(calorieProgress.total)} / {Math.round(calorieProgress.target)} kcal</span>
              </div>
              <div className={`daily-progress-bar ${calorieProgress.isSurplus ? 'surplus' : 'deficit'}`}>
                <div className="daily-progress-fill" style={{ width: `${calorieProgress.pct}%` }} />
              </div>
              <div className="daily-progress-note">
                {calorieProgress.isSurplus ? 'Surplus' : 'Deficit'}
              </div>
            </div>
            <div className="daily-macro-grid">
              <div>Protein: {Math.round(dailyNutrition?.total_protein ?? 0)}g / {Math.round(dailyNutrition?.target_protein ?? 0)}g</div>
              <div>Carbs: {Math.round(dailyNutrition?.total_carbs ?? 0)}g / {Math.round(dailyNutrition?.target_carbs ?? 0)}g</div>
              <div>Fats: {Math.round(dailyNutrition?.total_fats ?? 0)}g / {Math.round(dailyNutrition?.target_fats ?? 0)}g</div>
            </div>
            <div className="daily-ai-block">
              <h3>AI Nutrition Insights</h3>
              {dailyAiLoading ? (
                <div style={{ fontSize: 13 }}>Generating insights…</div>
              ) : dailyAiError ? (
                <div style={{ fontSize: 13 }}>{dailyAiError}</div>
              ) : (
                <div style={{ fontSize: 13 }}>{dailyAiSummary || 'No insights yet.'}</div>
              )}
              <h4>AI Suggestions</h4>
              {dailyAiLoading ? (
                <div style={{ fontSize: 13 }}>Generating suggestions…</div>
              ) : dailyAiError ? (
                <div style={{ fontSize: 13 }}>{dailyAiError}</div>
              ) : (
                <ul>
                  {(dailyAiSuggestions || []).slice(0, 5).map((s, idx) => (
                    <li key={idx}>{s}</li>
                  ))}
                </ul>
              )}
            </div>
          </>
        )}
      </div>
      <DashboardComparison summary={summary} week={week} monthData={monthData} ai={comparisonAi} />
    </div>
  );
}
