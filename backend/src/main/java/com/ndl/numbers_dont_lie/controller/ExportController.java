package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.entity.WeightEntry;
import com.ndl.numbers_dont_lie.entity.ActivityEntry;
import com.ndl.numbers_dont_lie.entity.UserConsent;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.WeightEntryRepository;
import com.ndl.numbers_dont_lie.repository.ActivityEntryRepository;
import com.ndl.numbers_dont_lie.repository.UserConsentRepository;
import com.ndl.numbers_dont_lie.service.JwtService;
import com.ndl.numbers_dont_lie.service.PrivacyService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/export")
public class ExportController {

  private final JwtService jwt;
  private final UserRepository users;
  private final ProfileRepository profiles;
  private final WeightEntryRepository weights;
  private final ActivityEntryRepository activities;
  private final PrivacyService privacyService;
  private final UserConsentRepository consentRepo;

  public ExportController(JwtService jwt,
                          UserRepository users,
                          ProfileRepository profiles,
                          WeightEntryRepository weights,
                          ActivityEntryRepository activities,
                          PrivacyService privacyService,
                          UserConsentRepository consentRepo) {
    this.jwt = jwt; this.users = users; this.profiles = profiles; this.weights = weights; this.activities = activities; this.privacyService = privacyService; this.consentRepo = consentRepo;
  }

  private String emailFrom(String auth){
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing token");
    var t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token"); return jwt.getEmail(t); }
    catch (JwtException e){ throw new IllegalStateException("Invalid or expired token"); }
  }

  @GetMapping("/health")
  public ResponseEntity<?> exportHealth(@RequestHeader("Authorization") String auth) {
    String email = emailFrom(auth);
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));

    ProfileEntity profile = profiles.findByUser(user).orElse(null);
    List<WeightEntry> weightHistory = weights.findAllByUserOrderByAtAsc(user);
    List<ActivityEntry> activityHistory = activities.findAllByUserAndAtBetweenOrderByAtAsc(user, Instant.EPOCH, Instant.now());
    UserConsent consentCurrent = privacyService.getOrCreate(email); // single current consent

    Map<String,Object> profileMap = null;
    if (profile != null) {
      profileMap = new LinkedHashMap<>();
      profileMap.put("age", profile.getAge());
      profileMap.put("gender", profile.getGender());
      profileMap.put("heightCm", profile.getHeightCm());
      profileMap.put("weightKg", profile.getWeightKg());
      profileMap.put("targetWeightKg", profile.getTargetWeightKg());
      profileMap.put("activityLevel", profile.getActivityLevel());
      profileMap.put("goal", profile.getGoal());
      // updatedAt отсутствует в схеме — оставим null для совместимости с примером
      profileMap.put("updatedAt", null);
    }

    List<Map<String,Object>> weightsArr = new ArrayList<>();
    for (var w : weightHistory) {
      var m = new LinkedHashMap<String,Object>();
      m.put("valueKg", w.getWeightKg());
      m.put("recordedAt", w.getAt().atOffset(ZoneOffset.UTC).toString());
      weightsArr.add(m);
    }

    List<Map<String,Object>> activitiesArr = new ArrayList<>();
    for (var a : activityHistory) {
      var m = new LinkedHashMap<String,Object>();
      m.put("type", a.getType());
      m.put("minutes", a.getMinutes());
      m.put("intensity", a.getIntensity());
      m.put("at", a.getAt().atOffset(ZoneOffset.UTC).toString());
      activitiesArr.add(m);
    }

    // История согласий не ведётся — возвращаем текущий как массив из одного элемента
    List<Map<String,Object>> consentsArr = new ArrayList<>();
    if (consentCurrent != null) {
      var c = new LinkedHashMap<String,Object>();
      c.put("version", consentCurrent.getVersion());
      c.put("accepted", consentCurrent.isAccepted());
      c.put("allowAiUseProfile", consentCurrent.isAllowAiUseProfile());
      c.put("allowAiUseHistory", consentCurrent.isAllowAiUseHistory());
      c.put("allowAiUseHabits", consentCurrent.isAllowAiUseHabits());
      c.put("emailProduct", consentCurrent.isEmailProduct());
      c.put("emailSummaries", consentCurrent.isEmailSummaries());
      c.put("publicProfile", consentCurrent.isPublicProfile());
      c.put("publicStats", consentCurrent.isPublicStats());
      c.put("savedAt", consentCurrent.getAcceptedAt().atOffset(ZoneOffset.UTC).toString());
      consentsArr.add(c);
    }

    Map<String,Object> payload = new LinkedHashMap<>();
    payload.put("userId", user.getId());
    payload.put("exportedAt", Instant.now().toString());
    payload.put("profile", profileMap);
    payload.put("weights", weightsArr);
    payload.put("activities", activitiesArr);
    payload.put("consents", consentsArr);
    return ResponseEntity.ok(payload);
  }
}
