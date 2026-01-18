package com.ndl.numbers_dont_lie.service;

import com.ndl.numbers_dont_lie.dto.NutritionalPreferencesDto;
import com.ndl.numbers_dont_lie.entity.NutritionalPreferences;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.model.NutritionalPreferencesConstants;
import com.ndl.numbers_dont_lie.repository.NutritionalPreferencesRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NutritionalPreferencesService {
    private final NutritionalPreferencesRepository preferencesRepo;
    private final UserRepository userRepo;

    public NutritionalPreferencesService(NutritionalPreferencesRepository preferencesRepo, UserRepository userRepo) {
        this.preferencesRepo = preferencesRepo;
        this.userRepo = userRepo;
    }

    public NutritionalPreferencesDto get(String email) {
        UserEntity user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        NutritionalPreferences prefs = preferencesRepo.findByUserId(user.getId())
                .orElse(new NutritionalPreferences(user.getId()));

        return new NutritionalPreferencesDto(
                prefs.getDietaryPreferences(),
                prefs.getAllergies(),
                prefs.getUpdatedAt()
        );
    }

    public NutritionalPreferencesDto upsert(String email, NutritionalPreferencesDto dto) {
        UserEntity user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (dto.dietaryPreferences != null) {
            Set<String> invalidPrefs = dto.dietaryPreferences.stream()
                    .filter(p -> !NutritionalPreferencesConstants.isValidDietaryPreference(p))
                    .collect(Collectors.toSet());
            if (!invalidPrefs.isEmpty()) {
                throw new IllegalArgumentException("Invalid dietary preferences: " + invalidPrefs);
            }
        }

        if (dto.allergies != null) {
            Set<String> invalidAllergies = dto.allergies.stream()
                    .filter(a -> !NutritionalPreferencesConstants.isValidAllergy(a))
                    .collect(Collectors.toSet());
            if (!invalidAllergies.isEmpty()) {
                throw new IllegalArgumentException("Invalid allergies: " + invalidAllergies);
            }
        }

        NutritionalPreferences prefs = preferencesRepo.findByUserId(user.getId())
                .orElseGet(() -> new NutritionalPreferences(user.getId()));

        if (dto.dietaryPreferences != null) {
            prefs.setDietaryPreferences(new HashSet<>(dto.dietaryPreferences));
        }
        if (dto.allergies != null) {
            prefs.setAllergies(new HashSet<>(dto.allergies));
        }

        NutritionalPreferences saved = preferencesRepo.save(prefs);

        return new NutritionalPreferencesDto(
                saved.getDietaryPreferences(),
                saved.getAllergies(),
                saved.getUpdatedAt()
        );
    }
}
