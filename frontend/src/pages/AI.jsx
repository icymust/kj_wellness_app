import '../styles/AI.css';
import { useEffect, useState } from 'react';
import { api } from '../lib/api';

export default function AI({ ctx }) {
  const { aiScope, setAiScope, loadAiLatest, regenAi, ai, aiLoading, token } = ctx;
  const [expanded, setExpanded] = useState({});
  const [aiEnabled, setAiEnabled] = useState(true);
  const [aiStatusLoading, setAiStatusLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        setAiStatusLoading(true);
        const s = await api.aiStatus(token);
        if (mounted && s && typeof s.enabled === 'boolean') setAiEnabled(s.enabled);
      } catch {
        // ignore
      } finally {
        if (mounted) setAiStatusLoading(false);
      }
    })();
    return () => { mounted = false; };
  }, [token]);

  async function toggleAi() {
    try {
      setAiStatusLoading(true);
      const s = await api.aiSetStatus(token, !aiEnabled);
      if (s && typeof s.enabled === 'boolean') setAiEnabled(s.enabled);
    } finally {
      setAiStatusLoading(false);
    }
  }
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
          <button onClick={regenAi} disabled={aiLoading || !aiEnabled} title={!aiEnabled ? 'AI service unavailable — showing cached recommendations' : ''}>
            Regenerate
          </button>
          <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
            <button onClick={toggleAi} disabled={aiStatusLoading}>
              {aiEnabled ? 'AI Enabled' : 'AI Disabled (show cached recommendations)'}
            </button>
          </div>
        </div>
        {!aiEnabled && (
          <div style={{ color:'#a66', fontSize: 13, marginBottom: 8 }}>
            AI service unavailable: you can load cached recommendations, but regeneration is disabled.
          </div>
        )}
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
                  }}>{(it.priorityLevel || it.priority || 'LOW').toString().toLowerCase()}</span>
                  <b>{it.title}</b>
                  <div style={{ color: "#444", marginTop: 4 }}>
                    {expanded[idx] ? it.detail : (it.message || it.detail)}
                  </div>
                  <button
                    style={{ marginTop: 4, fontSize: 12 }}
                    onClick={() => setExpanded(prev => ({ ...prev, [idx]: !prev[idx] }))}
                  >
                    {expanded[idx] ? 'Show less' : 'Show more'}
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </>
    </section>
  );
}
