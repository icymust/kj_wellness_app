package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.dto.AiInsightsResponse;
import com.ndl.numbers_dont_lie.service.AiInsightsService;
import com.ndl.numbers_dont_lie.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AiController {
  private final JwtService jwt;
  private final AiInsightsService service;

  public AiController(JwtService jwt, AiInsightsService service) { this.jwt = jwt; this.service = service; }

  private String emailFrom(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing or invalid Authorization header");
    String t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token type"); return jwt.getEmail(t); }
    catch (JwtException e) { throw new IllegalStateException("Invalid or expired token"); }
  }

  @GetMapping("/insights/latest")
  public ResponseEntity<?> latest(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @RequestParam(value = "scope", required = false) String scope) {
    try {
      String email = emailFrom(auth);
      AiInsightsResponse resp = service.latest(email, scope, false);
      return ResponseEntity.ok(resp);
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/insights/regenerate")
  public ResponseEntity<?> regenerate(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @RequestParam(value = "scope", required = false) String scope) {
    try {
      String email = emailFrom(auth);
      AiInsightsResponse resp = service.latest(email, scope, true);
      return ResponseEntity.ok(resp);
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
