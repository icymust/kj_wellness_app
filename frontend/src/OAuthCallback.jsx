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

    let redirect = "/";
    if (error) {
      alert("OAuth error: " + error);
      redirect = "/login";
    } else if (access && refresh) {
      localStorage.setItem("accessToken", access);
      localStorage.setItem("refreshToken", refresh);
      onTokens?.({ access, refresh });
      alert("OAuth login success");
      redirect = "/profile";
    } else if (need2fa === "1" && tempToken) {
      localStorage.setItem("pre2faTempToken", tempToken);
      localStorage.setItem("pre2faFlag", "1");
      alert("Two-factor authentication required — please enter your code.");
      redirect = "/login";
    }
    window.location.replace(redirect);
  }, [onTokens]);
  return <div style={{padding:16}}>Finishing OAuth login…</div>;
}
