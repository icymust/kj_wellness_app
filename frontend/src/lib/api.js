import { getAccessToken, getRefreshToken, setTokens } from "./tokens";

const BASE = import.meta.env.VITE_API_BASE || "http://localhost:5173";

let refreshInFlight = null;

async function request(path, { method = "GET", body, token, _retried } = {}) {
  const headers = {};
  // Only set JSON content-type when we actually send a body (avoids CORS preflight on simple GETs)
  if (body != null && method !== "GET") headers["Content-Type"] = "application/json";
  const access = token || getAccessToken();
  // Don't send Authorization for public auth endpoints to avoid confusing flows with stale tokens
  const isAuthPublic = path.startsWith("/auth/");
  if (access && !isAuthPublic) headers.Authorization = `Bearer ${access}`;

  // Debug: log outgoing request details to help diagnose network / CORS issues
  console.debug("API request:", { url: `${BASE}${path}`, method, hasToken: !!token, body });

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
    // keep default mode; CORS must be allowed server-side
  });

  const text = await res.text();
  const data = text ? JSON.parse(text) : null;

  if (!res.ok) {
    // If access token expired → try refresh once
    if (res.status === 401 && !_retried && !path.startsWith("/auth/")) {
      const refresh = getRefreshToken();
      if (refresh) {
        try {
          if (!refreshInFlight) {
            refreshInFlight = (async () => {
              const r = await fetch(`${BASE}/auth/refresh`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ refreshToken: refresh }),
              });
              const tText = await r.text();
              const tData = tText ? JSON.parse(tText) : null;
              if (r.ok && tData?.accessToken) {
                setTokens(tData.accessToken, tData.refreshToken || refresh);
                return tData;
              }
              throw new Error(tData?.error || `Refresh failed: ${r.status}`);
            })();
          }
          const tData = await refreshInFlight;
          // retry original request with new access
          return request(path, { method, body, token: tData.accessToken, _retried: true });
        } catch {
          // ignore and fall through to throw original error
        } finally {
          // reset only if we were the creator
          // small timeout to coalesce burst of 401s
          setTimeout(() => { refreshInFlight = null; }, 50);
        }
      }
    }
    const err = new Error(data?.error || `HTTP ${res.status}`);
    err.status = res.status;
    err.data = data;
    throw err;
  }
  return data;
}

export const api = {
  register: (email, password) => request("/auth/register", { method: "POST", body: { email, password } }),
  verify: (token) => request(`/auth/verify?token=${encodeURIComponent(token)}`),
  login: (email, password) => request("/auth/login", { method: "POST", body: { email, password } }),
  refresh: (refreshToken) => request("/auth/refresh", { method: "POST", body: { refreshToken } }),
  me: (accessToken) => request("/protected/me", { token: accessToken }),
  getProfile: (accessToken) => request("/profile", { token: accessToken }),
  saveProfile: (accessToken, payload) => request("/profile", { method: "PUT", token: accessToken, body: payload }),
  weightAdd: (token, weightKg, at) =>
  request("/progress/weight", { method: "POST", token, body: { weightKg, at } }),
  weightList: (token) => request("/progress/weight", { token }),
  analyticsSummary: (token) => request("/analytics/summary", { token }),
  activityAdd: (token, payload) =>
    request("/progress/activity", { method: "POST", token, body: payload }),
  activityWeek: (token, isoDate) =>
    request(`/progress/activity/week${isoDate ? `?date=${isoDate}` : ""}`, { token }),
  activityMonth: (token, year, month) =>
    request(`/progress/activity/month?year=${year}&month=${month}`, { token }),
  forgot: (email) => request("/auth/forgot", { method: "POST", body: { email } }),
  resetPwd: (token, password) => request("/auth/reset", { method: "POST", body: { token, password } }),
  devOutbox: () => request("/dev/outbox"),
  // Privacy
  privacyGet: (token) => request("/privacy/consent", { token }),
  privacySet: (token, body) => request("/privacy/consent", { method: "PUT", token, body }),
  privacyExport: (token) => request("/privacy/export", { token }),
  // AI insights
  aiLatest: (token, scope = "weekly") => request(`/ai/insights/latest?scope=${encodeURIComponent(scope)}`, { token }),
  aiRegen: (token, scope = "weekly") => request(`/ai/insights/regenerate?scope=${encodeURIComponent(scope)}`, { method: "POST", token }),
  // 2FA
  twofaEnroll: (token) => request("/2fa/enroll", { method: "POST", token }),
  twofaVerifySetup: (token, code) => request("/2fa/verify-setup", { method: "POST", token, body: { code } }),
  twofaDisable: (token, codeOrRecovery) => request("/2fa/disable", { method: "POST", token, body: { codeOrRecovery } }),
  authVerify2fa: (tempToken, code) => request("/auth/2fa/verify", { method: "POST", body: { tempToken, code } }),
};
