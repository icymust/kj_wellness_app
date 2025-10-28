const BASE = import.meta.env.VITE_API_BASE || "http://localhost:5173";

async function request(path, { method = "GET", body, token } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

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
};
