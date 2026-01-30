package com.ndl.numbers_dont_lie.recipe.repository;

import com.ndl.numbers_dont_lie.recipe.entity.Ingredient;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {
    Optional<Ingredient> findByLabel(String label);
    Optional<Ingredient> findByStableId(String stableId);
    List<Ingredient> findByLabelContainingIgnoreCase(String keyword);
    Optional<Ingredient> findTopByOrderByIdDesc();
}
