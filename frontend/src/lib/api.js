import { getAccessToken, getRefreshToken, setTokens } from "./tokens";

// Backend base URL. Set VITE_API_BASE in .env to override. Default points to Spring Boot backend on 8080.
const BASE = import.meta.env.VITE_API_BASE || "http://localhost:8080";

let refreshInFlight = null;

// Single-flight map to coalesce identical GETs; short-lived cache to reduce hammering endpoints
const singleFlight = new Map();
const shortCache = new Map(); // key -> { expiry, data }

function buildKey(path, { method, body, token }) {
  return `${method || 'GET'}:${path}|${token || ''}|${body ? JSON.stringify(body) : ''}`;
}

async function request(path, { method = "GET", body, token, _retried, _fromRetry } = {}) {
  const headers = {};
  if (body != null && method !== "GET") headers["Content-Type"] = "application/json";
  const access = token || getAccessToken();
  const isAuthPublic = path.startsWith("/auth/");
  if (access && !isAuthPublic) headers.Authorization = `Bearer ${access}`;

  const key = buildKey(path, { method, body, token: access });

  if (method === 'GET' && !_retried && !_fromRetry) {
    const cached = shortCache.get(key);
    if (cached && cached.expiry > Date.now()) {
      return cached.data;
    }
  }
  if (method === 'GET' && singleFlight.has(key)) {
    return singleFlight.get(key);
  }

  const promise = (async () => {
    const res = await fetch(`${BASE}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    let data = null;
    try { data = text ? JSON.parse(text) : null; } catch { data = null; }

    if (!res.ok) {
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
            return request(path, { method, body, token: tData.accessToken, _retried: true });
          } catch {
            // fall through
          } finally {
            setTimeout(() => { refreshInFlight = null; }, 50);
          }
        }
      }
      if (res.status === 429 && !_fromRetry) {
        const retryAfterHeader = res.headers.get('retry-after');
        const retrySec = retryAfterHeader ? parseInt(retryAfterHeader, 10) : NaN;
        // Enforce minimum 60s wait; if server suggests longer, respect it.
        let delayMs = 60000;
        if (!isNaN(retrySec)) {
          delayMs = Math.max(retrySec * 1000, 60000);
        }
        return new Promise((resolve, reject) => {
          setTimeout(() => {
            request(path, { method, body, token: access, _fromRetry: true })
              .then(resolve)
              .catch(reject);
          }, delayMs);
        });
      }
      const err = new Error(data?.error || `HTTP ${res.status}`);
      err.status = res.status;
      const hObj = {}; res.headers.forEach((v,k)=>{ hObj[k.toLowerCase()] = v; });
      err.headers = hObj;
      err.data = data;
      throw err;
    }
    if (method === 'GET') {
      // Extend cache TTL to 30s to reduce request frequency & chance of hitting rate limits
      shortCache.set(key, { expiry: Date.now() + 30000, data });
    }
    return data;
  })();

  if (method === 'GET') singleFlight.set(key, promise);
  try { return await promise; } finally { if (method === 'GET') singleFlight.delete(key); }
}

export const api = {
  register: (email, password) => request("/auth/register", { method: "POST", body: { email, password } }),
  verify: (token) => request(`/auth/verify?token=${encodeURIComponent(token)}`),
  login: (email, password) => request("/auth/login", { method: "POST", body: { email, password } }),
  refresh: (refreshToken) => request("/auth/refresh", { method: "POST", body: { refreshToken } }),
  me: (accessToken) => request("/protected/me", { token: accessToken }),
  getProfile: (accessToken) => request("/profile", { token: accessToken }),
  saveProfile: (accessToken, payload) => request("/profile", { method: "PUT", token: accessToken, body: payload }),
  // Accepts body: { weightKg, at, dietaryPreferences?, dietaryRestrictions? }
  weightAdd: (token, body) =>
  request("/progress/weight", { method: "POST", token, body }),
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
  exportHealth: (token) => request("/export/health", { token }),
  // Dietary meta for weight (per-user preferences stored separately from weight entries)
  weightMetaGet: (token) => request("/progress/weight/meta", { token }),
  weightMetaSet: (token, body) => request("/progress/weight/meta", { method: "PUT", token, body }),
  // AI insights
  aiLatest: (token, scope = "weekly") => request(`/ai/insights/latest?scope=${encodeURIComponent(scope)}`, { token }),
  aiRegen: (token, scope = "weekly") => request(`/ai/insights/regenerate?scope=${encodeURIComponent(scope)}`, { method: "POST", token }),
  aiStatus: (token) => request(`/ai/status`, { token }),
  aiSetStatus: (token, enabled) => request(`/ai/status`, { method: "PUT", token, body: { enabled } }),
  // 2FA
  twofaEnroll: (token) => request("/2fa/enroll", { method: "POST", token }),
  twofaVerifySetup: (token, code) => request("/2fa/verify-setup", { method: "POST", token, body: { code } }),
  twofaDisable: (token, codeOrRecovery) => request("/2fa/disable", { method: "POST", token, body: { codeOrRecovery } }),
  authVerify2fa: (tempToken, code) => request("/auth/2fa/verify", { method: "POST", body: { tempToken, code } }),
};
