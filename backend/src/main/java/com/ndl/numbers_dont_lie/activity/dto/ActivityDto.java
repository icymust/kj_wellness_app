package com.ndl.numbers_dont_lie.activity.dto;

public class ActivityDto {
  public String type;        // cardio|strength|flexibility|sports|other
  public Integer minutes;    // 5..300
  public String intensity;   // low|moderate|high (optional)
  public String at;          // ISO8601 (optional; если пусто — now)
}
