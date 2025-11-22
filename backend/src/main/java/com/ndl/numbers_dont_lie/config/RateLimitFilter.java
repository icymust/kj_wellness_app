package com.ndl.numbers_dont_lie.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter (no external dependency).
 * Default & overrides are now configurable via application.yml (app.rate-limit.*).
 * Not suitable for multi-instance production — use Redis or a shared store there.
 */
@Component
public class RateLimitFilter implements Filter {
  private final int defaultLimit;
  private final long windowSeconds;
  private final Map<String,Integer> overrides;

  private final Map<String, Deque<Long>> buckets = new ConcurrentHashMap<>();

  public RateLimitFilter(
      @org.springframework.beans.factory.annotation.Value("${app.rate-limit.default-per-minute:120}") int defaultLimit,
      @org.springframework.beans.factory.annotation.Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
      @org.springframework.beans.factory.annotation.Value("${app.rate-limit.override.auth2faVerify:10}") int auth2faVerify,
      @org.springframework.beans.factory.annotation.Value("${app.rate-limit.override.twofaVerifySetup:10}") int twofaVerifySetup
  ) {
    this.defaultLimit = defaultLimit;
    this.windowSeconds = windowSeconds;
    this.overrides = Map.of(
        "/auth/2fa/verify", auth2faVerify,
        "/2fa/verify-setup", twofaVerifySetup
    );
  }

  private String clientKey(HttpServletRequest req) {
    String email = (String) req.getAttribute("AUTH_EMAIL");
    if (email != null && !email.isBlank()) return "USR:" + email;
    String ip = req.getHeader("X-Forwarded-For");
    if (ip == null || ip.isBlank()) ip = req.getRemoteAddr();
    return "IP:" + ip;
  }

  private boolean isExempt(HttpServletRequest req) {
    // Do not rate-limit CORS preflight and some public endpoints
    if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
    String p = req.getRequestURI();
    return p.equals("/health") || p.startsWith("/swagger") || p.startsWith("/actuator") || p.startsWith("/static");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (isExempt(req)) {
      chain.doFilter(request, response);
      return;
    }

  String key = clientKey(req);
    Deque<Long> deque = buckets.computeIfAbsent(key, k -> new ArrayDeque<>());
    int limit = overrides.getOrDefault(req.getRequestURI(), defaultLimit);

    long now = Instant.now().getEpochSecond();
    synchronized (deque) {
      // remove old timestamps
      while (!deque.isEmpty() && deque.peekFirst() <= now - windowSeconds) {
        deque.pollFirst();
      }

      if (deque.size() < limit) {
        deque.addLast(now);
        chain.doFilter(request, response);
        return;
      }

      long oldest = deque.peekFirst();
  long retryAfter = Math.max(1, (oldest + windowSeconds) - now);

      res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
      res.setContentType("application/json");
      res.setHeader("Retry-After", String.valueOf(retryAfter));
      res.getWriter().write("{\"error\":\"rate_limited\",\"limitPerMinute\":"+limit+",\"retryAfterSec\":"+retryAfter+"}");
    }
  }
}
