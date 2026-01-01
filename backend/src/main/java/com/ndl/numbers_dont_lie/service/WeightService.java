package com.ndl.numbers_dont_lie.service;

import com.ndl.numbers_dont_lie.dto.WeightDto;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.WeightEntry;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.repository.WeightEntryRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class WeightService {
  private final UserRepository users;
  private final WeightEntryRepository weights;

  public WeightService(UserRepository users, WeightEntryRepository weights) {
    this.users = users;
    this.weights = weights;
  }

  public WeightEntry add(String email, WeightDto dto) {
    if (dto.weightKg == null || dto.weightKg < 20 || dto.weightKg > 500)
      throw new IllegalArgumentException("weightKg out of range");

    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    Instant at = (dto.at == null || dto.at.isBlank()) ? Instant.now() : Instant.parse(dto.at);

    // запрет дублей по (user, at)
    if (weights.findByUserAndAt(user, at).isPresent())
      throw new IllegalStateException("Duplicate weight entry for this timestamp");

    WeightEntry e = new WeightEntry();
    e.setUser(user);
    e.setAt(at);
    e.setWeightKg(dto.weightKg);
    // store dietary arrays as comma-separated strings (empty -> null)
    if (dto.dietaryPreferences != null && !dto.dietaryPreferences.isEmpty()) {
      e.setDietaryPreferences(String.join(",", dto.dietaryPreferences));
    } else {
      e.setDietaryPreferences(null);
    }
    if (dto.dietaryRestrictions != null && !dto.dietaryRestrictions.isEmpty()) {
      e.setDietaryRestrictions(String.join(",", dto.dietaryRestrictions));
    } else {
      e.setDietaryRestrictions(null);
    }
    return weights.save(e);
  }

  public List<WeightEntry> list(String email) {
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    return weights.findAllByUserOrderByAtAsc(user);
  }

  // Save per-user dietary preferences/restrictions (stored on UserEntity)
  public void saveDietary(String email, java.util.List<String> prefs, java.util.List<String> restrictions) {
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    user.setDietaryPreferencesJson(prefs != null && !prefs.isEmpty() ? String.join(",", prefs) : null);
    user.setDietaryRestrictionsJson(restrictions != null && !restrictions.isEmpty() ? String.join(",", restrictions) : null);
    users.save(user);
  }

  public java.util.Map<String, java.util.List<String>> loadDietary(String email) {
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    java.util.List<String> prefs = new java.util.ArrayList<>();
    java.util.List<String> restr = new java.util.ArrayList<>();
    if (user.getDietaryPreferencesJson() != null && !user.getDietaryPreferencesJson().isBlank()) {
      prefs = java.util.Arrays.asList(user.getDietaryPreferencesJson().split(","));
    }
    if (user.getDietaryRestrictionsJson() != null && !user.getDietaryRestrictionsJson().isBlank()) {
      restr = java.util.Arrays.asList(user.getDietaryRestrictionsJson().split(","));
    }
    return java.util.Map.of("dietaryPreferences", prefs, "dietaryRestrictions", restr);
  }
}
