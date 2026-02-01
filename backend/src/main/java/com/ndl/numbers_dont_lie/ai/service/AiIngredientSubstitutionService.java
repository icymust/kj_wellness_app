package com.ndl.numbers_dont_lie.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ndl.numbers_dont_lie.ai.GroqClient;
import com.ndl.numbers_dont_lie.ai.dto.AiIngredientSubstituteResponse;
import com.ndl.numbers_dont_lie.ai.exception.AiClientException;
import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.repository.RecipeRepository;
import com.ndl.numbers_dont_lie.repository.nutrition.NutritionalPreferencesRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AiIngredientSubstitutionService {
    private static final Logger logger = LoggerFactory.getLogger(AiIngredientSubstitutionService.class);
    private final GroqClient groqClient;
    private final NutritionalPreferencesRepository nutritionalPreferencesRepository;
    private final RecipeRepository recipeRepository;
    private final MealRepository mealRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiIngredientSubstitutionService(
            GroqClient groqClient,
            NutritionalPreferencesRepository nutritionalPreferencesRepository,
            RecipeRepository recipeRepository,
            MealRepository mealRepository) {
        this.groqClient = groqClient;
        this.nutritionalPreferencesRepository = nutritionalPreferencesRepository;
        this.recipeRepository = recipeRepository;
        this.mealRepository = mealRepository;
    }

    public AiIngredientSubstituteResponse suggestSubstitutes(
            Long recipeId,
            String ingredientName,
            String availableIngredients) {
        if (recipeId == null || ingredientName == null || ingredientName.isBlank()) {
            throw new IllegalArgumentException("recipeId and ingredientName are required");
        }

        Recipe recipe = recipeRepository.findById(recipeId)
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + recipeId));

        Long userId = resolveUserIdForRecipe(recipe.getStableId());
        logger.info(
            "[AI_SUBSTITUTE] Generating ingredient substitutes for userId={} recipeId={} ingredient={}",
            userId, recipeId, ingredientName
        );

        NutritionalPreferences prefs = userId != null
            ? nutritionalPreferencesRepository.findByUserId(userId).orElse(null)
            : null;

        List<String> dietaryPrefs = prefs != null ? new ArrayList<>(prefs.getDietaryPreferences()) : List.of();
        List<String> allergies = prefs != null ? new ArrayList<>(prefs.getAllergies()) : List.of();
        List<String> disliked = prefs != null ? new ArrayList<>(prefs.getDislikedIngredients()) : List.of();
        List<String> cuisines = prefs != null ? new ArrayList<>(prefs.getCuisinePreferences()) : List.of();

        String prompt = buildPrompt(
            recipe,
            ingredientName,
            dietaryPrefs,
            allergies,
            disliked,
            cuisines,
            availableIngredients
        );
        JsonNode response = groqClient.callForJson(prompt, 0.3);
        AiIngredientSubstituteResponse parsed = parseResponse(response);

        List<AiIngredientSubstituteResponse.Alternative> filtered = filterAlternatives(
            parsed.getAlternatives(), ingredientName, allergies, disliked, availableIngredients
        );

        return new AiIngredientSubstituteResponse(filtered);
    }

    private Long resolveUserIdForRecipe(String stableRecipeId) {
        List<Long> userIds = mealRepository.findUserIdsByRecipeIdOrderByIdDesc(
            stableRecipeId,
            PageRequest.of(0, 1)
        );
        return userIds.isEmpty() ? null : userIds.get(0);
    }

    private String buildPrompt(
            Recipe recipe,
            String ingredientName,
            List<String> dietaryPrefs,
            List<String> allergies,
            List<String> disliked,
            List<String> cuisines,
            String availableIngredients) {
        StringBuilder sb = new StringBuilder();
        sb.append("Suggest 1 to 3 ingredient substitutes as STRICT JSON only.\n");
        sb.append("Return JSON with schema:\n");
        sb.append("{ \"alternatives\": [ { \"name\": string, \"reason\": string } ] }\n\n");
        sb.append("Constraints:\n");
        sb.append("- original ingredient: ").append(ingredientName).append("\n");
        sb.append("- recipe name: ").append(recipe.getTitle()).append("\n");
        sb.append("- recipe cuisine: ").append(recipe.getCuisine()).append("\n");
        sb.append("- recipe meal type: ").append(recipe.getMeal()).append("\n");
        sb.append("- dietary preferences: ").append(dietaryPrefs).append("\n");
        sb.append("- allergies/intolerances (must avoid): ").append(allergies).append("\n");
        sb.append("- disliked ingredients (must avoid): ").append(disliked).append("\n");
        sb.append("- preferred cuisines: ").append(cuisines).append("\n");
        if (availableIngredients != null && !availableIngredients.isBlank()) {
            sb.append("- available ingredients to prefer: ").append(availableIngredients).append("\n");
            sb.append("Prefer available ingredients if possible; otherwise suggest the closest fit.\n");
        }
        sb.append("\n");
        sb.append("Do not repeat the original ingredient. Use short reasons (one sentence).\n");
        sb.append("No markdown, no extra text.\n");
        return sb.toString();
    }

    private AiIngredientSubstituteResponse parseResponse(JsonNode response) {
        try {
            AiIngredientSubstituteResponse parsed = objectMapper.convertValue(response, AiIngredientSubstituteResponse.class);
            if (parsed.getAlternatives() == null) {
                throw new IllegalArgumentException("AI response missing alternatives");
            }
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new AiClientException("Invalid AI substitute payload: " + e.getMessage(), e);
        }
    }

    private List<AiIngredientSubstituteResponse.Alternative> filterAlternatives(
            List<AiIngredientSubstituteResponse.Alternative> alternatives,
            String originalIngredient,
            List<String> allergies,
            List<String> disliked,
            String availableIngredients) {
        if (alternatives == null) {
            return List.of();
        }

        String originalNormalized = normalize(originalIngredient);
        Set<String> bannedTokens = new LinkedHashSet<>();
        allergies.forEach(a -> bannedTokens.add(normalize(a)));
        disliked.forEach(d -> bannedTokens.add(normalize(d)));
        List<String> preferred = parsePreferred(availableIngredients);

        List<AiIngredientSubstituteResponse.Alternative> filtered = new ArrayList<>();
        for (AiIngredientSubstituteResponse.Alternative alt : alternatives) {
            if (alt == null || alt.getName() == null || alt.getName().isBlank()) {
                continue;
            }
            String nameNormalized = normalize(alt.getName());
            if (nameNormalized.equals(originalNormalized)) {
                continue;
            }
            boolean banned = bannedTokens.stream().anyMatch(token -> !token.isEmpty() && nameNormalized.contains(token));
            if (banned) {
                continue;
            }
            String reason = alt.getReason() != null && !alt.getReason().isBlank()
                ? alt.getReason()
                : "Fits your preferences and works in this recipe.";
            filtered.add(new AiIngredientSubstituteResponse.Alternative(alt.getName(), reason));
        }

        if (preferred.isEmpty()) {
            return filtered.stream().limit(3).collect(Collectors.toList());
        }

        List<AiIngredientSubstituteResponse.Alternative> ordered = new ArrayList<>();
        for (AiIngredientSubstituteResponse.Alternative alt : filtered) {
            String name = normalize(alt.getName());
            boolean matches = preferred.stream().anyMatch(pref -> !pref.isEmpty() && name.contains(pref));
            if (matches) {
                ordered.add(alt);
            }
        }
        for (AiIngredientSubstituteResponse.Alternative alt : filtered) {
            if (!ordered.contains(alt)) {
                ordered.add(alt);
            }
        }

        return ordered.stream().limit(3).collect(Collectors.toList());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> parsePreferred(String availableIngredients) {
        if (availableIngredients == null || availableIngredients.isBlank()) {
            return List.of();
        }
        String[] tokens = availableIngredients.split("[,;]");
        List<String> values = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = normalize(token);
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }
}
