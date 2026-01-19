package com.ndl.numbers_dont_lie.service.nutrition;

import com.ndl.numbers_dont_lie.dto.nutrition.NutritionalPreferencesDto;
import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.model.nutrition.NutritionalPreferencesConstants;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
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

        NutritionalPreferencesDto dto = new NutritionalPreferencesDto();
        dto.dietaryPreferences = prefs.getDietaryPreferences();
        dto.allergies = prefs.getAllergies();
        dto.dislikedIngredients = prefs.getDislikedIngredients();
        dto.cuisinePreferences = prefs.getCuisinePreferences();
        dto.calorieTarget = prefs.getCalorieTarget();
        dto.proteinTarget = prefs.getProteinTarget();
        dto.carbsTarget = prefs.getCarbsTarget();
        dto.fatsTarget = prefs.getFatsTarget();
        dto.breakfastCount = prefs.getBreakfastCount();
        dto.lunchCount = prefs.getLunchCount();
        dto.dinnerCount = prefs.getDinnerCount();
        dto.snackCount = prefs.getSnackCount();
        dto.breakfastTime = prefs.getBreakfastTime();
        dto.lunchTime = prefs.getLunchTime();
        dto.dinnerTime = prefs.getDinnerTime();
        dto.snackTime = prefs.getSnackTime();
        dto.updatedAt = prefs.getUpdatedAt();
        
        return dto;
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

        // Validate meal counts
        validateMealCount(dto.breakfastCount, "breakfastCount");
        validateMealCount(dto.lunchCount, "lunchCount");
        validateMealCount(dto.dinnerCount, "dinnerCount");
        validateMealCount(dto.snackCount, "snackCount");

        // Validate time formats
        validateTimeFormat(dto.breakfastTime, "breakfastTime");
        validateTimeFormat(dto.lunchTime, "lunchTime");
        validateTimeFormat(dto.dinnerTime, "dinnerTime");
        validateTimeFormat(dto.snackTime, "snackTime");

        // Validate positive integers for targets
        validatePositiveInteger(dto.calorieTarget, "calorieTarget");
        validatePositiveInteger(dto.proteinTarget, "proteinTarget");
        validatePositiveInteger(dto.carbsTarget, "carbsTarget");
        validatePositiveInteger(dto.fatsTarget, "fatsTarget");

        NutritionalPreferences prefs = preferencesRepo.findByUserId(user.getId())
                .orElseGet(() -> new NutritionalPreferences(user.getId()));

        if (dto.dietaryPreferences != null) {
            prefs.setDietaryPreferences(new HashSet<>(dto.dietaryPreferences));
        }
        if (dto.allergies != null) {
            prefs.setAllergies(new HashSet<>(dto.allergies));
        }
        if (dto.dislikedIngredients != null) {
            prefs.setDislikedIngredients(dto.dislikedIngredients);
        }
        if (dto.cuisinePreferences != null) {
            prefs.setCuisinePreferences(dto.cuisinePreferences);
        }
        if (dto.calorieTarget != null) {
            prefs.setCalorieTarget(dto.calorieTarget);
        }
        if (dto.proteinTarget != null) {
            prefs.setProteinTarget(dto.proteinTarget);
        }
        if (dto.carbsTarget != null) {
            prefs.setCarbsTarget(dto.carbsTarget);
        }
        if (dto.fatsTarget != null) {
            prefs.setFatsTarget(dto.fatsTarget);
        }
        if (dto.breakfastCount != null) {
            prefs.setBreakfastCount(dto.breakfastCount);
        }
        if (dto.lunchCount != null) {
            prefs.setLunchCount(dto.lunchCount);
        }
        if (dto.dinnerCount != null) {
            prefs.setDinnerCount(dto.dinnerCount);
        }
        if (dto.snackCount != null) {
            prefs.setSnackCount(dto.snackCount);
        }
        // Only set times if count is not 0
        if (dto.breakfastTime != null && (dto.breakfastCount == null || dto.breakfastCount > 0)) {
            prefs.setBreakfastTime(dto.breakfastTime);
        }
        if (dto.lunchTime != null && (dto.lunchCount == null || dto.lunchCount > 0)) {
            prefs.setLunchTime(dto.lunchTime);
        }
        if (dto.dinnerTime != null && (dto.dinnerCount == null || dto.dinnerCount > 0)) {
            prefs.setDinnerTime(dto.dinnerTime);
        }
        if (dto.snackTime != null && (dto.snackCount == null || dto.snackCount > 0)) {
            prefs.setSnackTime(dto.snackTime);
        }

        NutritionalPreferences saved = preferencesRepo.save(prefs);

        NutritionalPreferencesDto result = new NutritionalPreferencesDto();
        result.dietaryPreferences = saved.getDietaryPreferences();
        result.allergies = saved.getAllergies();
        result.dislikedIngredients = saved.getDislikedIngredients();
        result.cuisinePreferences = saved.getCuisinePreferences();
        result.calorieTarget = saved.getCalorieTarget();
        result.proteinTarget = saved.getProteinTarget();
        result.carbsTarget = saved.getCarbsTarget();
        result.fatsTarget = saved.getFatsTarget();
        result.breakfastCount = saved.getBreakfastCount();
        result.lunchCount = saved.getLunchCount();
        result.dinnerCount = saved.getDinnerCount();
        result.snackCount = saved.getSnackCount();
        result.breakfastTime = saved.getBreakfastTime();
        result.lunchTime = saved.getLunchTime();
        result.dinnerTime = saved.getDinnerTime();
        result.snackTime = saved.getSnackTime();
        result.updatedAt = saved.getUpdatedAt();
        
        return result;
    }

    private void validateMealCount(Integer count, String fieldName) {
        if (count != null && (count < 0 || count > 5)) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 5");
        }
    }

    private void validateTimeFormat(String time, String fieldName) {
        if (time != null && !time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            throw new IllegalArgumentException(fieldName + " must be in HH:mm format");
        }
    }

    private void validatePositiveInteger(Integer value, String fieldName) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(fieldName + " must be a positive integer");
        }
    }
}
