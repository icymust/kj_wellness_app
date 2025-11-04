// Simple token storage with LocalStorage and a broadcast event
const ACCESS_KEY = "accessToken";
const REFRESH_KEY = "refreshToken";

export function getAccessToken() {
  try { return localStorage.getItem(ACCESS_KEY) || ""; }
  catch { /* ignore storage errors (SSR/private mode) */ return ""; }
}
export function getRefreshToken() {
  try { return localStorage.getItem(REFRESH_KEY) || ""; }
  catch { /* ignore storage errors (SSR/private mode) */ return ""; }
}
export function setTokens(access, refresh) {
  try {
    if (access != null) localStorage.setItem(ACCESS_KEY, access);
    if (refresh != null) localStorage.setItem(REFRESH_KEY, refresh);
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("ndl:toks", { detail: { access, refresh } }));
    }
  } catch { /* ignore storage errors (SSR/private mode) */ }
}
export function clearTokens() {
  try {
    localStorage.removeItem(ACCESS_KEY);
    localStorage.removeItem(REFRESH_KEY);
    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("ndl:toks", { detail: { access: "", refresh: "" } }));
    }
  } catch { /* ignore storage errors (SSR/private mode) */ }
}
