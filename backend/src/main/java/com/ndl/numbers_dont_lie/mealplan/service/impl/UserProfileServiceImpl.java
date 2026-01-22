package com.ndl.numbers_dont_lie.mealplan.service.impl;

import com.ndl.numbers_dont_lie.mealplan.dto.MealFrequency;
import com.ndl.numbers_dont_lie.mealplan.entity.MealType;
import com.ndl.numbers_dont_lie.mealplan.service.UserProfileService;
import com.ndl.numbers_dont_lie.profile.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.profile.repository.ProfileRepository;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of UserProfileService.
 * 
 * Fetches user profile data from Project 1 (User Profile domain)
 * to support meal planning in Project 2.
 */
@Service
public class UserProfileServiceImpl implements UserProfileService {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;

    public UserProfileServiceImpl(UserRepository userRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    public MealFrequency getMealFrequency(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found with ID: " + userId));
        
        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalStateException("User Profile not configured for user ID: " + userId));
        
        // Default to 3 meals per day (1 breakfast, 1 lunch, 1 dinner) if not specified
        String mealFrequencyStr = profile.getMealFrequency();
        
        Map<MealType, Integer> frequency = new HashMap<>();
        
        if (mealFrequencyStr == null || mealFrequencyStr.isBlank()) {
            // Default: 1 breakfast, 1 lunch, 1 dinner, 1 snack
            frequency.put(MealType.BREAKFAST, 1);
            frequency.put(MealType.LUNCH, 1);
            frequency.put(MealType.DINNER, 1);
            frequency.put(MealType.SNACK, 1);
        } else {
            // Parse from stored format (e.g., "breakfast:1,lunch:1,dinner:1,snack:1")
            try {
                String[] parts = mealFrequencyStr.split(",");
                for (String part : parts) {
                    String[] kv = part.trim().split(":");
                    if (kv.length == 2) {
                        MealType mealType = MealType.valueOf(kv[0].toUpperCase());
                        int count = Integer.parseInt(kv[1].trim());
                        if (count > 0) {
                            frequency.put(mealType, count);
                        }
                    }
                }
                // If parsing failed or resulted in empty map, use default
                if (frequency.isEmpty()) {
                    frequency.put(MealType.BREAKFAST, 1);
                    frequency.put(MealType.LUNCH, 1);
                    frequency.put(MealType.DINNER, 1);
                    frequency.put(MealType.SNACK, 1);
                }
            } catch (Exception e) {
                // If parsing fails, use default
                frequency.put(MealType.BREAKFAST, 1);
                frequency.put(MealType.LUNCH, 1);
                frequency.put(MealType.DINNER, 1);
                frequency.put(MealType.SNACK, 1);
            }
        }
        
        return new MealFrequency(frequency);
    }

    @Override
    public String getTimezone(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found with ID: " + userId));
        
        ProfileEntity profile = profileRepository.findByUser(user)
            .orElseThrow(() -> new IllegalStateException("User Profile not configured for user ID: " + userId));
        
        // Default to UTC if not specified
        String timezone = profile.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return "UTC";
        }
        
        return timezone;
    }
}
