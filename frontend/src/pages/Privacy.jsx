export default function Privacy({ ctx }) {
  const { loadConsent, exportData, saveConsent, consentForm, setConsentForm, accessToken } = ctx;
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Privacy & Export</h2>
      <>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <button onClick={loadConsent} disabled={!accessToken}>Load consent</button>
          <button onClick={exportData} disabled={!accessToken}>Export my data (JSON)</button>
        </div>

        <form onSubmit={saveConsent} style={{ display: "grid", gap: 8 }}>
          <label>
            <input type="checkbox" checked={consentForm.accepted} onChange={(e)=>setConsentForm(f=>({...f, accepted:e.target.checked}))} /> I accept the privacy policy
          </label>
          <label>
            <input type="checkbox" checked={consentForm.allowAiUseProfile} onChange={(e)=>setConsentForm(f=>({...f, allowAiUseProfile:e.target.checked}))} /> Allow AI to use my profile
          </label>
          <label>
            <input type="checkbox" checked={consentForm.allowAiUseHistory} onChange={(e)=>setConsentForm(f=>({...f, allowAiUseHistory:e.target.checked}))} /> Allow AI to use my history
          </label>
          <label>
            <input type="checkbox" checked={consentForm.publicProfile} onChange={(e)=>setConsentForm(f=>({...f, publicProfile:e.target.checked}))} /> Make my profile public
          </label>
          <label>
            <input type="checkbox" checked={consentForm.emailProduct} onChange={(e)=>setConsentForm(f=>({...f, emailProduct:e.target.checked}))} /> Receive product emails
          </label>
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
