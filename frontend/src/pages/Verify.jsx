export default function Verify({ ctx }) {
  const { verifyToken, setVerifyToken, handleVerify, verifyError, verifyStatus, verifyLoading } = ctx;
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16, maxWidth: 480 }}>
      <h2>Verify Email</h2>
      <div style={{ display: "grid", gap: 8 }}>
        <label>Token
          <input value={verifyToken} onChange={(e) => setVerifyToken(e.target.value)} style={{ width: "100%" }} placeholder="paste token or open link" />
        </label>
  <button onClick={handleVerify} disabled={!verifyToken || verifyLoading}>{verifyLoading ? "Checking..." : "Verify"}</button>
        {verifyError && <div style={{ color: '#b00' }}>{verifyError}</div>}
        {verifyStatus && <div style={{ color: '#064' }}>{verifyStatus}</div>}
  {!verifyToken && <small>Usually you follow the link from the email — the token will be filled automatically.</small>}
      </div>
    </section>
  );
}
