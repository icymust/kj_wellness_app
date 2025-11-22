import React, { useState, useMemo } from 'react';

export default function Security({ ctx }) {
  const { twofaSetup, setTwofaSetup, api, accessToken, me } = ctx;
  const [enrollCode, setEnrollCode] = useState('');
  const [disableCode, setDisableCode] = useState('');

  // Determine active status: prefer server flag if present, fallback to presence of recovery codes previously fetched
  const twofaEnabled = useMemo(() => {
    if (me && typeof me.twofaEnabled === 'boolean') return me.twofaEnabled;
    return Array.isArray(twofaSetup.recovery) && twofaSetup.recovery.length > 0;
  }, [me, twofaSetup.recovery]);

  const startEnroll = async () => {
    try {
      const r = await api.twofaEnroll(accessToken);
      setTwofaSetup({ qr: r.qrPngBase64, secretMasked: r.secretMasked, recovery: null });
      setEnrollCode('');
    } catch (e) { alert(e?.data?.error || 'Enroll failed'); }
  };
  const confirmEnroll = async () => {
    try {
      const v = await api.twofaVerifySetup(accessToken, enrollCode);
      setTwofaSetup(s => ({ ...s, recovery: v.recoveryCodes || [] }));
      alert('2FA enabled');
    } catch (e) { alert(e?.data?.error || 'Verify failed'); }
  };
  const disable = async () => {
    try {
      await api.twofaDisable(accessToken, disableCode);
      setTwofaSetup({ qr: null, secretMasked: null, recovery: null });
      setEnrollCode(''); setDisableCode('');
      alert('2FA disabled');
    } catch (e) { alert(e?.data?.error || 'Disable failed'); }
  };

  return (
    <section style={{ border: '1px solid #ddd', padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Security (2FA)</h2>
      {!twofaEnabled && (
        <div style={{ marginBottom: 12 }}>
          <button onClick={startEnroll} disabled={!accessToken}>Включить 2FA</button>
        </div>
      )}
      {!twofaEnabled && twofaSetup.qr && (
        <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 12 }}>
          <img alt="QR" src={`data:image/png;base64,${twofaSetup.qr}`} width={128} height={128} />
          <div>
            <div>Secret: {twofaSetup.secretMasked}</div>
            <input value={enrollCode} onChange={(e) => setEnrollCode(e.target.value)} placeholder="123456" maxLength={6} />
            <button onClick={confirmEnroll} disabled={!enrollCode}>Подтвердить</button>
          </div>
        </div>
      )}
      {twofaEnabled && (
        <div style={{ marginBottom: 12 }}>
          <input value={disableCode} onChange={(e) => setDisableCode(e.target.value)} placeholder="code or recovery" />
          <button onClick={disable} disabled={!accessToken || !disableCode}>Отключить 2FA</button>
        </div>
      )}
      {twofaSetup.recovery && (
        <div style={{ marginTop: 8 }}>
          <b>Recovery codes (save now):</b>
          <ul>{twofaSetup.recovery.map((c, i) => (<li key={i}><code>{c}</code></li>))}</ul>
        </div>
      )}
    </section>
  );
}
