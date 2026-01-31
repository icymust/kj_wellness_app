import '../styles/Profile.css';
import React, { useState, useEffect } from 'react';
import NutritionalPreferences from '../components/NutritionalPreferences.jsx';
import { useUser } from '../contexts/UserContext';

export default function Profile({ ctx }) {
  const { loadProfile, profile, saveProfile, profileError, profileSuccess, profileSaving, loadWeights, weights } = ctx;
  const { userId } = useUser();
  const [form, setForm] = useState({ age: '', gender: 'male', heightCm: '', weightKg: '', targetWeightKg: '', activityLevel: 'moderate', goal: 'general_fitness' });
  const [dirty, setDirty] = useState(false);
  const [readOnly, setReadOnly] = useState(true);
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  // auto-load on mount if not loaded yet
  useEffect(() => { if (!profile || profile.age === undefined) { loadProfile(); } }, [profile, loadProfile]);

  // sync global profile into local form when not dirty
  useEffect(() => {
    if (!dirty && profile) {
      setForm({
        age: profile.age ?? '',
        gender: profile.gender ?? 'male',
        heightCm: profile.heightCm ?? '',
        weightKg: profile.weightKg ?? '',
        targetWeightKg: profile.targetWeightKg ?? '',
        activityLevel: profile.activityLevel ?? 'moderate',
        goal: profile.goal ?? 'general_fitness'
      });
      setReadOnly(true);
    }
  }, [profile, dirty]);

  const onChange = (field, value) => {
    if (readOnly) return;
    setDirty(true); setStatus(null);
    setForm(f => ({ ...f, [field]: value }));
  };

  const handleReload = async () => {
    setLoading(true); setStatus(null);
    try {
      // load latest profile from server
      const savedProfile = await loadProfile();
      // also try to refresh weights history and prefer the most recent weight
      // Assumption: loadWeights() returns the loaded weights array (or at least updates `weights` in ctx).
      let loadedWeights = [];
      try {
        const res = await loadWeights();
        if (Array.isArray(res)) loadedWeights = res;
        else loadedWeights = weights || [];
      } catch {
        // if loadWeights fails or doesn't return, fall back to current `weights` from ctx
        loadedWeights = weights || [];
      }

      if (loadedWeights && loadedWeights.length) {
        // find the latest entry by timestamp
        const latest = [...loadedWeights].sort((a, b) => new Date(b.at) - new Date(a.at))[0];
        if (latest && latest.weightKg !== undefined) {
          // reflect the latest weight in the local form (keep other fields from server)
          setForm(f => ({
            ...f,
            age: savedProfile?.age ?? f.age,
            gender: savedProfile?.gender ?? f.gender,
            heightCm: savedProfile?.heightCm ?? f.heightCm,
            weightKg: latest.weightKg ?? (savedProfile?.weightKg ?? f.weightKg),
            targetWeightKg: savedProfile?.targetWeightKg ?? f.targetWeightKg,
            activityLevel: savedProfile?.activityLevel ?? f.activityLevel,
            goal: savedProfile?.goal ?? f.goal
          }));
        }
      }
    } finally { setLoading(false); setDirty(false); setReadOnly(true); }
  };

  const handleSave = async (e) => {
    e.preventDefault(); if (!dirty) return;
    setSaving(true); setStatus(null);
    try {
      const saved = await saveProfile(form);
      setStatus('Saved');
      console.log(`[PROFILE] Preferences saved for userId = ${userId}`);
      // reflect server (if changed) in form; numeric fields may come back as numbers
      setForm({
        age: saved.age ?? '',
        gender: saved.gender ?? 'male',
        heightCm: saved.heightCm ?? '',
        weightKg: saved.weightKg ?? '',
        targetWeightKg: saved.targetWeightKg ?? '',
        activityLevel: saved.activityLevel ?? 'moderate',
        goal: saved.goal ?? 'general_fitness'
      });
      setDirty(false);
      setReadOnly(true);
    } catch (e1) {
  setStatus(e1?.data?.error || e1.message || 'Save error');
    } finally { setSaving(false); }
  };

  const handleConfirm = () => {
    setStatus('Confirmed');
    setDirty(false);
    setReadOnly(true);
  };

  const handleEdit = () => {
    setStatus(null);
    setReadOnly(false);
  };

  return (
    <>
    <section style={{ border: '1px solid #ddd', padding: 16, borderRadius: 12, marginTop: 16 }}>
  <h2>Health Profile</h2>
      <div style={{ display:'flex', gap:8, marginBottom:12 }}>
        <button type="button" onClick={handleReload} disabled={loading}>{loading ? 'Loading...' : 'Update from server'}</button>
        {readOnly ? (
          <>
            <button type="button" onClick={handleConfirm}>Confirm</button>
            <button type="button" onClick={handleEdit}>Edit</button>
          </>
        ) : (
          <button type="button" onClick={() => { setReadOnly(true); setDirty(false); setStatus(null); }}>Cancel</button>
        )}
      </div>
  {/* Inline messages: priority — explicit profileError/profileSuccess from global; fallback to local status */}
  {profileError && <div style={{ marginBottom:12, color:'#b00' }}>{profileError}</div>}
  {profileSuccess && <div style={{ marginBottom:12, color:'#070' }}>{profileSuccess}</div>}
  {!profileError && !profileSuccess && status && <div style={{ marginBottom:12, color: status==='Saved' ? '#070' : '#b00' }}>{status}</div>}
      <form onSubmit={handleSave} style={{ display: 'grid', gap: 8, maxWidth: 420 }}>
        <label>Age 
          <input inputMode="numeric" pattern="[0-9]*" value={form.age} onChange={(e)=>onChange('age', e.target.value.replace(/[^0-9]/g,''))} disabled={readOnly} />
        </label>
        <label>Sex 
          <select value={form.gender} onChange={(e)=>onChange('gender', e.target.value)} disabled={readOnly}>
            <option value="male">male</option>
            <option value="female">female</option>
            <option value="other">other</option>
          </select>
        </label>
        <label>Height (cm)
          <input inputMode="numeric" pattern="[0-9]*" value={form.heightCm} onChange={(e)=>onChange('heightCm', e.target.value.replace(/[^0-9]/g,''))} disabled={readOnly} />
        </label>
        <label>Weight (kg)
          <input inputMode="decimal" value={form.weightKg} onChange={(e)=>onChange('weightKg', e.target.value.replace(/[^0-9.,]/g,'').replace(',', '.'))} disabled={readOnly} />
        </label>
        <label>Weight goal (kg)
          <input inputMode="decimal" value={form.targetWeightKg} onChange={(e)=>onChange('targetWeightKg', e.target.value.replace(/[^0-9.,]/g,'').replace(',', '.'))} disabled={readOnly} />
        </label>
        <label>Activity
          <select value={form.activityLevel} onChange={(e)=>onChange('activityLevel', e.target.value)} disabled={readOnly}>
            <option value="low">low</option>
            <option value="moderate">moderate</option>
            <option value="high">high</option>
          </select>
        </label>
        <label>Goal
          <select value={form.goal} onChange={(e)=>onChange('goal', e.target.value)} disabled={readOnly}>
            <option value="weight_loss">weight_loss</option>
            <option value="muscle_gain">muscle_gain</option>
            <option value="general_fitness">general_fitness</option>
          </select>
        </label>
        <div style={{ display:'flex', gap:8, marginTop:4 }}>
          <button type="submit" disabled={readOnly || !dirty || saving || profileSaving}>{(saving || profileSaving) ? 'Saving...' : 'Save'}</button>
          <button type="button" onClick={() => { setDirty(false); setStatus(null); setForm({ age: '', gender: 'male', heightCm: '', weightKg: '', targetWeightKg: '', activityLevel: 'moderate', goal: 'general_fitness' }); setReadOnly(true); }}>Reset</button>
        </div>
      </form>
    </section>
    <NutritionalPreferences token={ctx.accessToken} />
    </>
  );
}
