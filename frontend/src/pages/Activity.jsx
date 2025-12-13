import '../styles/Activity.css';
import React, { useRef, useState, useMemo } from "react";
import ActivityWeekChart from "../components/ActivityWeekChart.jsx";
import ActivityMonthChart from "../components/ActivityMonthChart.jsx";

export default function Activity({ ctx }) {
  const { addActivity, period, setPeriod, loadActivityWeek, loadActivityMonth, week, monthData } = ctx;
  const minutesRef = useRef(null);
  const [form, setForm] = useState({ type: 'cardio', minutes: '', intensity: 'moderate', at: '' });
  const onChange = (field, value) => setForm(f => ({ ...f, [field]: value }));
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.minutes) return;
    await addActivity(form);
    // refresh only current period
    if (period === 'week') await loadActivityWeek(); else if (period === 'month') await loadActivityMonth();
    setForm(f => ({ ...f, minutes: '', at: '' }));
    minutesRef.current?.focus();
  };
  // memoized charts prevent remount on form change
  const weekChart = useMemo(() => week ? <ActivityWeekChart byWeekdayMinutes={week.byWeekdayMinutes} /> : null, [week]);
  const monthChart = useMemo(() => monthData ? <ActivityMonthChart byDayMinutes={monthData.byDayMinutes} year={monthData.year} month={monthData.month} /> : null, [monthData]);
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Activity</h2>
      <>
        <form onSubmit={handleSubmit} style={{ display:"grid", gridTemplateColumns:"repeat(2,1fr)", gap:8 }}>
          <label>Type
            <select value={form.type} onChange={(e)=>onChange('type', e.target.value)}>
              <option value="cardio">cardio</option>
              <option value="strength">strength</option>
              <option value="flexibility">flexibility</option>
              <option value="sports">sports</option>
              <option value="other">other</option>
            </select>
          </label>
          <label>Minutes
            <input ref={minutesRef} required inputMode="numeric" pattern="[0-9]*" type="text" value={form.minutes} onChange={(e)=>onChange('minutes', e.target.value.replace(/[^0-9]/g,''))} placeholder="e.g. 45" />
          </label>
          <label>Intensity
            <select value={form.intensity} onChange={(e)=>onChange('intensity', e.target.value)}>
              <option value="low">low</option>
              <option value="moderate">moderate</option>
              <option value="high">high</option>
            </select>
          </label>
          <label>Timestamp (ISO, optional)
            <input placeholder="2025-10-22T18:00:00Z" value={form.at} onChange={(e)=>onChange('at', e.target.value)} />
          </label>
          <div style={{ gridColumn:"1 / -1" }}>
                <button type="submit" disabled={!form.minutes} style={{ width:"100%" }}>Add activity</button>
          </div>
        </form>

        <div style={{ display:"flex", gap:8, marginTop:8, alignItems:"center" }}>
          <label>
            Period:&nbsp;
            <select value={period} onChange={(e)=>setPeriod(e.target.value)}>
              <option value="week">Week</option>
              <option value="month">Month</option>
            </select>
          </label>
          {period === "week" ? (
            <button type="button" onClick={()=>loadActivityWeek()}>Refresh week</button>
          ) : (
            <button type="button" onClick={()=>loadActivityMonth()}>Refresh month</button>
          )}
        </div>

        {period === "week" && week && (
          <div style={{ marginTop: 12 }}>
            <div style={{ marginBottom:8 }}>
              <b>Week from:</b> {new Date(week.weekStartIso).toLocaleDateString()} ·
              <b> sessions:</b> {week.sessions} ·
              <b> total:</b> {week.totalMinutes} min ·
              <b> days active:</b> {week.daysActive}
            </div>
            {weekChart}
            <div style={{ marginTop:8 }}>
              <b>By type (min):</b>{" "}
              {Object.entries(week.byTypeMinutes || {}).map(([k,v])=>`${k}: ${v}`).join(" · ") || "—"}
            </div>
          </div>
        )}

        {period === "month" && monthData && (
          <>
            <div style={{ marginTop: 12 }}>
              <b>Month:</b> {monthData.year}-{String(monthData.month).padStart(2, "0")} ·
              <b> total:</b> {monthData.total} min ·
              <b> days active:</b> {monthData.daysActive}
            </div>
            {monthChart}
          </>
        )}
      </>
    </section>
  );
}
