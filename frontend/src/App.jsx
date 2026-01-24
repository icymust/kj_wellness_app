import React, { useState, useEffect } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate, Link, useNavigate, useLocation } from "react-router-dom";
import { api } from "./lib/api";
import { setTokens, clearTokens, getAccessToken, getRefreshToken } from "./lib/tokens";
import { UserProvider } from "./contexts/UserContext";
import OAuthCallback from "./OAuthCallback";
import Home from "./pages/Home";
import Register from "./pages/Register";
import Login from "./pages/Login";
import Privacy from "./pages/Privacy";
import Activity from "./pages/Activity";
import Weight from "./pages/Weight";
import Analytics from "./pages/Analytics";
import AI from "./pages/AI";
import Dashboard from "./pages/Dashboard";
import Profile from "./pages/Profile";
import Security from "./pages/Security";
import Verify from "./pages/Verify";
import Forgot from "./pages/Forgot";
import Reset from "./pages/Reset";
import Log from "./pages/Log";
import { DebugMealPlanPage } from "./pages/DebugMealPlanPage";
import { MealPlanPage } from "./pages/MealPlanPage";

// Root component wraps router so inner shell can use hooks
export default function App(){
  return (
    <Router>
      <AppShell />
    </Router>
  );
}

function AppShell() {
  const navigate = useNavigate();
  const location = useLocation();

  // minimal auth state
  // Инициализируем сразу из localStorage, чтобы при первом рендере защищённых роутов
  // не происходил мгновенный редирект на /login до useEffect.
  const [accessToken, setAccessToken] = useState(() => getAccessToken() || null);
  const [refreshToken, setRefreshToken] = useState(() => getRefreshToken() || null);
  const [me, setMe] = useState(null);
  // register/login form state
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [verificationLink, setVerificationLink] = useState(null);
  const [registerError, setRegisterError] = useState(null);
  const [registerLoading, setRegisterLoading] = useState(false);
  // email verification flow
  const [verifyToken, setVerifyToken] = useState("");
  const [verifyStatus, setVerifyStatus] = useState(null);
  const [verifyError, setVerifyError] = useState(null);
  const [verifyLoading, setVerifyLoading] = useState(false);
  // login & 2FA state
  const [loginError, setLoginError] = useState(null);
  const [loginLoading, setLoginLoading] = useState(false);
  const [need2fa, setNeed2fa] = useState(false);
  const [tempToken, setTempToken] = useState(null);
  const [twofaCode, setTwofaCode] = useState("");
  const [twofaError, setTwofaError] = useState(null);
  const [twofaLoading, setTwofaLoading] = useState(false);
  // 2FA setup page state
  const [twofaSetup, setTwofaSetup] = useState({ qr:null, secretMasked:null, recovery:null });
  // Profile
  const [profile, setProfile] = useState({
    age: "",
    gender: "male",
    heightCm: "",
    weightKg: "",
    targetWeightKg: "",
    activityLevel: "moderate",
    goal: "general_fitness",
  });
  const [profileError, setProfileError] = useState(null);
  const [profileSuccess, setProfileSuccess] = useState(null);
  const [profileSaving, setProfileSaving] = useState(false);
  // Weight
  const [newWeight, setNewWeight] = useState("");
  const [weightAt, setWeightAt] = useState("");
  const [weights, setWeights] = useState([]);
  // Analytics
  const [summary, setSummary] = useState(null);
  // Activity
  const [act, setAct] = useState({ type: "cardio", minutes: "", intensity: "moderate", at: "" });
  const [period, setPeriod] = useState("week");
  const [week, setWeek] = useState(null);
  const [monthData, setMonthData] = useState(null);
  // AI
  const [aiScope, setAiScope] = useState("weekly");
  const [ai, setAi] = useState(null);
  const [aiLoading, setAiLoading] = useState(false);
  // Rate limit global state (unix ms timestamp until which we should pause firing new dashboard-related requests)
  const [rateLimitUntil, setRateLimitUntil] = useState(0);
  // Unified dashboard loading + error state
  const [dashboardLoading, setDashboardLoading] = useState(false);
  const [dashboardError, setDashboardError] = useState(null);

  // Central handler for 429 responses to avoid spamming console & network
  function handleRateLimit(e, scope) {
    const retryAfterHeader = e?.headers?.['retry-after'];
    let delayMs = 8000; // default backoff
    if (retryAfterHeader) {
      const sec = parseInt(retryAfterHeader, 10);
      if (!isNaN(sec)) delayMs = sec * 1000;
    }
    const until = Date.now() + delayMs;
    setRateLimitUntil(prev => until > prev ? until : prev);
    if (!window.__lastRateLimitLog || Date.now() - window.__lastRateLimitLog > 1500) {
      console.warn(`Rate limited (${scope}) — delaying further requests for ${Math.round(delayMs/1000)}s`);
      window.__lastRateLimitLog = Date.now();
    }
  }
  // Privacy
  const [consentForm, setConsentForm] = useState({
    accepted: false,
    version: 1,
    allowAiUseProfile: false,
    allowAiUseHistory: false,
    publicProfile: false,
    emailProduct: false,
  });
  const [consentLoaded, setConsentLoaded] = useState(false);

  // small helpers / placeholders so pages can call them without crashing
  const handleRegister = async (e) => {
    e?.preventDefault?.();
    setRegisterError(null);
    setVerificationLink(null);
    setVerifyStatus(null);
    if (!email || !password || password.length < 6) {
      setRegisterError("Password must be at least 6 characters");
      return;
    }
    setRegisterLoading(true);
    try {
      const res = await api.register(email, password);
      setVerificationLink(res.verificationLink || null);
      if (res.verificationLink) {
        setVerifyStatus("User created. Please follow the verification link sent to your email.");
      } else {
        setVerifyStatus("User created. Verification link is available in server logs.");
      }
    } catch (err) {
      setRegisterError(err?.data?.error || err.message);
    } finally {
      setRegisterLoading(false);
    }
  };
  const handleVerify = async () => {
    if (!verifyToken) return;
    setVerifyError(null);
    setVerifyStatus(null);
    setVerifyLoading(true);
    try {
      const r = await api.verify(verifyToken);
      setVerifyStatus(r.message || "Email verified");
    } catch (err) {
      setVerifyError(err?.data?.error || err.message);
    } finally {
      setVerifyLoading(false);
    }
  };

  // Parse /auth/verify?token on mount when route matches
  useEffect(() => {
    if (location.pathname === '/auth/verify') {
      const params = new URLSearchParams(location.search);
      const t = params.get('token');
      if (t) setVerifyToken(t);
    }
  }, [location]);
  // (Больше не нужен повторный restore — уже взяли токены в начальном состоянии.)
  // restore pending OAuth 2FA state if present (need2fa + temp token persisted)
  useEffect(() => {
    if (!accessToken && !need2fa) {
      const pre = localStorage.getItem('pre2faTempToken');
      const flag = localStorage.getItem('pre2faFlag');
      if (pre && flag === '1') {
        setTempToken(pre);
        setNeed2fa(true);
      }
    }
  }, [accessToken, need2fa]);

  const handleLogin = async (e) => {
    e?.preventDefault?.();
    setLoginError(null);
    setNeed2fa(false); setTempToken(null); setTwofaCode("");
    setLoginLoading(true);
    try {
      const res = await api.login(email, password);
      if (res.need2fa) {
        setNeed2fa(true);
        setTempToken(res.tempToken);
      } else if (res.accessToken) {
        setAccessToken(res.accessToken);
        setRefreshToken(res.refreshToken || null);
        setTokens(res.accessToken, res.refreshToken || null);
        navigate('/profile'); // redirect after login
      } else {
        setLoginError('Unknown server response');
      }
    } catch (err) {
      setLoginError(err?.data?.error || err.message);
    } finally {
      setLoginLoading(false);
    }
  };

  const verify2fa = async () => {
    if (!twofaCode || !tempToken) return;
    setTwofaError(null); setTwofaLoading(true);
    try {
      const r = await api.authVerify2fa(tempToken, twofaCode);
      if (r.accessToken) {
        setAccessToken(r.accessToken);
        setRefreshToken(r.refreshToken || null);
        setTokens(r.accessToken, r.refreshToken || null);
        setNeed2fa(false); setTempToken(null); setTwofaCode("");
        localStorage.removeItem('pre2faTempToken');
        localStorage.removeItem('pre2faFlag');
        navigate('/profile');
      } else {
        setTwofaError('Response missing token');
      }
    } catch (err) {
      setTwofaError(err?.data?.error || err.message);
    } finally {
      setTwofaLoading(false);
    }
  };

  const handleLogout = () => {
    clearTokens();
    setAccessToken(null); setRefreshToken(null); setMe(null);
  };

  const handleRefresh = async () => {
    if (!refreshToken) return;
    try {
      const r = await api.refresh(refreshToken);
      if (r.accessToken) {
        setAccessToken(r.accessToken);
        setTokens(r.accessToken, refreshToken);
      }
    } catch (err) {
      console.warn('Refresh failed', err);
      handleLogout();
    }
  };

  const handleMe = async () => {
    if (!accessToken) return;
    try {
      const r = await api.me(accessToken);
      setMe(r.user || r);
    } catch (err) {
      console.warn('me failed', err);
    }
  };

  // Profile handlers
  const loadProfile = async () => {
    if (!accessToken) return;
    try {
      const r = await api.getProfile(accessToken);
      setProfile(r.profile || r || {});
    } catch (e) { console.warn('loadProfile', e); }
  };
  const saveProfile = async (eOrPayload) => {
    if (!accessToken) return;
    profileError && setProfileError(null);
    profileSuccess && setProfileSuccess(null);
    setProfileSaving(true);
    let src;
    if (eOrPayload && typeof eOrPayload.preventDefault === 'function') {
      eOrPayload.preventDefault();
      src = profile;
    } else {
      src = eOrPayload || profile;
    }
    const payload = {
      age: src.age !== '' && src.age != null ? Number(src.age) : null,
      gender: src.gender || null,
      heightCm: src.heightCm !== '' && src.heightCm != null ? Number(src.heightCm) : null,
      weightKg: src.weightKg !== '' && src.weightKg != null ? Number(src.weightKg) : null,
      targetWeightKg: src.targetWeightKg !== '' && src.targetWeightKg != null ? Number(src.targetWeightKg) : null,
      activityLevel: src.activityLevel || null,
      goal: src.goal || null,
    };
    try {
      const r = await api.saveProfile(accessToken, payload);
      const saved = r.profile || payload;
      setProfile(saved);
      setProfileSuccess('Profile saved');
      return saved;
    } catch (e) {
      console.warn('saveProfile', e);
      if (e.status === 400 && e.data?.error) {
        setProfileError(e.data.error);
      } else {
        setProfileError('Failed to save profile');
      }
      throw e;
    } finally {
      setProfileSaving(false);
    }
  };

  // Weight handlers
  const addWeight = async (payloadOrEvent) => {
    if (!accessToken) return;
    // Support both old form (event-based using global state) and new payload form
    let weightVal = null;
    let atVal = null;
    if (payloadOrEvent && typeof payloadOrEvent.preventDefault === 'function') {
      payloadOrEvent.preventDefault();
      weightVal = newWeight ? Number(newWeight) : null;
      atVal = weightAt || null;
    } else if (payloadOrEvent) {
      weightVal = payloadOrEvent.weight != null ? Number(payloadOrEvent.weight) : (newWeight ? Number(newWeight) : null);
      atVal = payloadOrEvent.at || weightAt || null;
    } else {
      weightVal = newWeight ? Number(newWeight) : null;
      atVal = weightAt || null;
    }
    try {
      const body = { weightKg: weightVal, at: atVal };
      // forward optional dietary arrays if provided in the payload
      if (payloadOrEvent && typeof payloadOrEvent === 'object') {
        if (Array.isArray(payloadOrEvent.dietaryPreferences)) body.dietaryPreferences = payloadOrEvent.dietaryPreferences;
        if (Array.isArray(payloadOrEvent.dietaryRestrictions)) body.dietaryRestrictions = payloadOrEvent.dietaryRestrictions;
      }
      await api.weightAdd(accessToken, body);
      setNewWeight(""); setWeightAt("");
      await loadWeights();
    } catch (e) { console.warn('addWeight', e); }
  };
  const loadWeights = async () => {
    if (!accessToken) return;
    try {
      const r = await api.weightList(accessToken);
      setWeights(Array.isArray(r.entries) ? r.entries : (Array.isArray(r) ? r : []));
    } catch (e) { console.warn('loadWeights', e); }
  };

  // Analytics handler
  const loadSummary = async () => {
    if (!accessToken) return;
    if (rateLimitUntil && Date.now() < rateLimitUntil) return; // paused
    try {
      const r = await api.analyticsSummary(accessToken);
      setSummary(r);
    } catch (e) {
      if (e.status === 429) handleRateLimit(e, 'summary'); else console.warn('loadSummary', e);
    }
  };

  // Activity handlers
  const addActivity = async (payload) => {
    if (!accessToken || !payload) return;
    try {
      await api.activityAdd(accessToken, {
        type: payload.type,
        minutes: Number(payload.minutes),
        intensity: payload.intensity,
        at: payload.at || null,
      });
    } catch (e1) { console.warn('addActivity', e1); }
  };
  const loadActivityWeek = async (isoDate) => {
    if (!accessToken) return;
    if (rateLimitUntil && Date.now() < rateLimitUntil) return;
    try {
      const r = await api.activityWeek(accessToken, isoDate);
      setWeek(r.summary || r);
    } catch (e) {
      if (e.status === 429) handleRateLimit(e, 'week'); else console.warn('loadActivityWeek', e);
    }
  };
  const loadActivityMonth = async () => {
    if (!accessToken) return;
    if (rateLimitUntil && Date.now() < rateLimitUntil) return;
    try {
      const now = new Date();
      const year = now.getFullYear();
      const month = now.getMonth() + 1;
      const r = await api.activityMonth(accessToken, year, month);
      setMonthData({
        year,
        month,
        total: Object.values(r.byDayMinutes || {}).reduce((s,v)=>s+v,0),
        daysActive: Object.values(r.byDayMinutes || {}).filter(v=>v>0).length,
        byDayMinutes: r.byDayMinutes || {},
      });
    } catch (e) {
      if (e.status === 429) handleRateLimit(e, 'month'); else console.warn('loadActivityMonth', e);
    }
  };

  // AI handlers
  const loadAiLatest = async () => {
    if (!accessToken) return;
    if (rateLimitUntil && Date.now() < rateLimitUntil) return;
    setAiLoading(true);
    try { const r = await api.aiLatest(accessToken, aiScope); setAi(r); }
    catch(e){ if (e.status === 429) handleRateLimit(e, 'ai'); else console.warn('aiLatest', e); }
    finally{ setAiLoading(false); }
  };
  const regenAi = async () => {
    if (!accessToken) return;
    setAiLoading(true);
    try { const r = await api.aiRegen(accessToken, aiScope); setAi(r); }
    catch(e){ console.warn('aiRegen', e); }
    finally{ setAiLoading(false); }
  };

  // Privacy handlers
  const loadConsent = async () => {
    if (!accessToken) return;
    try { const r = await api.privacyGet(accessToken); setConsentForm(f=>({ ...f, ...r })); setConsentLoaded(true); }
    catch(e){ console.warn('loadConsent', e); }
  };
  const saveConsent = async (e) => {
    e?.preventDefault?.(); if (!accessToken) return;
    try { await api.privacySet(accessToken, consentForm); }
    catch(e){ console.warn('saveConsent', e); }
  };

  // Dietary meta handlers (per-user, stored separately so saving does not alter weight history)
  const loadDietary = async () => {
    if (!accessToken) return { dietaryPreferences: [], dietaryRestrictions: [] };
    try { const r = await api.weightMetaGet(accessToken); return r || { dietaryPreferences: [], dietaryRestrictions: [] }; }
    catch(e){ console.warn('loadDietary', e); return { dietaryPreferences: [], dietaryRestrictions: [] }; }
  };
  const saveDietary = async (prefs, restrictions) => {
    if (!accessToken) return;
    try { await api.weightMetaSet(accessToken, { dietaryPreferences: prefs || [], dietaryRestrictions: restrictions || [] }); }
    catch(e){ console.warn('saveDietary', e); throw e; }
  };

  // After authentication, ensure consent is fetched and enforce redirect when any required consent is missing
  useEffect(() => {
    if (!accessToken) {
      setConsentLoaded(false);
      return;
    }
    // fetch consent on token change
    loadConsent().then(() => {
      // Enforce strict model: all three required consents must be enabled
      const allRequiredOk = !!consentForm.accepted && !!consentForm.allowAiUseProfile && !!consentForm.allowAiUseHistory;
      if (consentLoaded === true && !allRequiredOk && location.pathname !== '/privacy') {
        navigate('/privacy', { replace: true });
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [accessToken]);
  const exportData = async () => {
    if (!accessToken) return;
    try {
      const r = await api.privacyExport(accessToken);
      const blob = new Blob([JSON.stringify(r, null, 2)], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url; a.download = 'export.json'; a.click(); URL.revokeObjectURL(url);
    } catch(e){ console.warn('exportData', e); }
  };

  // Aggregated dashboard loader to simplify Dashboard.jsx logic
  const loadDashboard = async (force = false) => {
    if (!accessToken) return;
    if (!force && rateLimitUntil && Date.now() < rateLimitUntil) return; // respect rate limit window unless forced
    setDashboardError(null);
    setDashboardLoading(true);
    const tasks = [];
    // Only push tasks that still need data (avoid redundant calls if already present)
    if (!summary) tasks.push(loadSummary());
    if (!week) tasks.push(loadActivityWeek());
    if (!monthData) tasks.push(loadActivityMonth());
    if (!ai) tasks.push(loadAiLatest());
    if (!tasks.length) { setDashboardLoading(false); return; }
    const results = await Promise.allSettled(tasks);
    const errors = results.filter(r => r.status === 'rejected').map(r => r.reason);
    if (errors.length) {
      // If all failed and none were 429 (already handled), surface a generic error
      const non429 = errors.filter(e => !(e && e.status === 429));
      if (non429.length === errors.length) {
        setDashboardError(non429[0]?.message || 'Не удалось загрузить данные дашборда');
      }
    }
    setDashboardLoading(false);
  };

  // light OAuth URL helper
  function oauthUrl(provider){
    const base = import.meta.env.VITE_API_BASE_URL || import.meta.env.VITE_API_BASE || "";
    return `${base}/oauth2/authorization/${provider}`;
  }

  // Protected route wrapper
  const Protected = ({ children }) => {
    if (!accessToken) return <Navigate to="/login" replace />;
    // Enforce strict model: all three required consents must be enabled for protected routes
    const allRequiredOk = !!consentForm.accepted && !!consentForm.allowAiUseProfile && !!consentForm.allowAiUseHistory;
    if (consentLoaded && !allRequiredOk && location.pathname !== '/privacy') {
      return <Navigate to="/privacy" replace />;
    }
    return children;
  };

  // context passed to pages (minimal set)
  const ctx = {
    // auth & tokens
    accessToken, refreshToken, me,
    // registration
    email, setEmail, password, setPassword,
    verificationLink, registerError, registerLoading, handleRegister, verifyStatus,
    // email verification page
    verifyToken, setVerifyToken, handleVerify, verifyError, verifyLoading,
    // login & 2FA during login
    handleLogin, loginError, loginLoading, need2fa, twofaCode, setTwofaCode, verify2fa, twofaError, twofaLoading,
    // token ops
    handleRefresh, handleLogout, handleMe,
    // 2FA setup page
    twofaSetup, setTwofaSetup,
    // profile
    profile, setProfile, loadProfile, saveProfile, profileError, profileSuccess, profileSaving,
    // weight
    newWeight, setNewWeight, weightAt, setWeightAt, addWeight, loadWeights, weights, summary,
    // dietary meta
    loadDietary, saveDietary,
    // activity
    act, setAct, addActivity, period, setPeriod, loadActivityWeek, loadActivityMonth, week, monthData,
    // analytics
    loadSummary,
    // AI
    aiScope, setAiScope, loadAiLatest, regenAi, ai, aiLoading,
    // rate limit status
    rateLimitUntil,
    // dashboard aggregate
    dashboardLoading, dashboardError, loadDashboard,
    // privacy
    consentForm, setConsentForm, loadConsent, saveConsent, exportData,
    // export health
    exportHealth: (tokenArg) => api.exportHealth(tokenArg || accessToken),
    // helpers
    go: (path) => navigate(path), oauthUrl, api, tempToken,
  };
  return (
    <UserProvider>
      <div style={{ padding: 16, fontFamily: 'Arial, sans-serif' }}>
      <header style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
        <h1 style={{ margin: 0, fontSize: 18 }}>Numbers-Don't-Lie</h1>
        <nav style={{ marginLeft: 12, display: 'flex', gap: 8 }}>
          <Link to="/">Home</Link>
          <Link to="/register">Register</Link>
          <Link to="/activity">Activity</Link>
          <Link to="/weight">Weight</Link>
          <Link to="/analytics">Analytics</Link>
          <Link to="/ai">AI</Link>
          <Link to="/dashboard">Dashboard</Link>
          <Link to="/profile">Profile</Link>
          <Link to="/security">Security</Link>
          <Link to="/privacy">Privacy</Link>
            <Link to="/meals/today">Meal Plan</Link>
        </nav>
        <div style={{ marginLeft: 'auto' }}>
          {accessToken ? (
            <>
              <span style={{ marginRight: 8 }}>Signed in</span>
              <button onClick={handleLogout}>Logout</button>
            </>
          ) : (
            <Link to="/login">Login</Link>
          )}
        </div>
      </header>
      <main style={{ marginTop: 16 }}>
        <Routes>
          <Route path="/" element={<Home ctx={ctx} />} />
          <Route path="/register" element={<Register ctx={ctx} />} />
          <Route path="/login" element={<Login ctx={ctx} />} />
          <Route path="/privacy" element={<Privacy ctx={ctx} />} />
          <Route path="/activity" element={<Protected><Activity ctx={ctx} /></Protected>} />
          <Route path="/weight" element={<Protected><Weight ctx={ctx} /></Protected>} />
          <Route path="/analytics" element={<Protected><Analytics ctx={ctx} /></Protected>} />
          <Route path="/ai" element={<Protected><AI ctx={ctx} /></Protected>} />
          <Route path="/dashboard" element={<Protected><Dashboard ctx={ctx} /></Protected>} />
          <Route path="/profile" element={<Protected><Profile ctx={ctx} /></Protected>} />
          <Route path="/security" element={<Protected><Security ctx={ctx} /></Protected>} />
          <Route path="/auth/verify" element={<Verify ctx={ctx} />} />
          <Route path="/forgot" element={<Forgot ctx={ctx} />} />
          <Route path="/reset" element={<Reset ctx={ctx} />} />
          <Route path="/log" element={<Log ctx={ctx} />} />
          {/* Debug routes (read-only) */}
          <Route path="/debug/meal-plans" element={<DebugMealPlanPage />} />
          <Route path="/debug/meal-plan" element={<DebugMealPlanPage />} />
            <Route path="/meals/today" element={<MealPlanPage />} />
          <Route
            path="/oauth-callback"
            element={
              <OAuthCallback
                onTokens={({ access, refresh }) => {
                  setAccessToken(access);
                  setRefreshToken(refresh);
                  setTokens(access, refresh);
                  navigate('/profile');
                }}
              />
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </div>
    </UserProvider>
  );
}
