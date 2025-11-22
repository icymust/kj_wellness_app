export default function AI({ ctx }) {
  const { aiScope, setAiScope, loadAiLatest, regenAi, ai, aiLoading } = ctx;
  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
      <h2>AI Insights</h2>
      <>
        <div style={{ display:"flex", gap:8, alignItems:"center", marginBottom:8 }}>
          <label>
            Scope:&nbsp;
            <select value={aiScope} onChange={(e)=>setAiScope(e.target.value)}>
              <option value="weekly">weekly</option>
              <option value="monthly">monthly</option>
            </select>
          </label>
          <button onClick={loadAiLatest} disabled={aiLoading}>Load latest</button>
          <button onClick={regenAi} disabled={aiLoading}>Regenerate</button>
        </div>
        {ai && (
          <div style={{ marginTop:8 }}>
            <div style={{ color: ai.fromCache ? "#6a6" : "#666" }}>
              {ai.fromCache ? "from cache" : "fresh"} · generated: {ai.generatedAt ? new Date(ai.generatedAt).toLocaleString() : "—"}
            </div>
            <div style={{ marginTop:8 }}>
              <b>Summary:</b> {Object.entries(ai.summary || {}).map(([k,v])=>`${k}: ${v}`).join(" · ") || "—"}
            </div>
            <ul style={{ marginTop:8 }}>
              {(ai.items || []).map((it, idx) => (
                <li key={idx} style={{ marginBottom:8 }}>
                  <span style={{
                    padding: "2px 6px",
                    borderRadius: 6,
                    background: it.priority === 'high' ? '#fee' : it.priority === 'medium' ? '#ffe' : '#eef',
                    border: '1px solid #ddd',
                    marginRight: 8,
                    fontSize: 12
                  }}>{it.priority || 'low'}</span>
                  <b>{it.title}</b>
                  <div style={{ color: "#444", marginTop: 4 }}>{it.detail}</div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </>
    </section>
  );
}
