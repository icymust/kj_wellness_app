import "../styles/Security.css";
import React, { useState } from "react";

export default function Security({ ctx }) {
  const { api, accessToken, me, setMe } = ctx;

  const [twofaSetup, setTwofaSetup] = useState({
    qr: null,
    secretMasked: null,
    recovery: null,
  });

  const [enrollCode, setEnrollCode] = useState("");
  // Disable flow removed per requirements: only optional enable & verify 2FA
  const [loading, setLoading] = useState(false);

  const twofaEnabled = !!me?.twofaEnabled;

  const startEnroll = async () => {
    try {
      setLoading(true);
      const r = await api.twofaEnroll(accessToken);
      setTwofaSetup({
        qr: r.qrPngBase64,
        secretMasked: r.secretMasked,
        recovery: null,
      });
      setEnrollCode("");
    } catch (e) {
      console.error("Enroll failed", e?.data?.error || e);
    } finally {
      setLoading(false);
    }
  };

  const confirmEnroll = async () => {
    try {
      setLoading(true);
      const v = await api.twofaVerifySetup(accessToken, enrollCode);

      // Keep QR visible permanently as requested; only update recovery codes
      setTwofaSetup(s => ({
        qr: s.qr,
        secretMasked: s.secretMasked,
        recovery: v.recoveryCodes || [],
      }));

      setMe(m => ({ ...m, twofaEnabled: true }));
      setEnrollCode("");

      alert("2FA enabled");
    } catch (e) {
      // Suppress error alert per request; log for debugging
      console.error("Verify failed", e?.data?.error || e);
    } finally {
      setLoading(false);
    }
  };

  // No auto-enroll: QR should appear only during first enable flow

  // Disable flow intentionally omitted

  return (
    <section
      style={{
        border: "1px solid #ddd",
        padding: 16,
        borderRadius: 12,
        marginTop: 16,
      }}
    >
      <h2>Security (2FA)</h2>

      {!twofaEnabled && !twofaSetup.qr && (
        <div style={{ marginBottom: 12 }}>
          <button
            onClick={startEnroll}
            disabled={!accessToken || loading}
          >
            2FA
          </button>
        </div>
      )}

      {!twofaEnabled && twofaSetup.qr && (
        <div
          style={{
            display: "flex",
            gap: 12,
            alignItems: "center",
            marginBottom: 12,
          }}
        >
          <img
            alt="QR"
            src={`data:image/png;base64,${twofaSetup.qr}`}
            width={128}
            height={128}
          />

          <div>
            <div style={{ marginBottom: 6 }}>
              Secret: {twofaSetup.secretMasked}
            </div>

            <input
              value={enrollCode}
              onChange={e => setEnrollCode(e.target.value)}
              placeholder="123456"
              maxLength={6}
              style={{ marginRight: 8 }}
            />

            <button
              onClick={confirmEnroll}
              disabled={loading || enrollCode.length !== 6}
            >
              Confirm
            </button>
          </div>
        </div>
      )}

      {twofaSetup.recovery && (
        <div style={{ marginTop: 12 }}>
          <b>Recovery codes. Save them now:</b>
          <ul>
            {twofaSetup.recovery.map((c, i) => (
              <li key={i}>
                <code>{c}</code>
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  );
}
