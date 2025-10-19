package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.dto.ProfileUpsertDto;
import com.ndl.numbers_dont_lie.service.ProfileService;
import com.ndl.numbers_dont_lie.service.JwtService; // где лежит твой JwtService – поправь пакет на свой
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private final JwtService jwt;
  private final ProfileService service;

  public ProfileController(JwtService jwt, ProfileService service) {
    this.jwt = jwt;
    this.service = service;
  }

  private String emailFromAuth(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer "))
      throw new IllegalStateException("Missing or invalid Authorization header");
    String token = authHeader.substring("Bearer ".length());
    try {
      if (!jwt.isAccessToken(token)) throw new IllegalStateException("Invalid token type");
      return jwt.getEmail(token);
    } catch (JwtException e) {
      throw new IllegalStateException("Invalid or expired token");
    }
  }

  @GetMapping
  public ResponseEntity<?> get(@RequestHeader(value="Authorization", required=false) String auth) {
    try {
      String email = emailFromAuth(auth);
      var p = service.get(email);
      return ResponseEntity.ok(Map.of("profile", p));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping
  public ResponseEntity<?> upsert(@RequestHeader(value="Authorization", required=false) String auth,
                                  @RequestBody ProfileUpsertDto dto) {
    try {
      String email = emailFromAuth(auth);
      var p = service.upsert(email, dto);
      return ResponseEntity.ok(Map.of("profile", p));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
