import '../styles/Weight.css';
import React, { useState, useRef, useMemo } from "react";
import WeightChart from "../components/WeightChart.jsx";

export default function Weight({ ctx }) {
  const { addWeight, loadWeights, weights, summary, profile, loadDietary, saveDietary } = ctx;
  // Local form state — do not touch global states while typing
  const [form, setForm] = useState({ weight: "", at: "", dietaryPreferences: [], dietaryRestrictions: [] });
  const weightRef = useRef(null);
  const onChange = (field, value) => setForm(f => ({ ...f, [field]: value }));
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.weight) return;
    try {
      // Send dietary preferences/restrictions together with the weight entry as arrays
      await addWeight({
        weight: form.weight,
        at: form.at,
        dietaryPreferences: form.dietaryPreferences || [],
        dietaryRestrictions: form.dietaryRestrictions || []
      });
    } catch(e){ console.warn('addWeight failed', e); }
    setForm(f => ({ ...f, weight: "", at: "" }));
    weightRef.current?.focus();
  };

  const toggleArrayValue = (field, value) => {
    setForm(f => {
      const arr = new Set(f[field] || []);
      if (arr.has(value)) arr.delete(value); else arr.add(value);
      return { ...f, [field]: Array.from(arr) };
    });
  };

  // load saved dietary meta on mount (optional)
  React.useEffect(() => {
    let mounted = true;
    if (loadDietary) {
      loadDietary().then(r => {
        if (!mounted || !r) return;
        setForm(f => ({ ...f, dietaryPreferences: r.dietaryPreferences || [], dietaryRestrictions: r.dietaryRestrictions || [] }));
      }).catch(e => { /* ignore */ });
    }
    return () => { mounted = false; };
  }, [loadDietary]);

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

      {/* Dietary Preferences & Restrictions (optional) */}
      <section style={{ borderTop: '1px solid #eee', marginTop:18, paddingTop:12 }}>
        <h3>Dietary Preferences (optional)</h3>
        <div style={{ display:'flex', gap:12, flexWrap:'wrap' }}>
          <label style={{ display:'flex', alignItems:'center', gap:6 }}>
            <input type="checkbox" checked={form.dietaryPreferences.includes('Vegetarian')} onChange={()=>toggleArrayValue('dietaryPreferences','Vegetarian')} />
            Vegetarian
          </label>
          <label style={{ display:'flex', alignItems:'center', gap:6 }}>
            <input type="checkbox" checked={form.dietaryPreferences.includes('Vegan')} onChange={()=>toggleArrayValue('dietaryPreferences','Vegan')} />
            Vegan
          </label>
          <label style={{ display:'flex', alignItems:'center', gap:6 }}>
            <input type="checkbox" checked={form.dietaryPreferences.includes('No specific preference')} onChange={()=>toggleArrayValue('dietaryPreferences','No specific preference')} />
            No specific preference
          </label>
        </div>
        

        <h3 style={{ marginTop:12 }}>Dietary Restrictions (optional)</h3>
        <div style={{ display:'flex', gap:12, flexWrap:'wrap' }}>
          <label style={{ display:'flex', alignItems:'center', gap:6 }}>
            <input type="checkbox" checked={form.dietaryRestrictions.includes('Gluten-free')} onChange={()=>toggleArrayValue('dietaryRestrictions','Gluten-free')} />
            Gluten-free
          </label>
          <label style={{ display:'flex', alignItems:'center', gap:6 }}>
            <input type="checkbox" checked={form.dietaryRestrictions.includes('Lactose-free')} onChange={()=>toggleArrayValue('dietaryRestrictions','Lactose-free')} />
            Lactose-free
          </label>
          <label style={{ display:'flex', alignItems:'center', gap:6 }}>
            <input type="checkbox" checked={form.dietaryRestrictions.includes('None')} onChange={()=>toggleArrayValue('dietaryRestrictions','None')} />
            None
          </label>
        </div>
        

        <div style={{ marginTop:10, display:'flex', gap:8 }}>
          <button type="button" onClick={async ()=>{ try { await saveDietary(form.dietaryPreferences, form.dietaryRestrictions); alert('Preferences saved'); } catch(e){ alert('Save failed'); } }}>
            Save
          </button>
          <button type="button" onClick={async ()=>{ if (!loadDietary) return; const r = await loadDietary(); setForm(f => ({ ...f, dietaryPreferences: r.dietaryPreferences || [], dietaryRestrictions: r.dietaryRestrictions || [] })); }}>
            Load
          </button>
        </div>
      </section>

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
