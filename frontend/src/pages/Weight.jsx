import React, { useState, useRef, useMemo } from "react";
import WeightChart from "../components/WeightChart.jsx";

export default function Weight({ ctx }) {
  const { addWeight, loadWeights, weights, summary, profile } = ctx;
  // Локальное состояние формы — не трогаем глобальные стейты при наборе
  const [form, setForm] = useState({ weight: "", at: "" });
  const weightRef = useRef(null);
  const onChange = (field, value) => setForm(f => ({ ...f, [field]: value }));
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.weight) return;
    try {
      await addWeight({ weight: form.weight, at: form.at });
    } catch(e){ console.warn('addWeight failed', e); }
    setForm(f => ({ ...f, weight: "", at: "" }));
    weightRef.current?.focus();
  };

  // Мемоизация графика — чтобы ввод в форму не пересоздавал компонент
  const chart = useMemo(() => (
    <WeightChart
      entries={weights}
      targetWeightKg={summary?.goal?.targetWeightKg ?? profile?.targetWeightKg ?? null}
      initialWeightKg={profile?.weightKg ?? null}
    />
  ), [weights, summary?.goal?.targetWeightKg, profile?.targetWeightKg, profile?.weightKg]);

  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
      <h2>Weight Progress</h2>
      <form onSubmit={handleSubmit} style={{ display:"grid", gap:8, maxWidth:400 }}>
        <label>Weight (kg)
          <input
            ref={weightRef}
            inputMode="decimal"
            placeholder="e.g. 82.5"
            value={form.weight}
            onChange={(e)=>onChange('weight', e.target.value.replace(/[^0-9.,]/g,'').replace(',', '.'))}
            required
          />
        </label>
        <label>Timestamp (ISO, optional)
          <input
            placeholder="2025-10-15T21:10:00Z"
            value={form.at}
            onChange={(e)=>onChange('at', e.target.value)}
          />
        </label>
        <small>If empty, the current time will be used.</small>
        <div style={{ display:'flex', gap:8 }}>
          <button type="submit" disabled={!form.weight}>Add</button>
          <button type="button" onClick={loadWeights}>Refresh history</button>
        </div>
      </form>

      <div style={{ marginTop:16 }}>
        <ul style={{ maxHeight:180, overflowY:'auto', paddingLeft:20 }}>
          {weights.map(w => (
            <li key={w.id}>{w.at} — {w.weightKg} kg</li>
          ))}
        </ul>
      </div>
      <div style={{ marginTop: 16 }}>
        {chart}
      </div>
    </section>
  );
}
