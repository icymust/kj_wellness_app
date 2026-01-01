package com.ndl.numbers_dont_lie.dto;

import java.util.List;

public class DietaryDto {
  public List<String> dietaryPreferences;
  public List<String> dietaryRestrictions;

  public DietaryDto() {}
  public DietaryDto(List<String> prefs, List<String> restrictions) { this.dietaryPreferences = prefs; this.dietaryRestrictions = restrictions; }
}
