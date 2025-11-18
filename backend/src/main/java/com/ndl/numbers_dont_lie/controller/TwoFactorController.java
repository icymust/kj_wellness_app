package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.service.JwtService;
import com.ndl.numbers_dont_lie.service.TwoFactorService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class TwoFactorController {
  private final JwtService jwt;
  private final TwoFactorService svc;

  public TwoFactorController(JwtService jwt, TwoFactorService svc) {
    this.jwt = jwt; this.svc = svc;
  }

  private String emailFrom(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing or invalid Authorization header");
    String t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token type"); return jwt.getEmail(t); }
    catch (JwtException e) { throw new IllegalStateException("Invalid or expired token"); }
  }

  @PostMapping("/2fa/enroll")
  public ResponseEntity<?> enroll(@RequestHeader(value = "Authorization", required = false) String auth) {
    try {
      String email = emailFrom(auth);
      return ResponseEntity.ok(svc.enroll(email));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/2fa/verify-setup")
  public ResponseEntity<?> verifySetup(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestBody Map<String,String> body) {
    try {
      String email = emailFrom(auth);
      String code = body.get("code");
      if (code == null) return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
      return ResponseEntity.ok(svc.verifySetup(email, code));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PostMapping("/2fa/disable")
  public ResponseEntity<?> disable(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestBody Map<String,String> body) {
    try {
      String email = emailFrom(auth);
      String code = body.get("codeOrRecovery");
      if (code == null) return ResponseEntity.badRequest().body(Map.of("error", "Invalid input"));
      return ResponseEntity.ok(svc.disable(email, code));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
