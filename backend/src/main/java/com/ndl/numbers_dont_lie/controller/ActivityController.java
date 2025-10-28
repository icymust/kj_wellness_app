package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.dto.ActivityDto;
import com.ndl.numbers_dont_lie.dto.ActivityWeekSummary;
import com.ndl.numbers_dont_lie.entity.ActivityEntry;
import com.ndl.numbers_dont_lie.service.ActivityService;
import com.ndl.numbers_dont_lie.service.JwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/progress/activity")
public class ActivityController {

  private final JwtService jwt;
  private final ActivityService service;

  public ActivityController(JwtService jwt, ActivityService service) {
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
                               @RequestBody ActivityDto dto) {
    try {
      ActivityEntry e = service.add(emailFrom(auth), dto);
      return ResponseEntity.status(201).body(Map.of("entry", e));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (IllegalStateException ex) { // дубликат или нет юзера
      return ResponseEntity.status(409).body(Map.of("error", ex.getMessage()));
    }
  }

  @GetMapping("/week")
  public ResponseEntity<?> week(@RequestHeader(value="Authorization", required=false) String auth,
                                @RequestParam(required = false) String date) {
    try {
      String email = emailFrom(auth);
      LocalDate d = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
      ActivityWeekSummary s = service.weekSummary(email, d);
      return ResponseEntity.ok(Map.of("summary", s));
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
    }
  }

  @GetMapping("/month")
  public ResponseEntity<?> month(@RequestHeader(value="Authorization", required=false) String auth,
                                 @RequestParam int year, @RequestParam int month) {
    try {
      String email = emailFrom(auth);
      var map = service.monthByDayMinutes(email, year, month);
      return ResponseEntity.ok(Map.of("byDayMinutes", map));
    } catch (IllegalStateException ex) {
      return ResponseEntity.status(401).body(Map.of("error", ex.getMessage()));
    }
  }
}
