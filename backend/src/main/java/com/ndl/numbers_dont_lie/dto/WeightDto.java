package com.ndl.numbers_dont_lie.dto;

public class WeightDto {
  public Double weightKg;   // required
  public String at;         // ISO8601 optional; если пусто — возьмём now()
}
