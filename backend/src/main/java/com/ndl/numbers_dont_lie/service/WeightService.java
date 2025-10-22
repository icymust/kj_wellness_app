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
    return weights.save(e);
  }

  public List<WeightEntry> list(String email) {
    UserEntity user = users.findByEmail(email).orElseThrow(() -> new IllegalStateException("User not found"));
    return weights.findAllByUserOrderByAtAsc(user);
  }
}
