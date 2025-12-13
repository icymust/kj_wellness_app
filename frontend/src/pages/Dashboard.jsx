import React, { useEffect } from 'react';
import '../styles/Dashboard.css';

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
      <DashboardComparison summary={summary} week={week} monthData={monthData} ai={comparisonAi} />
    </div>
  );
}
