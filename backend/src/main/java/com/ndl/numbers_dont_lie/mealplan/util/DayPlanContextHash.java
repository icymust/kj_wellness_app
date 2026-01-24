package com.ndl.numbers_dont_lie.mealplan.util;

import com.ndl.numbers_dont_lie.ai.dto.AiMealStructureResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Generates a deterministic hash of the day plan context.
 * 
 * Two contexts with identical values produce the same hash.
 * Any change in preferences, targets, or meal structure changes the hash.
 * 
 * This enables detecting when user preferences have changed and meal plans need regeneration.
 */
public class DayPlanContextHash {
    
    /**
     * Generate context hash from user preferences and meal structure.
     * 
     * @param userId User ID
     * @param dietaryPreferences List of dietary preferences (e.g., "vegetarian")
     * @param allergies List of allergen restrictions
     * @param dislikedIngredients List of disliked ingredients
     * @param cuisinePreferences List of preferred cuisines
     * @param calorieTarget Daily calorie target (or null)
     * @param mealSlots Meal structure defining meal count and times
     * @return Deterministic hex hash of the context
     */
    public static String generate(
            Long userId,
            List<String> dietaryPreferences,
            List<String> allergies,
            List<String> dislikedIngredients,
            List<String> cuisinePreferences,
            Integer calorieTarget,
            List<AiMealStructureResult.MealSlot> mealSlots) {
        
        // Sort all lists to ensure deterministic order
        List<String> sortedDietary = safeSort(dietaryPreferences);
        List<String> sortedAllergies = safeSort(allergies);
        List<String> sortedDisliked = safeSort(dislikedIngredients);
        List<String> sortedCuisines = safeSort(cuisinePreferences);
        
        // Build context string
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("userId=").append(userId).append("|");
        contextBuilder.append("dietary=").append(sortedDietary).append("|");
        contextBuilder.append("allergies=").append(sortedAllergies).append("|");
        contextBuilder.append("disliked=").append(sortedDisliked).append("|");
        contextBuilder.append("cuisines=").append(sortedCuisines).append("|");
        contextBuilder.append("calories=").append(calorieTarget != null ? calorieTarget : "none").append("|");
        contextBuilder.append("meals=").append(mealSlotSignature(mealSlots));
        
        String context = contextBuilder.toString();
        return computeSha256Hash(context);
    }
    
    /**
     * Generate a signature of meal slots (count and types, not times which may vary).
     */
    private static String mealSlotSignature(List<AiMealStructureResult.MealSlot> mealSlots) {
        if (mealSlots == null || mealSlots.isEmpty()) {
            return "empty";
        }
        
        List<String> signature = new ArrayList<>();
        for (AiMealStructureResult.MealSlot slot : mealSlots) {
            signature.add(slot.getMealType());
        }
        Collections.sort(signature);
        return signature.toString();
    }
    
    /**
     * Safely sort a list of strings (handle nulls).
     */
    private static List<String> safeSort(List<String> list) {
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> sorted = new ArrayList<>(list);
        Collections.sort(sorted);
        return sorted;
    }
    
    /**
     * Compute SHA-256 hash of a string.
     */
    private static String computeSha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // Fallback: just use length-based hash if SHA-256 not available
            return Integer.toHexString(input.hashCode());
        }
    }
    
    /**
     * Convert byte array to hex string.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
