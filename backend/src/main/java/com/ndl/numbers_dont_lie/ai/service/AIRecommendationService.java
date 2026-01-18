package com.ndl.numbers_dont_lie.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.weight.entity.WeightEntry;
import com.ndl.numbers_dont_lie.weight.repository.WeightEntryRepository;
import com.ndl.numbers_dont_lie.activity.service.ActivityService;
import com.ndl.numbers_dont_lie.ai.repository.AiInsightCacheRepository;
import com.ndl.numbers_dont_lie.ai.dto.AiInsightsResponse;
import com.ndl.numbers_dont_lie.ai.dto.AiInsightItemDto;
import com.ndl.numbers_dont_lie.ai.entity.AiInsightCache;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import org.springframework.stereotype.Service;

/**
 * Deterministic, explainable and cacheable AI recommendations engine (no external AI).
 * This service separates data gathering (PII-aware) from the core rule engine (PII-free).
 */
@Service
public class AIRecommendationService {
  private final UserRepository users;
  private final ProfileRepository profiles;
  private final WeightEntryRepository weights;
  private final ActivityService activityService;
  private final AiInsightCacheRepository cacheRepo;
  private final ObjectMapper om;
  private final AiAvailabilityService availability;

  public AIRecommendationService(UserRepository users,
                                 ProfileRepository profiles,
                                 WeightEntryRepository weights,
                                 ActivityService activityService,
                                 AiInsightCacheRepository cacheRepo,
                                 ObjectMapper om,
                                 AiAvailabilityService availability) {
    this.users = users; this.profiles = profiles; this.weights = weights; this.activityService = activityService;
    this.cacheRepo = cacheRepo; this.om = om; this.availability = availability;
  }

