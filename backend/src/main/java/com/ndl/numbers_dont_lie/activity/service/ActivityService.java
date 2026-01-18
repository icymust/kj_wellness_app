package com.ndl.numbers_dont_lie.activity.service;

import com.ndl.numbers_dont_lie.activity.dto.ActivityDto;
import com.ndl.numbers_dont_lie.activity.dto.ActivityWeekSummary;
import com.ndl.numbers_dont_lie.activity.entity.ActivityEntry;
import com.ndl.numbers_dont_lie.activity.repository.ActivityEntryRepository;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

@Service
public class ActivityService {
  private final UserRepository users;
  private final ActivityEntryRepository repo;

  public ActivityService(UserRepository users, ActivityEntryRepository repo) {
    this.users = users; this.repo = repo;
  }

  public ActivityEntry add(String email, ActivityDto dto) {
    if (dto.type == null || dto.type.isBlank()) throw new IllegalArgumentException("type required");
    if (dto.minutes == null || dto.minutes < 5 || dto.minutes > 300) throw new IllegalArgumentException("minutes out of range");
    if (dto.intensity != null && !Set.of("low","moderate","high").contains(dto.intensity)) dto.intensity = "moderate";

    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    Instant at = (dto.at == null || dto.at.isBlank()) ? Instant.now() : Instant.parse(dto.at);

    if (repo.findByUserAndAt(user, at).isPresent())
      throw new IllegalStateException("Duplicate activity entry for this timestamp");

    ActivityEntry e = new ActivityEntry();
    e.setUser(user); e.setAt(at);
    e.setType(dto.type); e.setMinutes(dto.minutes); e.setIntensity(dto.intensity);
    return repo.save(e);
  }

  public List<ActivityEntry> list(String email, LocalDate from, LocalDate to) {
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    Instant start = from.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    return repo.findAllByUserAndAtBetweenOrderByAtAsc(user, start, end);
  }

  public ActivityWeekSummary weekSummary(String email, LocalDate anyDateInsideWeek) {
    // ISO week: понедельник — воскресенье
    LocalDate monday = anyDateInsideWeek.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate sunday = monday.plusDays(6);
    List<ActivityEntry> entries = list(email, monday, sunday);

    int total = 0; int sessions = entries.size();
    Map<String,Integer> byType = new TreeMap<>();
    Map<Integer,Integer> byWeekday = new TreeMap<>();
    Set<LocalDate> days = new HashSet<>();

    for (ActivityEntry e : entries) {
      total += e.getMinutes();
      byType.merge(e.getType(), e.getMinutes(), Integer::sum);
      LocalDate d = LocalDateTime.ofInstant(e.getAt(), ZoneOffset.UTC).toLocalDate();
      days.add(d);
      int wd = d.getDayOfWeek().getValue(); // 1..7
      byWeekday.merge(wd, e.getMinutes(), Integer::sum);
    }

    ActivityWeekSummary s = new ActivityWeekSummary();
    s.weekStartIso = monday.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
    s.totalMinutes = total;
    s.sessions = sessions;
    s.daysActive = days.size();
    s.byTypeMinutes = byType;
    s.byWeekdayMinutes = byWeekday;
    return s;
  }

  public Map<Integer,Integer> monthByDayMinutes(String email, int year, int month) {
    java.time.LocalDate first = java.time.LocalDate.of(year, month, 1);
    java.time.LocalDate last  = first.with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
    List<ActivityEntry> entries = list(email, first, last);
    Map<Integer,Integer> byDay = new java.util.TreeMap<>();
    for (ActivityEntry e : entries) {
      int day = java.time.LocalDateTime.ofInstant(e.getAt(), java.time.ZoneOffset.UTC).getDayOfMonth();
      byDay.merge(day, e.getMinutes(), Integer::sum);
    }
    return byDay;
  }
}
