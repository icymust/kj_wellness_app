package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.health.HealthCalc;
import com.ndl.numbers_dont_lie.health.GoalProgress;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.entity.WeightEntry;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.WeightEntryRepository;
import com.ndl.numbers_dont_lie.service.JwtService;
import com.ndl.numbers_dont_lie.service.ActivityService;
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
  private final ActivityService activityService;

  public AnalyticsController(JwtService jwt, UserRepository users, ProfileRepository profiles, WeightEntryRepository weights, ActivityService activityService) {
    this.jwt = jwt; this.users = users; this.profiles = profiles; this.weights = weights; this.activityService = activityService;
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
  // incorporate weekly activity summary as a booster
  var week = activityService.weekSummary(email, java.time.LocalDate.now());
  double profileActivity = HealthCalc.activityScore(p.getActivityLevel());
  double weeklyBoost     = HealthCalc.weeklyActivityBooster(week.totalMinutes, week.daysActive);

  // Берём "лучшее из двух": профиль или реальная неделя
  double activityScore = Math.max(profileActivity, weeklyBoost);
  // цель веса: используем profile.targetWeightKg если задано, иначе profile.weightKg
  Double target = p.getTargetWeightKg() != null ? p.getTargetWeightKg() : p.getWeightKg();
  double progressScore = HealthCalc.progressScore(latestWeight, target);
  double habitsScore = activityScore; // пока используем то же значение
  double wellness = HealthCalc.wellness(bmiScore, activityScore, progressScore, habitsScore);

      var initialWeight = (list.isEmpty() ? p.getWeightKg() : list.get(0).getWeightKg());
      var gp = GoalProgress.progress(initialWeight, target, latestWeight);

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
        "goal", Map.of(
          "targetWeightKg", target,
          "initialWeightKg", initialWeight,
          "currentWeightKg", latestWeight,
          "progress", Map.of(
            "percent", gp.percent,
            "coveredKg", gp.coveredKg,
            "remainingKg", gp.remainingKg,
            "milestones5pct", gp.milestones5,
            "direction", (latestWeight != null && initialWeight != null && latestWeight.doubleValue() > initialWeight.doubleValue()) ? "away" : "towards",
            "bravo", gp.percent != null && gp.percent >= 100 ? "goal_reached" : (gp.percent != null && gp.percent >= 50 ? "on_track" : "keep_going")
          )
        )
      ));
    } catch (IllegalStateException e) {
      return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
    }
  }
}
