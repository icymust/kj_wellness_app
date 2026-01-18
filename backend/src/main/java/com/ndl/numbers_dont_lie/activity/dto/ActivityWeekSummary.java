package com.ndl.numbers_dont_lie.activity.dto;

import java.util.Map;

public class ActivityWeekSummary {
  public String weekStartIso;     // понедельник (UTC) в ISO
  public int totalMinutes;        // суммарно за неделю
  public int sessions;            // число записей
  public int daysActive;          // количество дней с активностью
  public Map<String,Integer> byTypeMinutes;   // cardio: 120, strength: 60...
  public Map<Integer,Integer> byWeekdayMinutes; // 1..7 (Mon..Sun) -> минуты
}
