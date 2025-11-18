import { useEffect } from "react";

export default function OAuthCallback({ onTokens }) {
  useEffect(() => {
    const hash = window.location.hash.startsWith("#") ? window.location.hash.slice(1) : "";
    const params = new URLSearchParams(hash);
    const access = params.get("accessToken");
    const refresh = params.get("refreshToken");
    const need2fa = params.get("need2fa");
    const tempToken = params.get("tempToken");
    const error = params.get("error");

    if (error) {
      alert("OAuth error: " + error);
    } else if (access && refresh) {
      onTokens?.({ access, refresh });
      localStorage.setItem("accessToken", access);
      localStorage.setItem("refreshToken", refresh);
      alert("OAuth login success");
    } else if (need2fa === "1" && tempToken) {
      // Store temporary pre-2FA token and let the main app show the 2FA verify box
      localStorage.setItem("pre2faTempToken", tempToken);
      alert("Two-factor authentication required — please enter your code.");
    }
    window.location.replace("/");
  }, []);
  return <div style={{padding:16}}>Finishing OAuth login…</div>;
}
