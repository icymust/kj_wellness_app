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
    if (pwd.length < 6){ setStatus('Пароль минимум 6 символов'); return; }
    setLoading(true);
    try { await api.resetPwd(token, pwd); setStatus('Пароль обновлён. Можете войти.'); }
    catch(e1){ setStatus(e1?.data?.error || e1.message || 'Ошибка'); }
    finally{ setLoading(false); }
  };
  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:12 }}>
      <h3>Сброс пароля</h3>
      <form onSubmit={submit} style={{ display:"grid", gap:8, maxWidth:420 }}>
        <label>Токен
          <input required value={token} placeholder="из ссылки email" onChange={(e)=>setToken(e.target.value)} />
        </label>
        <label>Новый пароль
          <input type="password" required value={pwd} onChange={(e)=>setPwd(e.target.value)} />
        </label>
        <button type="submit" disabled={loading}>{loading ? 'Сброс...' : 'Сбросить'}</button>
        <small>Токен одноразовый, действует около 30 минут.</small>
      </form>
      {status && <div style={{ marginTop:12, color: status.startsWith('Пароль') ? '#070' : '#b00' }}>{status}</div>}
    </section>
  );
}
