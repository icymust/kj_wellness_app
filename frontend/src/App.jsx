import { useState } from "react";
import WeightChart from "./components/WeightChart.jsx";
import WellnessGauge from "./components/WellnessGauge.jsx";
import GoalProgressBar from "./components/GoalProgressBar.jsx";
import ActivityWeekChart from "./components/ActivityWeekChart.jsx";
import ActivityMonthChart from "./components/ActivityMonthChart.jsx";
import { api } from "./lib/api";
import OAuthCallback from "./OAuthCallback.jsx";

export default function App() {
  

  const [email, setEmail] = useState("test@example.com");
  const [password, setPassword] = useState("123456");
  const [verificationLink, setVerificationLink] = useState("");
  const [verifyToken, setVerifyToken] = useState("");
  const [accessToken, setAccessToken] = useState("");
  const [refreshToken, setRefreshToken] = useState("");
  const [me, setMe] = useState(null);
  const [log, setLog] = useState("");
  const [forgotEmail, setForgotEmail] = useState("");
  const [resetToken, setResetToken] = useState("");
  const [resetNewPwd, setResetNewPwd] = useState("");
  const [newWeight, setNewWeight] = useState("");
  const [weightAt, setWeightAt] = useState("");
  const [weights, setWeights] = useState([]);
  const [summary, setSummary] = useState(null);

  const [profile, setProfile] = useState({
    age: "",
    gender: "other",
    heightCm: "",
    weightKg: "",
    targetWeightKg: "",
    activityLevel: "moderate",
    goal: "general_fitness",
  });

  const [act, setAct] = useState({ type: "cardio", minutes: "", intensity: "moderate", at: "" });
  const [week, setWeek] = useState(null);
  const [period, setPeriod] = useState("week"); // 'week' | 'month'
  const [monthData, setMonthData] = useState(null);

  // Privacy / export
  const [consentForm, setConsentForm] = useState({
    accepted: false,
    version: "1.0",
    allowAiUseProfile: false,
    allowAiUseHistory: false,
    allowAiUseHabits: false,
    publicProfile: false,
    publicStats: false,
    emailProduct: false,
    emailSummaries: false,
  });

  const logMsg = (msg, obj) => setLog((l) => `${new Date().toLocaleTimeString()} ${msg}${obj ? " " + JSON.stringify(obj) : ""}\n` + l);

  function oauthUrl(provider){
    // Prefer explicit VITE_API_BASE_URL pointing to backend (e.g. http://localhost:5173)
    const base = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_BASE || "http://localhost:5173";
    return `${base}/oauth2/authorization/${provider}`;
  }

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

  async function onForgot(e) {
    e.preventDefault();
    try {
      const res = await api.forgot(forgotEmail);
      logMsg("Forgot sent", res);
      alert("If this email exists and is verified, we've sent a reset link.");
      setForgotEmail("");
    } catch (err) {
      logMsg("Forgot error:", { status: err.status, data: err.data });
      alert("Request accepted (we do not disclose existence).");
    }
  }

  async function onReset(e) {
    e.preventDefault();
    try {
      const res = await api.resetPwd(resetToken, resetNewPwd);
      logMsg("Password reset ok", res);
      alert("Password changed. You can log in with the new password.");
      setResetToken(""); setResetNewPwd("");
    } catch (err) {
      logMsg("Reset error:", { status: err.status, data: err.data });
      const msg = err?.data?.error || "Reset failed";
      alert(msg);
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
        targetWeightKg: res.profile?.targetWeightKg ?? "",
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
        targetWeightKg: profile.targetWeightKg ? Number(profile.targetWeightKg) : null,
        activityLevel: profile.activityLevel || null,
        goal: profile.goal || null,
      };
      const res = await api.saveProfile(accessToken, payload);
      logMsg("Profile saved", res.profile);
  // after saving profile, reload profile and weight history (seed may have been created)
  await loadProfile();
  await loadWeights();
    } catch (err) {
      console.error("Save profile error (full):", err);
      logMsg("Save profile error:", { status: err?.status, data: err?.data, message: err?.message });
      alert(err?.data?.error || err?.message || "Save failed");
    }
  }

  async function addWeight(e){ e.preventDefault();
  try{
    const res = await api.weightAdd(accessToken, Number(newWeight), weightAt || null);
    logMsg("Weight added", res.entry);
    setNewWeight(""); setWeightAt("");
    await loadWeights();
  }catch(err){ logMsg("Weight add error:", {status:err.status, data:err.data}); }
  }
  async function loadWeights(){
    try{
      const res = await api.weightList(accessToken);
      setWeights(res.entries || []);
      logMsg("Weight list loaded", (res.entries||[]).length);
    }catch(err){ logMsg("Weight list error:", {status:err.status, data:err.data}); }
  }
  async function loadSummary(){
    try{
      const res = await api.analyticsSummary(accessToken);
      setSummary(res);
      logMsg("Analytics summary", res);
    }catch(err){ logMsg("Summary error:", {status:err.status, data:err.data}); }
  }

  async function addActivity(e){ e.preventDefault();
    try {
      const payload = {
        type: act.type,
        minutes: Number(act.minutes),
        intensity: act.intensity || null,
        at: act.at || null,
      };
      const res = await api.activityAdd(accessToken, payload);
      logMsg("Activity added", res.entry);
      setAct({ type:"cardio", minutes:"", intensity:"moderate", at:"" });
      await loadActivityWeek();
    } catch(err) {
      logMsg("Add activity error:", { status: err.status, data: err.data });
    }
  }

  async function loadActivityWeek(dateStr){
    try {
      const res = await api.activityWeek(accessToken, dateStr || null);
      setWeek(res.summary);
      logMsg("Week summary", res.summary);
    } catch(err){
      logMsg("Week summary error:", { status: err.status, data: err.data });
    }
  }

  async function loadActivityMonth(d = new Date()){
    const y = d.getUTCFullYear();
    const m = d.getUTCMonth() + 1;
    try {
      const res = await api.activityMonth(accessToken, y, m);
      const by = res.byDayMinutes || {};
      const total = Object.values(by).reduce((a,b)=>a+(b||0), 0);
      const daysActive = Object.values(by).filter(v=>v>0).length;
      setMonthData({ byDayMinutes: by, year: y, month: m, total, daysActive });
      logMsg("Month summary", { year: y, month: m, total, daysActive, by });
    } catch(err){
      logMsg("Month summary error:", { status: err.status, data: err.data });
    }
  }

  // Privacy helpers
  async function loadConsent() {
    try {
      const res = await api.privacyGet(accessToken);
      setConsentForm(res);
      logMsg("Consent", res);
    } catch (e) { logMsg("Consent load error", e); }
  }

  async function saveConsent(e) {
    e.preventDefault();
    try {
      const res = await api.privacySet(accessToken, consentForm);
      logMsg("Consent saved", res);
      alert("Saved");
    } catch (e) {
      alert(e?.data?.error || "Save failed");
    }
  }

  async function exportData() {
    try {
      const data = await api.privacyExport(accessToken);
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: "application/json" });
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url; a.download = "ndl-export.json"; a.click();
      URL.revokeObjectURL(url);
    } catch (e) { console.error(e); alert(e?.message || "Export failed"); }
  }

  // handle OAuth callback route after hooks are initialized
  if (typeof window !== 'undefined' && window.location.pathname === '/oauth-callback') {
    return <OAuthCallback onTokens={({ access, refresh }) => { setAccessToken(access); setRefreshToken(refresh); }} />;
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
        <h2>Privacy & Export</h2>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <button onClick={loadConsent} disabled={!accessToken}>Load consent</button>
          <button onClick={exportData} disabled={!accessToken}>Export my data (JSON)</button>
        </div>

        <form onSubmit={saveConsent} style={{ display: "grid", gap: 8 }}>
          <label>
            <input type="checkbox" checked={consentForm.accepted} onChange={(e)=>setConsentForm(f=>({...f, accepted:e.target.checked}))} /> I accept the privacy policy
          </label>
          <label>
            <input type="checkbox" checked={consentForm.allowAiUseProfile} onChange={(e)=>setConsentForm(f=>({...f, allowAiUseProfile:e.target.checked}))} /> Allow AI to use my profile
          </label>
          <label>
            <input type="checkbox" checked={consentForm.allowAiUseHistory} onChange={(e)=>setConsentForm(f=>({...f, allowAiUseHistory:e.target.checked}))} /> Allow AI to use my history
          </label>
          <label>
            <input type="checkbox" checked={consentForm.publicProfile} onChange={(e)=>setConsentForm(f=>({...f, publicProfile:e.target.checked}))} /> Make my profile public
          </label>
          <label>
            <input type="checkbox" checked={consentForm.emailProduct} onChange={(e)=>setConsentForm(f=>({...f, emailProduct:e.target.checked}))} /> Receive product emails
          </label>
          <div>
            <small>Version: {consentForm.version}</small>
          </div>
          <div>
            <button type="submit" disabled={!accessToken}>Save consent</button>
          </div>
        </form>
      </section>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>Activity</h2>
        <form onSubmit={addActivity} style={{ display:"grid", gridTemplateColumns:"repeat(2,1fr)", gap:8 }}>
          <label>Type
            <select value={act.type} onChange={(e)=>setAct(a=>({...a, type:e.target.value}))}>
              <option value="cardio">cardio</option>
              <option value="strength">strength</option>
              <option value="flexibility">flexibility</option>
              <option value="sports">sports</option>
              <option value="other">other</option>
            </select>
          </label>
          <label>Minutes
            <input required value={act.minutes} onChange={(e)=>setAct(a=>({...a, minutes:e.target.value}))}/>
          </label>
          <label>Intensity
            <select value={act.intensity} onChange={(e)=>setAct(a=>({...a, intensity:e.target.value}))}>
              <option value="low">low</option>
              <option value="moderate">moderate</option>
              <option value="high">high</option>
            </select>
          </label>
          <label>Timestamp (ISO, optional)
            <input placeholder="2025-10-22T18:00:00Z" value={act.at} onChange={(e)=>setAct(a=>({...a, at:e.target.value}))}/>
          </label>
          <div style={{ gridColumn:"1 / -1" }}>
            <button type="submit" disabled={!accessToken || !act.minutes} style={{ width:"100%" }}>Add activity</button>
          </div>
        </form>

        <div style={{ display:"flex", gap:8, marginTop:8, alignItems:"center" }}>
          <label>
            Period:&nbsp;
            <select value={period} onChange={(e)=>setPeriod(e.target.value)}>
              <option value="week">Week</option>
              <option value="month">Month</option>
            </select>
          </label>

          {period === "week" ? (
            <button onClick={()=>loadActivityWeek()} disabled={!accessToken}>Load this week</button>
          ) : (
            <button onClick={()=>loadActivityMonth()} disabled={!accessToken}>Load this month</button>
          )}
        </div>

        {period === "week" && week && (
          <div style={{ marginTop: 12 }}>
            <div style={{ marginBottom:8 }}>
              <b>Week from:</b> {new Date(week.weekStartIso).toLocaleDateString()} ·
              <b> sessions:</b> {week.sessions} ·
              <b> total:</b> {week.totalMinutes} min ·
              <b> days active:</b> {week.daysActive}
            </div>
            <ActivityWeekChart byWeekdayMinutes={week.byWeekdayMinutes}/>
            <div style={{ marginTop:8 }}>
              <b>By type (min):</b>{" "}
              {Object.entries(week.byTypeMinutes || {}).map(([k,v])=>`${k}: ${v}`).join(" · ") || "—"}
            </div>
          </div>
        )}

        {period === "month" && monthData && (
          <>
            <div style={{ marginTop: 12 }}>
              <b>Month:</b> {monthData.year}-{String(monthData.month).padStart(2, "0")} ·
              <b> total:</b> {monthData.total} min ·
              <b> days active:</b> {monthData.daysActive}
            </div>
            <ActivityMonthChart
              byDayMinutes={monthData.byDayMinutes}
              year={monthData.year}
              month={monthData.month}
            />
          </>
        )}
      </section>

      <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
        <h2>Weight Progress</h2>
        <form onSubmit={addWeight} style={{ display:"grid", gap:8 }}>
          <label>Weight (kg) <input value={newWeight} onChange={e=>setNewWeight(e.target.value)} required /></label>
          <label>Timestamp (ISO, optional) <input placeholder="2025-10-15T21:10:00Z" value={weightAt} onChange={e=>setWeightAt(e.target.value)} /></label>
          <small>Если пусто — сохранится текущий timestamp. Повтор того же timestamp вернёт 409.</small>
          <button type="submit" disabled={!accessToken}>Add</button>
        </form>

        <div style={{ marginTop:8 }}>
          <button onClick={loadWeights} disabled={!accessToken}>Load history</button>
          <ul>{weights.map(w=>(
            <li key={w.id}>{w.at} — {w.weightKg} kg</li>
          ))}</ul>
        </div>
        <div style={{ marginTop: 12 }}>
          <WeightChart
            entries={weights}
            targetWeightKg={summary?.goal?.targetWeightKg ?? profile?.targetWeightKg ?? null}
            initialWeightKg={profile?.weightKg ?? null}
          />
        </div>
      </section>

      <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
        <h2>Analytics</h2>
        <button onClick={loadSummary} disabled={!accessToken}>Load BMI & Wellness</button>
        {summary && (
          <div style={{ display: "grid", gridTemplateColumns: "1fr 2fr", gap: 16, marginTop: 12 }}>
                  <div>
                    <WellnessGauge value={summary?.scores?.wellness ?? 0} />
                    <div style={{ marginTop: 8 }}>
                      <b>BMI:</b> {summary?.bmi?.value} ({summary?.bmi?.classification})
                    </div>

                    <div style={{ marginTop: 12 }}>
                      <GoalProgressBar
                        percent={summary?.goal?.progress?.percent ?? 0}
                        remainingKg={summary?.goal?.progress?.remainingKg ?? null}
                      />
                    </div>
                  </div>
            <pre style={{ background: "#f7f7f7", padding: 12, borderRadius: 8 }}>
              {JSON.stringify(summary, null, 2)}
            </pre>
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
          <label>Target Weight (kg) <input value={profile.targetWeightKg} onChange={(e)=>setProfile(p=>({...p, targetWeightKg:e.target.value}))} /></label>
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

      <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:16 }}>
        <h3>Forgot password</h3>
        <form onSubmit={onForgot} style={{ display:"grid", gap:8, maxWidth:420 }}>
          <label>Email
            <input type="email" required value={forgotEmail}
                   onChange={(e)=>setForgotEmail(e.target.value)} />
          </label>
          <button type="submit">Send reset link</button>
          <small>Мы всегда отвечаем 200 и не раскрываем, существует ли аккаунт.</small>
        </form>
      </section>

      <section style={{ border:"1px solid #ddd", padding:16, borderRadius:12, marginTop:12 }}>
        <h3>Reset password</h3>
        <form onSubmit={onReset} style={{ display:"grid", gap:8, maxWidth:420 }}>
          <label>Token
            <input required value={resetToken}
                   placeholder="paste token from email link"
                   onChange={(e)=>setResetToken(e.target.value)} />
          </label>
          <label>New password
            <input type="password" required value={resetNewPwd}
                   onChange={(e)=>setResetNewPwd(e.target.value)} />
          </label>
          <button type="submit">Reset</button>
          <small>Токен одноразовый, действует ~30 минут.</small>
        </form>
      </section>

      <section style={{ border: "1px solid #ddd", padding: 16, borderRadius: 12, marginTop: 16 }}>
        <h2>3) Login → Get Tokens</h2>
        <div style={{ display: "flex", gap: 8, marginBottom: 8 }}>
          <button onClick={() => window.location.href = oauthUrl("google")}>Sign in with Google</button>
          <button onClick={() => window.location.href = oauthUrl("github")}>Sign in with GitHub</button>
        </div>
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
