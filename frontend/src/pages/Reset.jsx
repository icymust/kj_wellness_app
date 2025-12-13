import '../styles/Reset.css';
import React, { useState, useEffect } from 'react';

export default function Reset({ ctx }) {
  const { api } = ctx;
  const [token, setToken] = useState('');
  const [pwd, setPwd] = useState('');
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);
  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const t = params.get('token'); if (t) setToken(t);
  }, []);
  const submit = async (e) => {
    e.preventDefault(); setStatus(null);
  if (pwd.length < 6){ setStatus('Password must be at least 6 characters'); return; }
    setLoading(true);
  try { await api.resetPwd(token, pwd); setStatus('Password updated. You can now log in.'); }
  catch(e1){ setStatus(e1?.data?.error || e1.message || 'Error'); }
    finally{ setLoading(false); }
  };
  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:12 }}>
      <h3>Password reset</h3>
      <form onSubmit={submit} style={{ display:"grid", gap:8, maxWidth:420 }}>
        <label>Token
          <input required value={token} placeholder="from email link" onChange={(e)=>setToken(e.target.value)} />
        </label>
  <label>New password
          <input type="password" required value={pwd} onChange={(e)=>setPwd(e.target.value)} />
        </label>
        <button type="submit" disabled={loading}>{loading ? 'Reset...' : 'Reset'}</button>
        <small>The token is one-time use and valid for 30 minutes.</small>
      </form>
  {status && <div style={{ marginTop:12, color: status.startsWith('Password') ? '#070' : '#b00' }}>{status}</div>}
    </section>
  );
}
