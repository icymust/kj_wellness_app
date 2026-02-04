import '../styles/NutritionalPreferences.css';
import React, { useCallback, useEffect, useState } from 'react';

export default function NutritionalPreferences({ token }) {
  const [nutritionalPrefs, setNutritionalPrefs] = useState(null);
  const [availableDietaryPreferences, setAvailableDietaryPreferences] = useState([]);
  const [availableAllergies, setAvailableAllergies] = useState([]);
  const [availableCuisines] = useState(['American', 'Asian', 'French', 'Indian', 'Italian', 'Mediterranean', 'Mexican', 'Middle Eastern', 'Other']);
  const [selectedDietary, setSelectedDietary] = useState(new Set());
  const [selectedAllergies, setSelectedAllergies] = useState(new Set());
  const [selectedCuisines, setSelectedCuisines] = useState(new Set());
  
  // New state for food preferences and targets
  const [dislikedIngredients, setDislikedIngredients] = useState('');
  const [calorieTarget, setCalorieTarget] = useState('');
  const [proteinTarget, setProteinTarget] = useState('');
  const [carbsTarget, setCarbsTarget] = useState('');
  const [fatsTarget, setFatsTarget] = useState('');
  
  // Meal frequency and timing
  const [breakfastCount, setBreakfastCount] = useState(1);
  const [lunchCount, setLunchCount] = useState(1);
  const [dinnerCount, setDinnerCount] = useState(1);
  const [snackCount, setSnackCount] = useState(0);
  const [breakfastTime, setBreakfastTime] = useState('08:00');
  const [lunchTime, setLunchTime] = useState('12:30');
  const [dinnerTime, setDinnerTime] = useState('18:00');
  const [snackTime, setSnackTime] = useState('');
  
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);
  const [readOnly, setReadOnly] = useState(true);

  const loadPreferences = useCallback(async () => {
    setLoading(true);
    setStatus(null);
    try {
      const { api } = await import('../lib/api.js');
      const response = await api.getNutritionalPreferences(token);
      
      if (response.nutritionalPreferences) {
        const prefs = response.nutritionalPreferences;
        setNutritionalPrefs(prefs);
        setSelectedDietary(new Set(prefs.dietaryPreferences || []));
        setSelectedAllergies(new Set(prefs.allergies || []));
        setSelectedCuisines(new Set(prefs.cuisinePreferences || []));
        
        // Load new fields
        setDislikedIngredients((prefs.dislikedIngredients || []).join(', '));
        setCalorieTarget(prefs.calorieTarget || '');
        setProteinTarget(prefs.proteinTarget || '');
        setCarbsTarget(prefs.carbsTarget || '');
        setFatsTarget(prefs.fatsTarget || '');
        
        // Load meal frequency and timing
        setBreakfastCount(prefs.breakfastCount ?? 1);
        setLunchCount(prefs.lunchCount ?? 1);
        setDinnerCount(prefs.dinnerCount ?? 1);
        setSnackCount(prefs.snackCount ?? 0);
        setBreakfastTime(prefs.breakfastTime || '08:00');
        setLunchTime(prefs.lunchTime || '12:30');
        setDinnerTime(prefs.dinnerTime || '18:00');
        setSnackTime(prefs.snackTime || '');
        setReadOnly(true);
      }
      
      if (response.availableDietaryPreferences) {
        setAvailableDietaryPreferences(Array.from(response.availableDietaryPreferences).sort());
      }
      
      if (response.availableAllergies) {
        setAvailableAllergies(Array.from(response.availableAllergies).sort());
      }
    } catch (err) {
      setStatus(err?.data?.error || err.message || 'Failed to load preferences');
    } finally {
      setLoading(false);
      setDirty(false);
      setReadOnly(true);
    }
  }, [token]);

  useEffect(() => {
    loadPreferences();
  }, [loadPreferences]);

  const handleDietaryChange = (preference) => {
    if (readOnly) return;
    const newSelected = new Set(selectedDietary);
    if (newSelected.has(preference)) {
      newSelected.delete(preference);
    } else {
      newSelected.add(preference);
    }
    setSelectedDietary(newSelected);
    setDirty(true);
    setStatus(null);
  };

  const handleAllergyChange = (allergen) => {
    if (readOnly) return;
    const newSelected = new Set(selectedAllergies);
    if (newSelected.has(allergen)) {
      newSelected.delete(allergen);
    } else {
      newSelected.add(allergen);
    }
    setSelectedAllergies(newSelected);
    setDirty(true);
    setStatus(null);
  };

  const handleCuisineChange = (cuisine) => {
    if (readOnly) return;
    const newSelected = new Set(selectedCuisines);
    if (newSelected.has(cuisine)) {
      newSelected.delete(cuisine);
    } else {
      newSelected.add(cuisine);
    }
    setSelectedCuisines(newSelected);
    setDirty(true);
    setStatus(null);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!dirty) return;
    
    setSaving(true);
    setStatus(null);
    
    try {
      const { api } = await import('../lib/api.js');
      
      // Parse comma-separated strings to arrays
      const dislikedIngredientsArray = dislikedIngredients
        .split(',')
        .map(s => s.trim())
        .filter(s => s.length > 0);
      
      const response = await api.saveNutritionalPreferences(token, {
        dietaryPreferences: Array.from(selectedDietary),
        allergies: Array.from(selectedAllergies),
        dislikedIngredients: dislikedIngredientsArray,
        cuisinePreferences: Array.from(selectedCuisines),
        calorieTarget: calorieTarget ? parseInt(calorieTarget) : null,
        proteinTarget: proteinTarget ? parseInt(proteinTarget) : null,
        carbsTarget: carbsTarget ? parseInt(carbsTarget) : null,
        fatsTarget: fatsTarget ? parseInt(fatsTarget) : null,
        breakfastCount: breakfastCount,
        lunchCount: lunchCount,
        dinnerCount: dinnerCount,
        snackCount: snackCount,
        breakfastTime: breakfastCount > 0 ? breakfastTime : null,
        lunchTime: lunchCount > 0 ? lunchTime : null,
        dinnerTime: dinnerCount > 0 ? dinnerTime : null,
        snackTime: snackCount > 0 ? snackTime : null
      });
      
      if (response.nutritionalPreferences) {
        setNutritionalPrefs(response.nutritionalPreferences);
        setStatus('Saved successfully');
        setDirty(false);
        setReadOnly(true);
      }
    } catch (err) {
      setStatus(err?.data?.error || err.message || 'Failed to save preferences');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = () => {
    if (nutritionalPrefs) {
      setSelectedDietary(new Set(nutritionalPrefs.dietaryPreferences || []));
      setSelectedAllergies(new Set(nutritionalPrefs.allergies || []));
      setSelectedCuisines(new Set(nutritionalPrefs.cuisinePreferences || []));
      setDislikedIngredients((nutritionalPrefs.dislikedIngredients || []).join(', '));
      setCalorieTarget(nutritionalPrefs.calorieTarget || '');
      setProteinTarget(nutritionalPrefs.proteinTarget || '');
      setCarbsTarget(nutritionalPrefs.carbsTarget || '');
      setFatsTarget(nutritionalPrefs.fatsTarget || '');
      setBreakfastCount(nutritionalPrefs.breakfastCount ?? 1);
      setLunchCount(nutritionalPrefs.lunchCount ?? 1);
      setDinnerCount(nutritionalPrefs.dinnerCount ?? 1);
      setSnackCount(nutritionalPrefs.snackCount ?? 0);
      setBreakfastTime(nutritionalPrefs.breakfastTime || '08:00');
      setLunchTime(nutritionalPrefs.lunchTime || '12:30');
      setDinnerTime(nutritionalPrefs.dinnerTime || '18:00');
      setSnackTime(nutritionalPrefs.snackTime || '');
    }
    setDirty(false);
    setStatus(null);
    setReadOnly(true);
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

  if (loading && !nutritionalPrefs) {
    return (
      <section style={{ border: '1px solid #ddd', padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>Nutritional Preferences</h2>
        <p>Loading...</p>
      </section>
    );
  }

  return (
    <section style={{ border: '1px solid #ddd', padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Nutritional Preferences</h2>
      
      {status && (
        <div style={{ 
          marginBottom: 12, 
          padding: 8,
          borderRadius: 4,
          color: status.includes('successfully') || status === 'Saved' ? '#070' : '#b00',
          backgroundColor: status.includes('successfully') || status === 'Saved' ? '#f0f8f0' : '#f8f0f0'
        }}>
          {status}
        </div>
      )}
      <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        {readOnly ? (
          <>
            <button type="button" onClick={handleConfirm} disabled={saving}>Confirm</button>
            <button type="button" onClick={handleEdit} disabled={saving}>Edit</button>
          </>
        ) : (
          <button type="button" onClick={() => { handleReset(); setReadOnly(true); }} disabled={saving}>
            Cancel
          </button>
        )}
      </div>

      <form onSubmit={handleSave}>
        {/* Food Preferences & Targets Section */}
        <div style={{ marginBottom: 20, padding: 16, border: '1px solid #e0e0e0', borderRadius: 8, backgroundColor: '#fafafa' }}>
          <h3>Food Preferences & Targets</h3>
          
          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold' }}>
              Disliked ingredients
            </label>
            <input
              type="text"
              value={dislikedIngredients}
              onChange={(e) => { if (readOnly) return; setDislikedIngredients(e.target.value); setDirty(true); setStatus(null); }}
              placeholder="e.g. mushrooms, olives, cilantro"
              disabled={saving || readOnly}
              style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
            />
          </div>

          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Cuisine preferences
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 8 }}>
              {availableCuisines.map((cuisine) => (
                <label key={cuisine} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                  <input
                    type="checkbox"
                    checked={selectedCuisines.has(cuisine)}
                    onChange={() => handleCuisineChange(cuisine)}
                    disabled={saving || readOnly}
                  />
                  <span>{cuisine}</span>
                </label>
              ))}
            </div>
          </div>

          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', marginBottom: 4, fontWeight: 'bold' }}>
              Daily calorie target (kcal)
            </label>
            <input
              type="number"
              value={calorieTarget}
              onChange={(e) => { if (readOnly) return; setCalorieTarget(e.target.value); setDirty(true); setStatus(null); }}
              placeholder="e.g. 2000"
              disabled={saving || readOnly}
              min="0"
              style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
            />
          </div>

          <div style={{ marginBottom: 12 }}>
            <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
              Macro targets
            </label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                  Protein (g)
                </label>
                <input
                  type="number"
                  value={proteinTarget}
                  onChange={(e) => { if (readOnly) return; setProteinTarget(e.target.value); setDirty(true); setStatus(null); }}
                  placeholder="150"
                  disabled={saving || readOnly}
                  min="0"
                  style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                  Carbs (g)
                </label>
                <input
                  type="number"
                  value={carbsTarget}
                  onChange={(e) => { if (readOnly) return; setCarbsTarget(e.target.value); setDirty(true); setStatus(null); }}
                  placeholder="200"
                  disabled={saving || readOnly}
                  min="0"
                  style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
                />
              </div>
              <div>
                <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                  Fats (g)
                </label>
                <input
                  type="number"
                  value={fatsTarget}
                  onChange={(e) => { if (readOnly) return; setFatsTarget(e.target.value); setDirty(true); setStatus(null); }}
                  placeholder="65"
                  disabled={saving || readOnly}
                  min="0"
                  style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Meal Frequency Section */}
        <div style={{ marginBottom: 20, padding: 16, border: '1px solid #e0e0e0', borderRadius: 8, backgroundColor: '#fafafa' }}>
          <h3>Meal Frequency</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 12 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Breakfast
              </label>
              <input
                type="number"
                value={breakfastCount}
                onChange={(e) => {
                  if (readOnly) return;
                  const val = parseInt(e.target.value) || 0;
                  setBreakfastCount(val);
                  if (val === 0) setBreakfastTime('');
                  setDirty(true);
                  setStatus(null);
                }}
                disabled={saving || readOnly}
                min="0"
                max="5"
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Lunch
              </label>
              <input
                type="number"
                value={lunchCount}
                onChange={(e) => {
                  if (readOnly) return;
                  const val = parseInt(e.target.value) || 0;
                  setLunchCount(val);
                  if (val === 0) setLunchTime('');
                  setDirty(true);
                  setStatus(null);
                }}
                disabled={saving || readOnly}
                min="0"
                max="5"
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Dinner
              </label>
              <input
                type="number"
                value={dinnerCount}
                onChange={(e) => {
                  if (readOnly) return;
                  const val = parseInt(e.target.value) || 0;
                  setDinnerCount(val);
                  if (val === 0) setDinnerTime('');
                  setDirty(true);
                  setStatus(null);
                }}
                disabled={saving || readOnly}
                min="0"
                max="5"
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Snacks
              </label>
              <input
                type="number"
                value={snackCount}
                onChange={(e) => {
                  if (readOnly) return;
                  const val = parseInt(e.target.value) || 0;
                  setSnackCount(val);
                  if (val === 0) setSnackTime('');
                  setDirty(true);
                  setStatus(null);
                }}
                disabled={saving || readOnly}
                min="0"
                max="5"
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc' }}
              />
            </div>
          </div>
        </div>

        {/* Meal Timing Section */}
        <div style={{ marginBottom: 20, padding: 16, border: '1px solid #e0e0e0', borderRadius: 8, backgroundColor: '#fafafa' }}>
          <h3>Meal Timing</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 12 }}>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Breakfast
              </label>
              <input
                type="time"
                value={breakfastTime}
                onChange={(e) => { if (readOnly) return; setBreakfastTime(e.target.value); setDirty(true); setStatus(null); }}
                disabled={saving || readOnly || breakfastCount === 0}
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', opacity: breakfastCount === 0 ? 0.5 : 1 }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Lunch
              </label>
              <input
                type="time"
                value={lunchTime}
                onChange={(e) => { if (readOnly) return; setLunchTime(e.target.value); setDirty(true); setStatus(null); }}
                disabled={saving || readOnly || lunchCount === 0}
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', opacity: lunchCount === 0 ? 0.5 : 1 }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Dinner
              </label>
              <input
                type="time"
                value={dinnerTime}
                onChange={(e) => { if (readOnly) return; setDinnerTime(e.target.value); setDirty(true); setStatus(null); }}
                disabled={saving || readOnly || dinnerCount === 0}
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', opacity: dinnerCount === 0 ? 0.5 : 1 }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: 4, fontSize: '0.9em' }}>
                Snack
              </label>
              <input
                type="time"
                value={snackTime}
                onChange={(e) => { if (readOnly) return; setSnackTime(e.target.value); setDirty(true); setStatus(null); }}
                disabled={saving || readOnly || snackCount === 0}
                style={{ width: '100%', padding: 8, borderRadius: 4, border: '1px solid #ccc', opacity: snackCount === 0 ? 0.5 : 1 }}
              />
            </div>
          </div>
        </div>

        <div style={{ marginBottom: 20 }}>
          <h3>Dietary Preferences</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 8 }}>
            {availableDietaryPreferences.map((pref) => (
              <label key={pref} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <input
                  type="checkbox"
                  checked={selectedDietary.has(pref)}
                  onChange={() => handleDietaryChange(pref)}
                  disabled={saving || readOnly}
                />
                <span>{pref.replace('-', ' ')}</span>
              </label>
            ))}
          </div>
        </div>

        <div style={{ marginBottom: 20 }}>
          <h3>Allergies & Intolerances</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 8 }}>
            {availableAllergies.map((allergen) => (
              <label key={allergen} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <input
                  type="checkbox"
                  checked={selectedAllergies.has(allergen)}
                  onChange={() => handleAllergyChange(allergen)}
                  disabled={saving || readOnly}
                />
                <span>{allergen.replace('-', ' ')}</span>
              </label>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <button 
            type="submit" 
            disabled={!dirty || saving || readOnly}
            style={{ cursor: dirty && !saving ? 'pointer' : 'not-allowed' }}
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
          <button 
            type="button" 
            onClick={handleReset}
            disabled={!dirty || saving}
            style={{ cursor: dirty && !saving ? 'pointer' : 'not-allowed' }}
          >
            Reset
          </button>
          <button 
            type="button" 
            onClick={loadPreferences}
            disabled={saving}
            style={{ cursor: !saving ? 'pointer' : 'not-allowed' }}
          >
            Reload from Server
          </button>
        </div>

        {nutritionalPrefs && nutritionalPrefs.updatedAt && (
          <p style={{ fontSize: '0.85em', color: '#666', marginTop: 12 }}>
            Last updated: {new Date(nutritionalPrefs.updatedAt).toISOString()}
          </p>
        )}
      </form>
    </section>
  );
}
