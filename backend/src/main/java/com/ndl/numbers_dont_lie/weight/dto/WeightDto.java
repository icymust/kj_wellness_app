package com.ndl.numbers_dont_lie.weight.dto;

public class WeightDto {
  public Double weightKg;   // required
  public String at;         // ISO8601 optional; если пусто — возьмём now()
  // Optional arrays for dietary data
  public java.util.List<String> dietaryPreferences;
  public java.util.List<String> dietaryRestrictions;
}
