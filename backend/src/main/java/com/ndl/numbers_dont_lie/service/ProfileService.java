package com.ndl.numbers_dont_lie.service;

import com.ndl.numbers_dont_lie.dto.ProfileUpsertDto;
import com.ndl.numbers_dont_lie.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {
  private final ProfileRepository profiles;
  private final UserRepository users;

  public ProfileService(ProfileRepository profiles, UserRepository users) {
    this.profiles = profiles;
    this.users = users;
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

    ProfileEntity p = profiles.findByUser(user).orElseGet(() -> {
      ProfileEntity np = new ProfileEntity();
      np.setUser(user);
      return np;
    });

    if (dto.age != null)        p.setAge(dto.age);
    if (dto.gender != null)     p.setGender(dto.gender);
    if (dto.heightCm != null)   p.setHeightCm(dto.heightCm);
    if (dto.weightKg != null)   p.setWeightKg(dto.weightKg);
    if (dto.activityLevel != null) p.setActivityLevel(dto.activityLevel);
    if (dto.goal != null)       p.setGoal(dto.goal);

    return profiles.save(p);
  }

  public ProfileEntity get(String email) {
    UserEntity user = users.findByEmail(email)
        .orElseThrow(() -> new IllegalStateException("User not found"));
    return profiles.findByUser(user).orElse(null);
  }
}
