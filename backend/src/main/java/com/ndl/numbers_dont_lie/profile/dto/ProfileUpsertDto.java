package com.ndl.numbers_dont_lie.profile.dto;

public class ProfileUpsertDto {
  public Integer age;
  public String gender;
  public Integer heightCm;   // уже в см
  public Double  weightKg;   // уже в кг
  public Double  targetWeightKg; // цель по весу (кг)
  public String activityLevel;
  public String goal;
}
