package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.dto.AiInsightsResponse;
import com.ndl.numbers_dont_lie.service.AIRecommendationService;
import com.ndl.numbers_dont_lie.service.JwtService;
import com.ndl.numbers_dont_lie.service.AiAvailabilityService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
  private final JwtService jwt;
  private final AIRecommendationService service;
  private final AiAvailabilityService availability;

  public AiController(JwtService jwt, AIRecommendationService service, AiAvailabilityService availability) {
    this.jwt = jwt; this.service = service; this.availability = availability; }

  private String emailFrom(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing or invalid Authorization header");
    String t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token type"); return jwt.getEmail(t); }
    catch (JwtException e) { throw new IllegalStateException("Invalid or expired token"); }
  }

  /**
   * Load latest cached recommendation only; no recomputation.
   */
  @GetMapping("/insights/latest")
  public ResponseEntity<?> latest(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @RequestParam(value = "scope", required = false) String scope) {
    try {
      String email = emailFrom(auth);
      return service.loadLatest(email, scope)
        .<ResponseEntity<?>>map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(204).build());
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * Regenerate recommendation deterministically and save; if AI disabled, return cached only (or 503).
   */
  @PostMapping("/insights/regenerate")
  public ResponseEntity<?> regenerate(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @RequestParam(value = "scope", required = false) String scope) {
    try {
      String email = emailFrom(auth);
      if (!availability.isEnabled()) {
        return ResponseEntity.status(503).body(Map.of("error", "AI service unavailable"));
      }
      AiInsightsResponse resp = service.regenerate(email, scope);
      return ResponseEntity.ok(resp);
    } catch (IllegalStateException e) {
      String msg = e.getMessage() != null ? e.getMessage() : "Service unavailable";
      if (msg.toLowerCase().contains("unavailable")) {
        return ResponseEntity.status(503).body(Map.of("error", msg));
      }
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  /**
   * AI availability status endpoints
   */
  @GetMapping("/status")
  public ResponseEntity<?> status(@RequestHeader(value = "Authorization", required = false) String auth) {
    try {
      emailFrom(auth); // just to validate access token
      return ResponseEntity.ok(Map.of("enabled", availability.isEnabled()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/status")
  public ResponseEntity<?> setStatus(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @RequestBody Map<String, Object> body) {
    try {
      emailFrom(auth); // permission model simple: any authenticated user can toggle for demo
      Object val = body.get("enabled");
      if (!(val instanceof Boolean)) {
        return ResponseEntity.badRequest().body(Map.of("error", "Field 'enabled' must be boolean"));
      }
      availability.setEnabled((Boolean) val);
      return ResponseEntity.ok(Map.of("enabled", availability.isEnabled()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
