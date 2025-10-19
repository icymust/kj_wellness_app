import { useState } from "react";
import { api } from "./lib/api";

export default function App() {
  const [email, setEmail] = useState("test@example.com");
  const [password, setPassword] = useState("123456");
  const [verificationLink, setVerificationLink] = useState("");
  const [verifyToken, setVerifyToken] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [refreshToken, setRefreshToken] = useState("");
  const [me, setMe] = useState(null);
  const [log, setLog] = useState("");

  const [profile, setProfile] = useState({
    age: "",
    gender: "other",
    heightCm: "",
    weightKg: "",
    activityLevel: "moderate",
    goal: "general_fitness",
  });

  const logMsg = (msg, obj) => setLog((l) => `${new Date().toLocaleTimeString()} ${msg}${obj ? " " + JSON.stringify(obj) : ""}\n` + l);

  async function handleRegister(e) {
    e.preventDefault();
    try {
      const res = await api.register(email, password);
      setVerificationLink(res.verificationLink || "");
      const tokenFromLink = res.verificationLink?.split("token=")[1] || "";
      setVerifyToken(tokenFromLink);
      logMsg("Registered:", res.user);
    } catch (err) {
      logMsg("Register error:", { status: err.status, data: err.data });
    }
  }

  async function handleVerify(e) {
    e.preventDefault();
    try {
      const res = await api.verify(verifyToken);
      logMsg("Verified:", res.user);
    } catch (err) {
      logMsg("Verify error:", { status: err.status, data: err.data });
    }
  }

  async function handleLogin(e) {
    e.preventDefault();
    try {
      const res = await api.login(email, password);
      setAccessToken(res.accessToken);
      setRefreshToken(res.refreshToken);
      logMsg("Logged in: tokens received");
    } catch (err) {
      logMsg("Login error:", { status: err.status, data: err.data });
    }
  }

  async function handleMe() {
    try {
      const res = await api.me(accessToken);
      setMe(res);
      logMsg("Protected /me:", res);
    } catch (err) {
      logMsg("Me error:", { status: err.status, data: err.data });
    }
  }

  async function handleRefresh() {
    try {
      const res = await api.refresh(refreshToken);
      setAccessToken(res.accessToken);
      logMsg("Access token refreshed");
    } catch (err) {
      logMsg("Refresh error:", { status: err.status, data: err.data });
    }
  }

  async function loadProfile() {
    try {
      const res = await api.getProfile(accessToken);
      setProfile({
        age: res.profile?.age ?? "",
        gender: res.profile?.gender ?? "other",
        heightCm: res.profile?.heightCm ?? "",
        weightKg: res.profile?.weightKg ?? "",
        activityLevel: res.profile?.activityLevel ?? "moderate",
        goal: res.profile?.goal ?? "general_fitness",
      });
      logMsg("Profile loaded", res.profile);
    } catch (err) {
      logMsg("Load profile error:", { status: err.status, data: err.data });
    }
  }

  async function saveProfile(e) {
    e?.preventDefault?.();
    try {
      const payload = {
        age: profile.age ? Number(profile.age) : null,
        gender: profile.gender || null,
        heightCm: profile.heightCm ? Number(profile.heightCm) : null,
        weightKg: profile.weightKg ? Number(profile.weightKg) : null,
        activityLevel: profile.activityLevel || null,
        goal: profile.goal || null,
      };
      const res = await api.saveProfile(accessToken, payload);
      logMsg("Profile saved", res.profile);
    } catch (err) {
      logMsg("Save profile error:", { status: err.status, data: err.data });
    }
  }

  return (
    <div style={{ maxWidth: 720, margin: "2rem auto", fontFamily: "system-ui, -apple-system, Segoe UI, Roboto, sans-serif" }}>
      <h1>Numbers Don’t Lie — Auth Demo</h1>
      <p style={{ color: "#555" }}>Backend: {import.meta.env.VITE_API_BASE}</p>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>1) Register</h2>
        <form onSubmit={handleRegister} style={{ display: "grid", gap: 8 }}>
          <label>Email
            <input value={email} onChange={(e) => setEmail(e.target.value)} required style={{ width: "100%" }} />
          </label>
          <label>Password (min 6)
            <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required style={{ width: "100%" }} />
          </label>
          <button type="submit">Register</button>
        </form>
        {verificationLink && (
          <div style={{ marginTop: 8 }}>
            <div>Verification link:</div>
            <code style={{ wordBreak: "break-all" }}>{verificationLink}</code>
          </div>
        )}
      </section>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>Health Profile</h2>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <button onClick={loadProfile} disabled={!accessToken}>Load profile</button>
        </div>
        <form onSubmit={saveProfile} style={{ display: "grid", gap: 8 }}>
          <label>Age <input value={profile.age} onChange={(e)=>setProfile(p=>({...p, age:e.target.value}))} /></label>
          <label>Gender
            <select value={profile.gender} onChange={(e)=>setProfile(p=>({...p, gender:e.target.value}))}>
              <option value="male">male</option>
              <option value="female">female</option>
              <option value="other">other</option>
            </select>
          </label>
          <label>Height (cm) <input value={profile.heightCm} onChange={(e)=>setProfile(p=>({...p, heightCm:e.target.value}))} /></label>
          <label>Weight (kg) <input value={profile.weightKg} onChange={(e)=>setProfile(p=>({...p, weightKg:e.target.value}))} /></label>
          <label>Activity
            <select value={profile.activityLevel} onChange={(e)=>setProfile(p=>({...p, activityLevel:e.target.value}))}>
              <option value="low">low</option>
              <option value="moderate">moderate</option>
              <option value="high">high</option>
            </select>
          </label>
          <label>Goal
            <select value={profile.goal} onChange={(e)=>setProfile(p=>({...p, goal:e.target.value}))}>
              <option value="weight_loss">weight_loss</option>
              <option value="muscle_gain">muscle_gain</option>
              <option value="general_fitness">general_fitness</option>
            </select>
          </label>
          <button type="submit" disabled={!accessToken}>Save</button>
        </form>
      </section>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>2) Verify Email</h2>
        <div style={{ display: "grid", gap: 8 }}>
          <label>Token
            <input value={verifyToken} onChange={(e) => setVerifyToken(e.target.value)} style={{ width: "100%" }} />
          </label>
          <button onClick={handleVerify}>Verify</button>
        </div>
      </section>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>3) Login → Get Tokens</h2>
        <form onSubmit={handleLogin} style={{ display: "grid", gap: 8 }}>
          <button type="submit">Login</button>
        </form>
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
        </div>
        {me && (
          <pre style={{ background: "#f7f7f7", padding: 12, borderRadius: 8, marginTop: 8 }}>
            {JSON.stringify(me, null, 2)}
          </pre>
        )}
      </section>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h3>Log</h3>
        <textarea readOnly rows={10} style={{ width: "100%", fontFamily: "monospace" }} value={log} />
      </section>
    </div>
  );
}
