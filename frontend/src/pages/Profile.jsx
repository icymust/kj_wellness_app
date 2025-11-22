import React, { useState, useEffect } from 'react';

export default function Profile({ ctx }) {
  const { loadProfile, profile, saveProfile, profileError, profileSuccess, profileSaving } = ctx;
  const [form, setForm] = useState({ age: '', gender: 'male', heightCm: '', weightKg: '', targetWeightKg: '', activityLevel: 'moderate', goal: 'general_fitness' });
  const [dirty, setDirty] = useState(false);
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
    }
  }, [profile, dirty]);

  const onChange = (field, value) => {
    setDirty(true); setStatus(null);
    setForm(f => ({ ...f, [field]: value }));
  };

  const handleReload = async () => {
    setLoading(true); setStatus(null);
    try { await loadProfile(); } finally { setLoading(false); setDirty(false); }
  };

  const handleSave = async (e) => {
    e.preventDefault(); if (!dirty) return;
    setSaving(true); setStatus(null);
    try {
      const saved = await saveProfile(form);
      setStatus('Сохранено');
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
    } catch (e1) {
      setStatus(e1?.data?.error || e1.message || 'Ошибка сохранения');
    } finally { setSaving(false); }
  };

  return (
    <section style={{ border: '1px solid #ddd', padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Профиль здоровья</h2>
      <div style={{ display:'flex', gap:8, marginBottom:12 }}>
        <button type="button" onClick={handleReload} disabled={loading}>{loading ? 'Загрузка...' : 'Обновить с сервера'}</button>
      </div>
  {/* Inline messages: priority — explicit profileError/profileSuccess from global; fallback to local status */}
  {profileError && <div style={{ marginBottom:12, color:'#b00' }}>{profileError}</div>}
  {profileSuccess && <div style={{ marginBottom:12, color:'#070' }}>{profileSuccess}</div>}
  {!profileError && !profileSuccess && status && <div style={{ marginBottom:12, color: status==='Сохранено' ? '#070' : '#b00' }}>{status}</div>}
      <form onSubmit={handleSave} style={{ display: 'grid', gap: 8, maxWidth: 420 }}>
        <label>Возраст
          <input inputMode="numeric" pattern="[0-9]*" value={form.age} onChange={(e)=>onChange('age', e.target.value.replace(/[^0-9]/g,''))} />
        </label>
        <label>Пол
          <select value={form.gender} onChange={(e)=>onChange('gender', e.target.value)}>
            <option value="male">male</option>
            <option value="female">female</option>
            <option value="other">other</option>
          </select>
        </label>
        <label>Рост (см)
          <input inputMode="numeric" pattern="[0-9]*" value={form.heightCm} onChange={(e)=>onChange('heightCm', e.target.value.replace(/[^0-9]/g,''))} />
        </label>
        <label>Вес (кг)
          <input inputMode="decimal" value={form.weightKg} onChange={(e)=>onChange('weightKg', e.target.value.replace(/[^0-9.,]/g,'').replace(',', '.'))} />
        </label>
        <label>Целевой вес (кг)
          <input inputMode="decimal" value={form.targetWeightKg} onChange={(e)=>onChange('targetWeightKg', e.target.value.replace(/[^0-9.,]/g,'').replace(',', '.'))} />
        </label>
        <label>Активность
          <select value={form.activityLevel} onChange={(e)=>onChange('activityLevel', e.target.value)}>
            <option value="low">low</option>
            <option value="moderate">moderate</option>
            <option value="high">high</option>
          </select>
        </label>
        <label>Цель
          <select value={form.goal} onChange={(e)=>onChange('goal', e.target.value)}>
            <option value="weight_loss">weight_loss</option>
            <option value="muscle_gain">muscle_gain</option>
            <option value="general_fitness">general_fitness</option>
          </select>
        </label>
        <div style={{ display:'flex', gap:8, marginTop:4 }}>
          <button type="submit" disabled={!dirty || saving || profileSaving}>{(saving || profileSaving) ? 'Сохранение...' : 'Сохранить'}</button>
          <button type="button" onClick={() => { setDirty(false); setStatus(null); setForm({ age: '', gender: 'male', heightCm: '', weightKg: '', targetWeightKg: '', activityLevel: 'moderate', goal: 'general_fitness' }); }}>Сбросить</button>
        </div>
      </form>
    </section>
  );
}
