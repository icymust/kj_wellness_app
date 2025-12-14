import '../styles/Privacy.css';
export default function Privacy({ ctx }) {
  const { loadConsent, exportData, exportHealth, saveConsent, consentForm, setConsentForm, accessToken } = ctx;
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Privacy & Export</h2>
      <p style={{ marginTop: 8, color: '#444' }}>
        To use the platform, all consents are required. Without them, we cannot provide health analytics or personalized insights.
      </p>
      <>
        <div style={{ display: "flex", flexWrap:'wrap', gap: 8, marginBottom: 8 }}>
          <button onClick={loadConsent} disabled={!accessToken}>Load consent</button>
          <button onClick={exportData} disabled={!accessToken}>Export (legacy)</button>
          <button onClick={async () => {
            if (!accessToken) return; try {
              const data = await exportHealth();
              const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
              const url = URL.createObjectURL(blob);
              const a = document.createElement('a');
              const ts = new Date().toISOString().slice(0,19).replace(/[:T]/g,'-');
              a.href = url; a.download = `health-export-${ts}.json`;
              document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url);
            } catch (e) { console.warn('exportHealth', e); }
          }} disabled={!accessToken}>Download health data</button>
        </div>

        <form onSubmit={saveConsent} style={{ display: "grid", gap: 12 }}>
          <fieldset style={{ border: '1px solid #eee', borderRadius: 8, padding: 12 }}>
            <legend>Required consents (all must be enabled)</legend>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
              <div style={{ flex: '1 1 260px', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
                <label>
                  <input
                    type="checkbox"
                    checked={!!consentForm.accepted}
                    onChange={(e)=>setConsentForm(f=>({...f, accepted:e.target.checked}))}
                  />{' '}
                  I accept the privacy policy
                </label>
                <small style={{ color: '#666', display: 'block', marginTop: 6 }}>
                  We collect basic health data such as height, weight, activity level and goals to calculate BMI, wellness scores and progress charts.
                </small>
              </div>

              <div style={{ flex: '1 1 260px', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
                <label>
                  <input
                    type="checkbox"
                    checked={!!consentForm.allowAiUseProfile}
                    onChange={(e)=>setConsentForm(f=>({...f, allowAiUseProfile:e.target.checked}))}
                  />{' '}
                  Use my info for AI preferences
                </label>
                <small style={{ color: '#666', display: 'block', marginTop: 6 }}>
                  Your profile data will be used to generate personalized health insights and recommendations.
                </small>
              </div>

              <div style={{ flex: '1 1 260px', border: '1px solid #f0f0f0', borderRadius: 8, padding: 12 }}>
                <label>
                  <input
                    type="checkbox"
                    checked={!!consentForm.allowAiUseHistory}
                    onChange={(e)=>setConsentForm(f=>({...f, allowAiUseHistory:e.target.checked}))}
                  />{' '}
                  Allow to use my health history
                </label>
                <small style={{ color: '#666', display: 'block', marginTop: 6 }}>
                  Your weight and activity history will be analyzed to track progress and generate weekly and monthly summaries.
                </small>
              </div>
            </div>
          </fieldset>

          

          <div>
            <small>Version: {consentForm.version}</small>
          </div>
          <div>
            <button type="submit" disabled={!accessToken}>Save consent</button>
          </div>
        </form>
      </>
    </section>
  );
}
