package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.dto.WeightDto;
import com.ndl.numbers_dont_lie.service.JwtService;
import com.ndl.numbers_dont_lie.service.WeightService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/progress/weight")
public class WeightController {
  private final JwtService jwt;
  private final WeightService service;

  public WeightController(JwtService jwt, WeightService service) {
    this.jwt = jwt; this.service = service;
  }

  private String emailFrom(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing or invalid Authorization header");
    String t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token type"); return jwt.getEmail(t); }
    catch (JwtException e) { throw new IllegalStateException("Invalid or expired token"); }
  }

  @PostMapping
  public ResponseEntity<?> add(@RequestHeader(value="Authorization", required=false) String auth,
                               @RequestBody WeightDto dto) {
    try { var e = service.add(emailFrom(auth), dto); return ResponseEntity.status(201).body(Map.of("entry", e)); }
    catch (IllegalArgumentException ex) { return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage())); }
    catch (IllegalStateException ex) { return ResponseEntity.status(409).body(Map.of("error", ex.getMessage())); } // дубликат → 409
  }

  @GetMapping
  public ResponseEntity<?> list(@RequestHeader(value="Authorization", required=false) String auth) {
    try { return ResponseEntity.ok(Map.of("entries", service.list(emailFrom(auth)))); }
    catch (IllegalStateException ex) { return ResponseEntity.status(401).body(Map.of("error", ex.getMessage())); }
  }
}
