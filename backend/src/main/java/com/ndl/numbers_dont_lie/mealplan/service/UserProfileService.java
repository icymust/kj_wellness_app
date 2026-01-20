package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.mealplan.dto.MealFrequency;

/**
 * Interface for accessing User Profile data from Project 1.
 * 
 * Implementation responsibilities:
 * - Fetch user profile entity
 * - Extract meal frequency configuration
 * - Transform to MealFrequency DTO
 * - Handle missing/incomplete profiles
 * 
 * This interface will be implemented to integrate with existing
 * User Profile domain from Project 1.
 */
public interface UserProfileService {
    
    /**
     * Get meal frequency configuration from user's profile.
     * 
     * @param userId User ID
     * @return MealFrequency derived from User Profile
     * @throws IllegalStateException if user profile is not configured
     */
    MealFrequency getMealFrequency(Long userId);
    
    /**
     * Get timezone from user's profile.
     * 
     * @param userId User ID
     * @return IANA timezone string (e.g., "Europe/Tallinn")
     * @throws IllegalStateException if user profile is not configured
     */
    String getTimezone(Long userId);
}
