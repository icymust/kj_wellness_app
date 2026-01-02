package com.ndl.numbers_dont_lie.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.dto.AiInsightItemDto;
import com.ndl.numbers_dont_lie.dto.AiInsightsResponse;
import com.ndl.numbers_dont_lie.entity.AiInsightCache;
import com.ndl.numbers_dont_lie.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.WeightEntry;
import com.ndl.numbers_dont_lie.health.HealthCalc;
import com.ndl.numbers_dont_lie.repository.AiInsightCacheRepository;
import com.ndl.numbers_dont_lie.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.repository.WeightEntryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class AiInsightsService {
  private final UserRepository users;
  private final ProfileRepository profiles;
  private final WeightEntryRepository weights;
  private final ActivityService activityService;
  private final PrivacyService privacyService;
  private final AiInsightCacheRepository cacheRepo;
  private final ObjectMapper om;

  @Value("${app.ai.disabled:false}")
  private boolean aiDisabled;

  public AiInsightsService(UserRepository users, ProfileRepository profiles, WeightEntryRepository weights,
                           ActivityService activityService, PrivacyService privacyService,
                           AiInsightCacheRepository cacheRepo, ObjectMapper om) {
    this.users = users; this.profiles = profiles; this.weights = weights; this.activityService = activityService;
    this.privacyService = privacyService; this.cacheRepo = cacheRepo; this.om = om;
  }

  public AiInsightsResponse latest(String email, String scope, boolean forceRegenerate) {
    if (scope == null || scope.isBlank()) scope = "weekly";
    if (!scope.equals("weekly") && !scope.equals("monthly")) scope = "weekly";

    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    String goalKey = "default"; // future: derive from profile goal

    // EA: not sure about hardcoding default values, maybe make a settings option, there should be some good practice, but i suppose it can be hard coded

    if (!forceRegenerate) {
      Optional<AiInsightCache> latest = cacheRepo.findFirstByUserAndScopeAndGoalKeyOrderByGeneratedAtDesc(user, scope, goalKey);
      if (latest.isPresent()) {
        AiInsightCache c = latest.get();
        if (c.getExpiresAt() == null || c.getExpiresAt().isAfter(Instant.now())) {
          // return cached
          try {
            AiInsightsResponse resp = om.readValue(c.getDataJson(), AiInsightsResponse.class);
            resp.fromCache = true;
            return resp;
          } catch (Exception e) {
            // fall through: regenerate
          }
        }
      }
    }

    AiInsightsResponse fresh = generate(email, scope);
    // persist in cache
    try {
      AiInsightCache c = new AiInsightCache();
      c.setUser(user);
      c.setScope(scope);
      c.setGoalKey(goalKey);
      c.setGeneratedAt(Instant.now());
      int ttlHours = scope.equals("weekly") ? 6 : 24;
      c.setExpiresAt(Instant.now().plusSeconds(ttlHours * 3600L));
      // store JSON without fromCache=true (it will be computed when loading)
      AiInsightsResponse toStore = new AiInsightsResponse(fresh.items, fresh.summary, false, fresh.generatedAt);
      c.setDataJson(om.writeValueAsString(toStore));
      cacheRepo.save(c);
    } catch (JsonProcessingException ignored) {}
    return fresh;
  }

  private AiInsightsResponse generate(String email, String scope) {
    var consent = privacyService.getOrCreate(email);
    boolean canUseProfile = consent.isAccepted() && consent.isAllowAiUseProfile();
    boolean canUseHistory = consent.isAccepted() && consent.isAllowAiUseHistory();
    boolean canUseHabits  = consent.isAccepted() && consent.isAllowAiUseHabits();

    List<AiInsightItemDto> items = new ArrayList<>();
    Map<String,Integer> summary = new HashMap<>();

    if (aiDisabled || !(canUseProfile || canUseHistory || canUseHabits)) {
      // Safe defaults: generic tips
      items.add(new AiInsightItemDto(
        "Weekly activity",
        "Aim for at least 150 minutes of moderate activity per week and move at least 3 days out of 7.",
        "medium",
        List.of("generic","activity")
      ));
      items.add(new AiInsightItemDto(
        "Nutrition balance",
        "Balance your diet: more vegetables, fiber and water; avoid excess sugar and ultra-processed foods.",
        "low",
        List.of("generic","nutrition")
      ));
      return finalizeResponse(items, summary, true);
    }

    ProfileEntity p = profiles.findByUser(users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"))).orElse(null);
    List<WeightEntry> list = Collections.emptyList();
    if (canUseHistory) {
      list = weights.findAllByUserOrderByAtAsc(users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found")));
    }

    Double latestWeight = (list.isEmpty() ? (p != null ? p.getWeightKg() : null) : list.get(list.size()-1).getWeightKg());
    Integer heightCm    = p != null ? p.getHeightCm() : null;
    String activityLevel = p != null ? p.getActivityLevel() : null; // not used, can be removed?

    // BMI-based insight
    if (canUseProfile && latestWeight != null && heightCm != null) {
      var bmi = HealthCalc.bmi(latestWeight, heightCm);
      switch (bmi.classification) {
        case "underweight" -> items.add(new AiInsightItemDto(
          "Underweight",
          "Your BMI is below normal. Discuss with a doctor how to gain weight safely and adjust nutrition.",
          "medium",
          List.of("bmi","underweight")));
        case "overweight" -> items.add(new AiInsightItemDto(
          "Overweight",
          "Aim for a modest calorie deficit and more activity. 0.25–0.5 kg per week is a realistic pace.",
          "high",
          List.of("bmi","overweight")));
        case "obese" -> items.add(new AiInsightItemDto(
          "High BMI",
          "We recommend consulting a specialist and gradual changes: walking, strength work, and calorie control.",
          "high",
          List.of("bmi","obese")));
        default -> items.add(new AiInsightItemDto(
          "Normal BMI",
          "Maintain your current weight through regular activity and balanced nutrition.",
          "low",
          List.of("bmi","normal")));
      }
    }

    // Weekly or Monthly activity summary insights
    if (canUseHabits) {
      if (scope.equals("weekly")) {
        var week = activityService.weekSummary(email, LocalDate.now());
        if (week.totalMinutes < 150) {
          items.add(new AiInsightItemDto(
            "Increase activity",
            "Add 1–2 short walks or a 20–30 minute session to reach 150+ minutes/week.",
            week.totalMinutes < 90 ? "high" : "medium",
            List.of("activity","weekly")));
        } else {
          items.add(new AiInsightItemDto(
            "Great week",
            "You are meeting WHO activity recommendations. Keep it up!",
            "low",
            List.of("activity","weekly")));
        }
        if (week.daysActive < 3) {
          items.add(new AiInsightItemDto(
            "Active days",
            "Try to move at least 3 days out of 7 — small regular sessions have a big impact.",
            "medium",
            List.of("activity","frequency")));
        }
      } else { // monthly
        var now = LocalDate.now(ZoneOffset.UTC);
        var byDay = activityService.monthByDayMinutes(email, now.getYear(), now.getMonthValue());
        int total = byDay.values().stream().mapToInt(Integer::intValue).sum();
        long activeDays = byDay.values().stream().filter(v -> v > 0).count();
        if (total < 600) { // < 10 hours per month
          items.add(new AiInsightItemDto(
            "Low total activity",
            "Consider planning 3×30 min weekly — that’s ~6 hours per month and a noticeable health benefit.",
            "medium",
            List.of("activity","monthly")));
        }
        if (activeDays < 12) {
          items.add(new AiInsightItemDto(
            "Irregularity",
            "Increase the number of active days: even 10–15 minutes on weekdays help build a sustainable habit.",
            "low",
            List.of("activity","consistency")));
        }
      }
    }

    // Progress towards target weight
    if (canUseProfile && latestWeight != null && p != null) {
      Double target = p.getTargetWeightKg() != null ? p.getTargetWeightKg() : p.getWeightKg();
      if (target != null) {
        double diff = Math.abs(latestWeight - target);
        if (diff <= 1.0) {
          items.add(new AiInsightItemDto(
            "Close to your goal",
            "You’re almost there — keep your routine and gradually transition to maintenance.",
            "low",
            List.of("goal","progress")));
        } else if (diff <= 3.0) {
          items.add(new AiInsightItemDto(
            "Good progress",
            "You are 1–3 kg from your goal. Focus on sleep and regular steps to cement success.",
            "medium",
            List.of("goal","progress")));
        } else {
          items.add(new AiInsightItemDto(
            "Stay the course",
            "Big goals take time. Break them into 2–3 kg stages and celebrate small wins.",
            "low",
            List.of("goal","progress")));
        }
      }
    }

    return finalizeResponse(items, summary, false);
  }

  private AiInsightsResponse finalizeResponse(List<AiInsightItemDto> items, Map<String,Integer> summary, boolean genericOnly) {
    // Build summary by priority
    for (AiInsightItemDto it : items) {
      summary.merge(it.priority != null ? it.priority : "low", 1, Integer::sum);
    }
    String ts = Instant.now().toString();
    return new AiInsightsResponse(items, summary, false, ts);
  }
}
