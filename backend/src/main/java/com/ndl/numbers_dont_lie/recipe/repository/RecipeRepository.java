package com.ndl.numbers_dont_lie.recipe.repository;

import com.ndl.numbers_dont_lie.recipe.entity.MealType;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByCuisine(String cuisine);
    List<Recipe> findByMeal(MealType meal);
    List<Recipe> findByDietaryTagsContaining(String tag);
    List<Recipe> findByTimeMinutesLessThanEqual(Integer maxMinutes);
    List<Recipe> findByTitleContainingIgnoreCase(String keyword);
}