  /**
   * Load the latest cached recommendation for scope and current goal.
   * Does NOT recompute.
   */
  public Optional<AiInsightsResponse> loadLatest(String email, String scope) {
    String normalizedScope = normalizeScope(scope);
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    ProfileEntity p = profiles.findByUser(user).orElse(null);
    String goalKey = (p != null && p.getGoal() != null) ? p.getGoal() : "general_fitness";

    Optional<AiInsightCache> latest = cacheRepo.findFirstByUserAndScopeAndGoalKeyOrderByGeneratedAtDesc(user, normalizedScope, goalKey);
    if (latest.isEmpty()) return Optional.empty();
    try {
      AiInsightsResponse resp = om.readValue(latest.get().getDataJson(), AiInsightsResponse.class);
      resp.fromCache = true;
      if (resp.items != null) {
        for (var it : resp.items) { it.cached = true; }
      }
      return Optional.of(resp);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  /**
   * Regenerate recommendation deterministically based on current metrics and save to cache.
   * If AI is disabled (availability issue), returns cached recommendation only.
   */
  public AiInsightsResponse regenerate(String email, String scope) {
    String normalizedScope = normalizeScope(scope);
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    ProfileEntity p = profiles.findByUser(user).orElse(null);
    String goalKey = (p != null && p.getGoal() != null) ? p.getGoal() : "general_fitness";

    // AI availability handling
    if (!availability.isEnabled()) {
      Optional<AiInsightsResponse> cached = loadLatest(email, normalizedScope);
      if (cached.isPresent()) return cached.get();
      throw new IllegalStateException("AI unavailable and no cached recommendation");
    }

    // Build non-PII metrics for the core rule engine
    Metrics m = buildMetrics(email, p, normalizedScope);
    AiInsightsResponse fresh = computeDeterministic(m);

    // Persist in cache (TTL based on scope)
    try {
      AiInsightCache c = new AiInsightCache();
      c.setUser(user);
      c.setScope(normalizedScope);
      c.setGoalKey(goalKey);
      c.setGeneratedAt(Instant.now());
      int ttlHours = normalizedScope.equals("weekly") ? 6 : 24;
      c.setExpiresAt(Instant.now().plusSeconds(ttlHours * 3600L));
      // Store JSON; fromCache flag set to false (will be computed on load)
      AiInsightsResponse toStore = new AiInsightsResponse(fresh.items, fresh.summary, false, fresh.generatedAt);
      c.setDataJson(om.writeValueAsString(toStore));
      cacheRepo.save(c);
    } catch (JsonProcessingException ignored) {}

    return fresh;
  }

  private String normalizeScope(String scope) {
    if (scope == null || scope.isBlank()) return "weekly";
    return (scope.equals("weekly") || scope.equals("monthly")) ? scope : "weekly";
  }

  /**
   * PII-free metrics used by the rule engine.
   */
  static class Metrics {
    String goal;                  // weight_loss | muscle_gain | general_fitness
    String weeklyActivityLevel;   // low | medium | high
    String progressStatus;        // improving | stalled
    String scope;                 // weekly | monthly
    int activeDays;               // number of active days for the current scope
    int totalMinutes;             // total minutes of activity for the current scope
    int wellnessDeltaPoints;      // deterministic points change vs previous period
  }

  private Metrics buildMetrics(String email, ProfileEntity p, String scope) {
    Metrics m = new Metrics();
    m.scope = scope;
    m.goal = (p != null && p.getGoal() != null) ? p.getGoal() : "general_fitness";

    // Determine activity metrics and level depending on scope; compute wellness delta vs previous period
    if ("monthly".equals(scope)) {
      var now = LocalDate.now(ZoneOffset.UTC);
      var byDay = activityService.monthByDayMinutes(email, now.getYear(), now.getMonthValue());
      int total = byDay.values().stream().mapToInt(Integer::intValue).sum();
      long activeDays = byDay.values().stream().filter(v -> v > 0).count();
      m.totalMinutes = total;
      m.activeDays = (int) activeDays;
      // Previous month
      var prev = now.minusMonths(1);
      var prevByDay = activityService.monthByDayMinutes(email, prev.getYear(), prev.getMonthValue());
      int prevTotal = prevByDay.values().stream().mapToInt(Integer::intValue).sum();
      m.wellnessDeltaPoints = clamp(Math.round((float)(total - prevTotal) / 30f), -10, 10);
      // Map monthly total to level
      if (total < 600) m.weeklyActivityLevel = "low";
      else if (total < 900) m.weeklyActivityLevel = "medium";
      else m.weeklyActivityLevel = "high";
    } else {
      var thisWeek = activityService.weekSummary(email, LocalDate.now());
      m.totalMinutes = thisWeek.totalMinutes;
      m.activeDays = thisWeek.daysActive;
      var prevWeek = activityService.weekSummary(email, LocalDate.now().minusWeeks(1));
      m.wellnessDeltaPoints = clamp(Math.round((float)(thisWeek.totalMinutes - prevWeek.totalMinutes) / 30f), -10, 10);
      int minutes = thisWeek.totalMinutes;
      if (minutes < 90) m.weeklyActivityLevel = "low";
      else if (minutes < 150) m.weeklyActivityLevel = "medium";
      else m.weeklyActivityLevel = "high";
    }

    // Progress status based on recent weight trend (last 28 days)
    List<WeightEntry> list = weights.findAllByUserOrderByAtAsc(users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found")));
    m.progressStatus = computeProgressStatus(list, p);
    return m;
  }

  private String computeProgressStatus(List<WeightEntry> entries, ProfileEntity p) {
    if (entries == null || entries.size() < 2) return "stalled";
    // Compare average of last 3 vs previous 3 entries within recent period if available
    int n = entries.size();
    double lastAvg = avg(entries.subList(Math.max(n - 3, 0), n));
    double prevAvg = avg(entries.subList(Math.max(n - 6, 0), Math.max(n - 3, 0)));
    double delta = lastAvg - prevAvg; // positive => gaining

    String goal = (p != null && p.getGoal() != null) ? p.getGoal() : "general_fitness";
    double threshold = 0.3; // kg over ~few weeks

    if ("weight_loss".equals(goal)) {
      return (delta <= -threshold) ? "improving" : "stalled";
    } else if ("muscle_gain".equals(goal)) {
      return (delta >= threshold) ? "improving" : "stalled";
    } else {
      // general fitness: use activity consistency as proxy via entries cadence; if frequent logging: improving
      return Math.abs(delta) >= threshold ? "improving" : "stalled";
    }
  }

  private double avg(List<WeightEntry> sub) {
    if (sub == null || sub.isEmpty()) return Double.NaN;
    double s = 0.0; int c = 0;
    for (WeightEntry e : sub) { if (e != null && e.getWeightKg() != null) { s += e.getWeightKg(); c++; } }
    return c == 0 ? Double.NaN : s / c;
  }

  /**
   * Core deterministic rule engine producing explainable recommendations.
   */
  private AiInsightsResponse computeDeterministic(Metrics m) {
    List<AiInsightItemDto> items = new ArrayList<>();
    Map<String,Integer> summary = new HashMap<>();
    String nowIso = Instant.now().toString();

    // SUMMARY item: weekly/monthly overview with required fields
    String scopeLabel = "weekly".equals(m.scope) ? "Weekly" : "Monthly";
    String onTrack = "improving".equals(m.progressStatus) ? "on track" : "stalled";
    String summaryMsg = String.format(
      "%s summary: activity level %s, %d active %s, wellness score change %+d points, progress toward goal: %s.",
      scopeLabel,
      m.weeklyActivityLevel.toUpperCase(),
      m.activeDays,
      "weekly".equals(m.scope) ? "days this week" : "days this month",
      m.wellnessDeltaPoints,
      onTrack
    );
    String summaryDetail = String.format(
      "%s overview\n- Activity level: %s\n- Total minutes: %d\n- Active days: %d (%s)\n- Wellness score change: %+d points vs previous %s\n- Progress toward goal: %s\n- Goal in focus: %s",
      scopeLabel,
      m.weeklyActivityLevel.toUpperCase(),
      m.totalMinutes,
      m.activeDays,
      "weekly".equals(m.scope) ? "this week" : "this month",
      m.wellnessDeltaPoints,
      "weekly".equals(m.scope) ? "week" : "month",
      onTrack,
      m.goal
    );
    String summaryPriority = switch (m.weeklyActivityLevel) {
      case "low" -> "HIGH";
      case "medium" -> "MEDIUM";
      default -> "LOW";
    };
    items.add(item("SUMMARY", summaryPriority, summaryMsg, summaryDetail, nowIso, false));

    // GOAL-based recommendation
    String goalMsg;
    String goalPriority = "HIGH";
    String goalDetail;
    if ("weight_loss".equals(m.goal)) {
      goalMsg = "Since your goal is weight loss, focus on a small, sustainable calorie deficit and regular activity.";
      goalDetail = "Explanation: combine moderate portion sizes, more vegetables and water, and consistent movement. Avoid extreme restrictions; consistency beats speed.";
    } else if ("muscle_gain".equals(m.goal)) {
      goalMsg = "For muscle gain, prioritize progressive strength training, adequate protein, and recovery.";
      goalDetail = "Explanation: aim for progressive overload in key lifts, include protein with each meal, and ensure rest days to support adaptation.";
    } else {
      goalMsg = "Since your goal is general fitness, keep a varied weekly activity routine and a balanced diet.";
      goalDetail = "Explanation: mix cardio and strength across the week, maintain flexible balanced meals, and focus on regularity and recovery.";
      goalPriority = "MEDIUM";
    }
    items.add(item("GOAL", goalPriority, goalMsg, goalDetail, nowIso, false));

    // ACTIVITY-based recommendation
    String actMsg; String actPriority;
    String actDetail;
    switch (m.weeklyActivityLevel) {
      case "low" -> {
        actMsg = "Your weekly activity is low. Add 2–3 short 20–30 minute sessions to gradually reach ≥150 minutes per week.";
        actPriority = "HIGH";
        actDetail = "Plan: schedule two 20–30 minute walks or light sessions on weekdays and one longer session on the weekend. Keep it realistic and repeatable.";
      }
      case "medium" -> {
        actMsg = "You're on the right track. Add one more short workout or walk to consistently meet weekly activity recommendations.";
        actPriority = "MEDIUM";
        actDetail = "Plan: pick one extra 20–30 minute slot mid-week. Track steps or minutes to confirm you meet the target regularly.";
      }
      default -> {
        actMsg = "Great job! You're meeting activity recommendations. Keep your routine steady and include adequate recovery.";
        actPriority = "LOW";
        actDetail = "Tip: maintain current frequency, include mobility work, and keep one lighter day for recovery to stay consistent.";
      }
    }
    // Explicitly reference goal in activity recommendation
    actMsg = prefixGoal(m.goal) + actMsg;
    actDetail = prefixGoal(m.goal) + actDetail;
    items.add(item("ACTIVITY", actPriority, actMsg, actDetail, nowIso, false));

    // PROGRESS-based recommendation
    String progMsg; String progPriority;
    String progDetail;
    if ("improving".equals(m.progressStatus)) {
      progMsg = "Progress is positive. Maintain a steady pace and habits — small weekly steps lead to reliable results.";
      progPriority = "LOW";
      progDetail = "Explanation: keep your current routine, prioritize sleep (7–8h), and review one small habit to reinforce (hydration, steps, or protein).";
    } else {
      progMsg = "Progress has temporarily stalled. Try micro-adjustments: 7–8 hours of sleep, one extra walk, and a small intake/load tweak aligned with your goal.";
      progPriority = "MEDIUM";
      if ("weight_loss".equals(m.goal)) {
        progDetail = "For weight loss: add a 20–30 minute walk on 1–2 days and reduce energy intake slightly (e.g., smaller portion or fewer snacks).";
      } else if ("muscle_gain".equals(m.goal)) {
        progDetail = "For muscle gain: add a small volume increase in a key lift (e.g., +1 set) and ensure protein throughout the day; avoid over-fatigue.";
      } else {
        progDetail = "For general fitness: add one short cardio session and one brief strength circuit; keep intensity moderate and consistent.";
      }
    }
    progMsg = prefixGoal(m.goal) + progMsg;
    progDetail = prefixGoal(m.goal) + progDetail;
    items.add(item("PROGRESS", progPriority, progMsg, progDetail, nowIso, false));

    // Build summary by uppercase priority
    for (AiInsightItemDto it : items) {
      summary.merge(it.priorityLevel != null ? it.priorityLevel : "LOW", 1, Integer::sum);
    }

    return new AiInsightsResponse(items, summary, false, nowIso);
  }

  private String prefixGoal(String goal) {
  if ("weight_loss".equals(goal)) return "Since your goal is weight loss: ";
  if ("muscle_gain".equals(goal)) return "For muscle gain: ";
  return "For general fitness: ";
  }

  private AiInsightItemDto item(String type, String priorityLevel, String message, String detail, String createdAt, boolean cached) {
    AiInsightItemDto dto = new AiInsightItemDto();
    dto.type = type;                      // GOAL | ACTIVITY | PROGRESS
    dto.priorityLevel = priorityLevel;    // HIGH | MEDIUM | LOW (for tests)
    dto.priority = priorityLevel.toLowerCase(); // keep legacy field for UI coloring
    dto.message = message;
    dto.title = type.charAt(0) + type.substring(1).toLowerCase(); // Legacy title for UI
    dto.detail = detail;                  // Full explanation for expanded view
    dto.createdAt = createdAt;
    dto.cached = cached;
    dto.tags = List.of(type.toLowerCase());
    return dto;
  }

  private int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
