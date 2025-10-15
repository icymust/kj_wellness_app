const BASE = import.meta.env.VITE_API_BASE || "http://localhost:5173";

async function request(path, { method = "GET", body, token } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
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
};
