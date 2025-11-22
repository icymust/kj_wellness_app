import React, { useState } from 'react';

export default function Forgot({ ctx }) {
  const { api } = ctx;
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState(null);
  const submit = async (e) => {
    e.preventDefault(); setStatus(null); setLoading(true);
    try { await api.forgot(email); setStatus('Если адрес существует, отправили письмо со ссылкой для сброса.'); }
    catch(e1){ setStatus(e1?.data?.error || e1.message || 'Ошибка'); }
    finally{ setLoading(false); }
  };
  return (
    <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
      <h3>Восстановление пароля</h3>
      <form onSubmit={submit} style={{ display:"grid", gap:8, maxWidth:420 }}>
        <label>Email
          <input type="email" required value={email} onChange={(e)=>setEmail(e.target.value)} />
        </label>
        <button type="submit" disabled={loading}>{loading ? 'Отправка...' : 'Отправить ссылку'}</button>
        <small>Ответ всегда успешный: мы не раскрываем наличие аккаунта.</small>
      </form>
      {status && <div style={{ marginTop:12 }}>{status}</div>}
    </section>
  );
}
