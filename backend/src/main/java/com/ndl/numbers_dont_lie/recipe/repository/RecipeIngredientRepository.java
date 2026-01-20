package com.ndl.numbers_dont_lie.recipe.repository;

import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import com.ndl.numbers_dont_lie.recipe.entity.RecipeIngredient;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Long> {
    List<RecipeIngredient> findByRecipe(Recipe recipe);
}
