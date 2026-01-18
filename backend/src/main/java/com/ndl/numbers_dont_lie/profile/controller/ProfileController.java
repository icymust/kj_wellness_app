package com.ndl.numbers_dont_lie.profile.controller;

import com.ndl.numbers_dont_lie.profile.dto.ProfileUpsertDto;
import com.ndl.numbers_dont_lie.profile.service.ProfileService;
import com.ndl.numbers_dont_lie.dto.nutrition.NutritionalPreferencesDto;
import com.ndl.numbers_dont_lie.service.nutrition.NutritionalPreferencesService;
import com.ndl.numbers_dont_lie.auth.service.JwtService;
import com.ndl.numbers_dont_lie.model.nutrition.NutritionalPreferencesConstants;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
public class ProfileController {

  private final JwtService jwt;
  private final ProfileService service;
  private final NutritionalPreferencesService nutritionalPreferencesService;

  public ProfileController(JwtService jwt, ProfileService service, NutritionalPreferencesService nutritionalPreferencesService) {
    this.jwt = jwt;
    this.service = service;
    this.nutritionalPreferencesService = nutritionalPreferencesService;
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
      // Дополнительная простая валидация "обязательных" положительных значений для корректного BMI / логики профиля
      // (расширяет существующие проверки диапазона внутри ProfileService)
      StringBuilder errs = new StringBuilder();
      if (dto.heightCm != null && dto.heightCm <= 0) {
        errs.append("Height must be greater than 0");
      }
      if (dto.weightKg != null && dto.weightKg <= 0) {
        if (errs.length() > 0) errs.append(". ");
        errs.append("Weight must be greater than 0");
      }
      if (dto.age != null && dto.age < 0) {
        if (errs.length() > 0) errs.append(". ");
        errs.append("Age cannot be negative");
      }
      if (errs.length() > 0) {
        return ResponseEntity.badRequest().body(Map.of("error", errs.toString()));
      }
      var p = service.upsert(email, dto);
      return ResponseEntity.ok(Map.of("profile", p));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @GetMapping("/nutritional-preferences")
  public ResponseEntity<?> getNutritionalPreferences(@RequestHeader(value="Authorization", required=false) String auth) {
    try {
      String email = emailFromAuth(auth);
      var prefs = nutritionalPreferencesService.get(email);
      return ResponseEntity.ok(Map.of(
              "nutritionalPreferences", prefs,
              "availableDietaryPreferences", NutritionalPreferencesConstants.AVAILABLE_DIETARY_PREFERENCES,
              "availableAllergies", NutritionalPreferencesConstants.AVAILABLE_ALLERGIES
      ));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }

  @PutMapping("/nutritional-preferences")
  public ResponseEntity<?> updateNutritionalPreferences(@RequestHeader(value="Authorization", required=false) String auth,
                                                        @RequestBody NutritionalPreferencesDto dto) {
    try {
      String email = emailFromAuth(auth);
      var prefs = nutritionalPreferencesService.upsert(email, dto);
      return ResponseEntity.ok(Map.of("nutritionalPreferences", prefs));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
