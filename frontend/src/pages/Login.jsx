export default function Login({ ctx }) {
  const { oauthUrl, email, setEmail, password, setPassword, handleLogin, loginError, loginLoading, need2fa, twofaCode, setTwofaCode, verify2fa, twofaError, twofaLoading, accessToken, refreshToken, handleRefresh, handleMe, handleLogout, me } = ctx;
  const forgotNav = () => { window.location.href = '/forgot'; };
  return (
    <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
      <h2>Login → Get Tokens</h2>
      <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
        <button onClick={() => window.location.href = oauthUrl("google")}>Sign in with Google</button>
        <button onClick={() => window.location.href = oauthUrl("github")}>Sign in with GitHub</button>
      </div>
      <form onSubmit={handleLogin} style={{ display: "grid", gap: 8, maxWidth: 420 }}>
        <label>Email
          <input type="email" required value={email} onChange={(e)=>setEmail(e.target.value)} />
        </label>
        <label>Password
          <input type="password" required value={password} onChange={(e)=>setPassword(e.target.value)} />
        </label>
        <button type="submit" disabled={loginLoading}>{loginLoading ? 'Вход...' : 'Login'}</button>
  <button type="button" onClick={forgotNav} style={{ background:'none', border:'none', color:'#06c', textDecoration:'underline', padding:0, textAlign:'left', cursor:'pointer' }}>Forgot password?</button>
      </form>
      {loginError && <div style={{ marginTop:8, color:'#b00' }}>{loginError}</div>}
      {need2fa && (
        <div style={{ marginTop: 8, border: "1px dashed #ccc", padding: 8, borderRadius: 8 }}>
          <div>Enter 6-digit code from your authenticator app or a recovery code:</div>
          <input value={twofaCode} onChange={(e)=>setTwofaCode(e.target.value)} placeholder="123456 or RECOVERYCODE" />
          <button onClick={verify2fa} disabled={!twofaCode || twofaLoading}>{twofaLoading ? 'Verification...' : 'Verify 2FA'}</button>
          {twofaError && <div style={{ marginTop:4, color:'#b00' }}>{twofaError}</div>}
        </div>
      )}
      <div style={{ marginTop: 8 }}>
        <div>Access Token:</div>
        <code style={{ wordBreak: "break-all" }}>{accessToken || "—"}</code>
      </div>
      <div style={{ marginTop: 8 }}>
        <div>Refresh Token:</div>
        <code style={{ wordBreak: "break-all" }}>{refreshToken || "—"}</code>
      </div>
      <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
        <button onClick={handleRefresh} disabled={!refreshToken}>Refresh access</button>
        <button onClick={handleMe} disabled={!accessToken}>Call /protected/me</button>
        <button onClick={handleLogout} style={{ marginLeft: "auto" }}>Logout</button>
      </div>
      {me && (
        <pre style={{ background: "#f7f7f7", padding: 12, borderRadius: 8, marginTop: 8 }}>
          {JSON.stringify(me, null, 2)}
        </pre>
      )}
    </section>
  );
}
