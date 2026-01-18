import '../styles/NutritionalPreferences.css';
import React, { useState, useEffect } from 'react';

export default function NutritionalPreferences({ token }) {
  const [nutritionalPrefs, setNutritionalPrefs] = useState(null);
  const [availableDietaryPreferences, setAvailableDietaryPreferences] = useState([]);
  const [availableAllergies, setAvailableAllergies] = useState([]);
  const [selectedDietary, setSelectedDietary] = useState(new Set());
  const [selectedAllergies, setSelectedAllergies] = useState(new Set());
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [dirty, setDirty] = useState(false);

  useEffect(() => {
    loadPreferences();
  }, [token]);

  const loadPreferences = async () => {
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
    }
  };

  const handleDietaryChange = (preference) => {
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

  const handleSave = async (e) => {
    e.preventDefault();
    if (!dirty) return;
    
    setSaving(true);
    setStatus(null);
    
    try {
      const { api } = await import('../lib/api.js');
      const response = await api.saveNutritionalPreferences(token, {
        dietaryPreferences: Array.from(selectedDietary),
        allergies: Array.from(selectedAllergies)
      });
      
      if (response.nutritionalPreferences) {
        setNutritionalPrefs(response.nutritionalPreferences);
        setStatus('Saved successfully');
        setDirty(false);
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
    }
    setDirty(false);
    setStatus(null);
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

      <form onSubmit={handleSave}>
        <div style={{ marginBottom: 20 }}>
          <h3>Dietary Preferences</h3>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 8 }}>
            {availableDietaryPreferences.map((pref) => (
              <label key={pref} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                <input
                  type="checkbox"
                  checked={selectedDietary.has(pref)}
                  onChange={() => handleDietaryChange(pref)}
                  disabled={saving}
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
                  disabled={saving}
                />
                <span>{allergen.replace('-', ' ')}</span>
              </label>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <button 
            type="submit" 
            disabled={!dirty || saving}
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
            Last updated: {new Date(nutritionalPrefs.updatedAt).toLocaleString()}
          </p>
        )}
      </form>
    </section>
  );
}
