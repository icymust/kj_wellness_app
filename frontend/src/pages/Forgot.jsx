import '../styles/Forgot.css';
import React, { useState } from 'react';

export default function Forgot({ ctx }) {
  const { api } = ctx;
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState(null);
  const submit = async (e) => {
    e.preventDefault(); setStatus(null); setLoading(true);
    try { await api.forgot(email); setStatus('If the address exists, please send an email using the reset link.'); }
  catch(e1){ setStatus(e1?.data?.error || e1.message || 'Error'); }
    finally{ setLoading(false); }
  };
  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
      <h3>Password recovery</h3>
      <form onSubmit={submit} style={{ display:"grid", gap:8, maxWidth:420 }}>
        <label>Email
          <input type="email" required value={email} onChange={(e)=>setEmail(e.target.value)} />
        </label>
        <button type="submit" disabled={loading}>{loading ? 'Sending...' : 'Send link'}</button>
        <small>The answer is always successful: we do not disclose the existence of the account.</small>
      </form>
      {status && <div style={{ marginTop:12 }}>{status}</div>}
    </section>
  );
}
