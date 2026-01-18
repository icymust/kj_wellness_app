package com.ndl.numbers_dont_lie.privacy.controller;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.auth.service.JwtService;
import com.ndl.numbers_dont_lie.privacy.service.PrivacyService;
import com.ndl.numbers_dont_lie.privacy.dto.ConsentDto;
import com.ndl.numbers_dont_lie.activity.entity.ActivityEntry;
import com.ndl.numbers_dont_lie.activity.repository.ActivityEntryRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.*;

@RestController
@RequestMapping("/privacy")
public class PrivacyController {
  private final JwtService jwt;
  private final PrivacyService privacy;
  private final UserRepository users;
  private final ActivityEntryRepository activities;

  public PrivacyController(JwtService jwt, PrivacyService privacy,
                           UserRepository users, ActivityEntryRepository activities) {
    this.jwt = jwt; this.privacy = privacy; this.users = users; this.activities = activities;
  }

  private String emailFrom(String auth){
    if (auth == null || !auth.startsWith("Bearer ")) throw new IllegalStateException("Missing token");
    var t = auth.substring("Bearer ".length());
    try { if (!jwt.isAccessToken(t)) throw new IllegalStateException("Invalid token"); return jwt.getEmail(t); }
    catch (JwtException e){ throw new IllegalStateException("Invalid or expired token"); }
  }

  @GetMapping("/consent")
  public ResponseEntity<?> getConsent(@RequestHeader("Authorization") String auth){
    var email = emailFrom(auth);
    var c = privacy.getOrCreate(email);
    return ResponseEntity.ok(Map.of(
      "accepted", c.isAccepted(),
      "version", c.getVersion(),
      "allowAiUseProfile", c.isAllowAiUseProfile(),
      "allowAiUseHistory", c.isAllowAiUseHistory(),
      "allowAiUseHabits", c.isAllowAiUseHabits(),
      "publicProfile", c.isPublicProfile(),
      "publicStats", c.isPublicStats(),
      "emailProduct", c.isEmailProduct(),
      "emailSummaries", c.isEmailSummaries()
    ));
  }

  @PutMapping("/consent")
  public ResponseEntity<?> setConsent(@RequestHeader("Authorization") String auth,
                                      @RequestBody ConsentDto dto){
    var email = emailFrom(auth);
    var c = privacy.update(email, dto);
    return ResponseEntity.ok(Map.of("ok", true, "accepted", c.isAccepted(), "version", c.getVersion()));
  }

  @GetMapping("/export")
  public ResponseEntity<?> export(@RequestHeader("Authorization") String auth){
    var email = emailFrom(auth);
    UserEntity u = users.findByEmail(email).orElseThrow();

    Map<String,Object> profile = new LinkedHashMap<>();
    profile.put("id", u.getId());
    profile.put("createdAt", u.getCreatedAt());
    profile.put("healthProfile", null);

  List<ActivityEntry> entries = activities.findAllByUserAndAtBetweenOrderByAtAsc(
    u, java.time.Instant.EPOCH, java.time.Instant.now());

  List<Map<String,Object>> activity = new ArrayList<>();
  for (var a : entries) {
      var m = new LinkedHashMap<String,Object>();
      m.put("at", a.getAt().atOffset(ZoneOffset.UTC).toString());
      m.put("type", a.getType());
      m.put("minutes", a.getMinutes());
      m.put("intensity", a.getIntensity());
      activity.add(m);
    }

    Map<String,Object> payload = new LinkedHashMap<>();
    payload.put("exportedAt", java.time.Instant.now().toString());
    payload.put("profile", profile);
    payload.put("activityHistory", activity);
    return ResponseEntity.ok(payload);
  }
}
