package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.health.HealthCalc;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.entity.WeightEntry;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.WeightEntryRepository;
import com.ndl.numbers_dont_lie.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {
  private final JwtService jwt;
  private final UserRepository users;
  private final ProfileRepository profiles;
  private final WeightEntryRepository weights;

  public AnalyticsController(JwtService jwt, UserRepository users, ProfileRepository profiles, WeightEntryRepository weights) {
    this.jwt = jwt; this.users = users; this.profiles = profiles; this.weights = weights;
  }

  private String emailFrom(String auth) {
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing or invalid Authorization header");
    String t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token type"); return jwt.getEmail(t); }
    catch (JwtException e) { throw new IllegalStateException("Invalid or expired token"); }
  }

  @GetMapping("/summary")
  public ResponseEntity<?> summary(@RequestHeader(value="Authorization", required=false) String auth) {
    try {
      String email = emailFrom(auth);
      UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
      ProfileEntity p = profiles.findByUser(user).orElse(null);
      if (p == null || p.getHeightCm() == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "Profile with height (cm) required"));
      }

      // берём последний вес: либо из профиля, либо из weight history
      List<WeightEntry> list = weights.findAllByUserOrderByAtAsc(user);
      Double latestWeight = (list.isEmpty() ? p.getWeightKg() : list.get(list.size()-1).getWeightKg());
      if (latestWeight == null) {
        return ResponseEntity.badRequest().body(Map.of("error", "No weight available"));
      }

      var bmi = HealthCalc.bmi(latestWeight, p.getHeightCm());
      double bmiScore = HealthCalc.bmiScoreComponent(bmi.classification);
      double activityScore = HealthCalc.activityScore(p.getActivityLevel());
      // пока нет цели веса в профиле — используем profile.weightKg как «цель» для примера
      double progressScore = HealthCalc.progressScore(latestWeight, p.getWeightKg());
      double habitsScore = HealthCalc.habitsScore(p.getActivityLevel());
      double wellness = HealthCalc.wellness(bmiScore, activityScore, progressScore, habitsScore);

      return ResponseEntity.ok(Map.of(
        "bmi", Map.of("value", bmi.bmi, "classification", bmi.classification),
        "scores", Map.of(
          "bmi", bmiScore,
          "activity", activityScore,
          "progress", progressScore,
          "habits", habitsScore,
          "wellness", wellness
        ),
        "latestWeightKg", latestWeight,
        "heightCm", p.getHeightCm(),
        "activityLevel", p.getActivityLevel(),
        "goal", p.getGoal()
      ));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
