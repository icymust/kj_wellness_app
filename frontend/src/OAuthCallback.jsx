import { useEffect } from "react";

export default function OAuthCallback({ onTokens }) {
  useEffect(() => {
    const hash = window.location.hash.startsWith("#") ? window.location.hash.slice(1) : "";
    const params = new URLSearchParams(hash);
    const access = params.get("accessToken");
    const refresh = params.get("refreshToken");
    const error = params.get("error");

    if (error) {
      alert("OAuth error: " + error);
    } else if (access && refresh) {
      onTokens?.({ access, refresh });
      localStorage.setItem("accessToken", access);
      localStorage.setItem("refreshToken", refresh);
      alert("OAuth login success");
    }
    window.location.replace("/");
  }, []);
  return <div style={{padding:16}}>Finishing OAuth login…</div>;
}
