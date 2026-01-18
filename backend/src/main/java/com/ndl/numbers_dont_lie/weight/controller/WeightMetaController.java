package com.ndl.numbers_dont_lie.weight.controller;
import com.ndl.numbers_dont_lie.auth.service.JwtService;
import com.ndl.numbers_dont_lie.weight.service.WeightService;
import com.ndl.numbers_dont_lie.dto.DietaryDto;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/progress/weight/meta")
public class WeightMetaController {
  private final JwtService jwt;
  private final WeightService service;

  public WeightMetaController(JwtService jwt, WeightService service) { this.jwt = jwt; this.service = service; }

  private String emailFrom(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing or invalid Authorization header");
    String t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token type"); return jwt.getEmail(t); }
    catch (JwtException e) { throw new IllegalStateException("Invalid or expired token"); }
  }

  @GetMapping
  public ResponseEntity<?> get(@RequestHeader(value="Authorization", required=false) String auth) {
    try {
      var map = service.loadDietary(emailFrom(auth));
      return ResponseEntity.ok(map);
    } catch (IllegalStateException ex) { return ResponseEntity.status(401).body(Map.of("error", ex.getMessage())); }
  }

  @PutMapping
  public ResponseEntity<?> put(@RequestHeader(value="Authorization", required=false) String auth,
                               @RequestBody DietaryDto dto) {
    try {
      service.saveDietary(emailFrom(auth), dto.dietaryPreferences, dto.dietaryRestrictions);
      return ResponseEntity.ok(Map.of("status", "ok"));
    } catch (IllegalStateException ex) { return ResponseEntity.status(401).body(Map.of("error", ex.getMessage())); }
  }
}
