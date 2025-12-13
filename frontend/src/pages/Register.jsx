import '../styles/Register.css';
export default function Register({ ctx }) {
  const { email, setEmail, password, setPassword, handleRegister, verificationLink, registerError, registerLoading, verifyStatus } = ctx;
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>1) Register</h2>
      <form onSubmit={handleRegister} style={{ display: "grid", gap: 8, maxWidth: 420 }}>
        <label>Email
          <input value={email} onChange={(e) => setEmail(e.target.value)} required style={{ width: "100%" }} />
        </label>
        <label>Password (min 6)
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required style={{ width: "100%" }} />
        </label>
  <button type="submit" disabled={registerLoading}>{registerLoading ? "Registering..." : "Register"}</button>
      </form>
      {registerError && (
        <div style={{ marginTop: 8, color: "#b00" }}>{registerError}</div>
      )}
      {verifyStatus && (
        <div style={{ marginTop: 8, color: "#064" }}>{verifyStatus}</div>
      )}
      {verificationLink && (
        <div style={{ marginTop: 8 }}>
          <div>Verification link (dev):</div>
          <a href={verificationLink} style={{ wordBreak: "break-all", display: "inline-block" }}>{verificationLink}</a>
        </div>
      )}
    </section>
  );
}
