package com.ndl.numbers_dont_lie.profile.service;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.profile.dto.ProfileUpsertDto;
import com.ndl.numbers_dont_lie.weight.entity.WeightEntry;
import com.ndl.numbers_dont_lie.weight.repository.WeightEntryRepository;








import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ProfileService {
  private final ProfileRepository profiles;
  private final UserRepository users;
  private final WeightEntryRepository weights;

  public ProfileService(ProfileRepository profiles, UserRepository users, WeightEntryRepository weights) {
    this.profiles = profiles;
    this.users = users;
    this.weights = weights;
  }

  public ProfileEntity upsert(String email, ProfileUpsertDto dto) {
    UserEntity user = users.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("User not found"));

    if (dto.age != null && (dto.age < 5 || dto.age > 120))
      throw new IllegalArgumentException("age out of range");
    if (dto.heightCm != null && (dto.heightCm < 50 || dto.heightCm > 300))
      throw new IllegalArgumentException("heightCm out of range");
    if (dto.weightKg != null && (dto.weightKg < 20 || dto.weightKg > 500))
      throw new IllegalArgumentException("weightKg out of range");
    if (dto.targetWeightKg != null && (dto.targetWeightKg < 20 || dto.targetWeightKg > 500))
      throw new IllegalArgumentException("targetWeightKg out of range");

    ProfileEntity p = profiles.findByUser(user).orElseGet(() -> {
      ProfileEntity np = new ProfileEntity();
      np.setUser(user);
      return np;
    });

    if (dto.age != null)        p.setAge(dto.age);
    if (dto.gender != null)     p.setGender(dto.gender);
    if (dto.heightCm != null)   p.setHeightCm(dto.heightCm);
    if (dto.weightKg != null)   p.setWeightKg(dto.weightKg);
    if (dto.targetWeightKg != null) p.setTargetWeightKg(dto.targetWeightKg);
    if (dto.activityLevel != null) p.setActivityLevel(dto.activityLevel);
    if (dto.goal != null)       p.setGoal(dto.goal);

    ProfileEntity saved = profiles.save(p);

    // если в истории нет ни одной записи, а в профиле есть вес — создаём первую запись
    boolean hasNoHistory = weights.findAllByUserOrderByAtAsc(user).isEmpty();
    if (hasNoHistory && saved.getWeightKg() != null) {
      WeightEntry w = new WeightEntry();
      w.setUser(user);
      // ставим timestamp чуть раньше «сейчас», чтобы baseline был левее первой «реальной» точки
      w.setAt(Instant.now().minusSeconds(60));
      w.setWeightKg(saved.getWeightKg());
      try {
        weights.save(w);
      } catch (Exception ignore) {
        // если внезапно совпали timestamps — просто пропускаем
      }
    }

    return saved;
  }

  public ProfileEntity get(String email) {
    UserEntity user = users.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("User not found"));
    return profiles.findByUser(user).orElse(null);
  }
}
