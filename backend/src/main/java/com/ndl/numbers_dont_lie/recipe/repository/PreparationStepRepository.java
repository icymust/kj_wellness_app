package com.ndl.numbers_dont_lie.recipe.repository;

import com.ndl.numbers_dont_lie.recipe.entity.PreparationStep;
import com.ndl.numbers_dont_lie.recipe.entity.Recipe;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreparationStepRepository extends JpaRepository<PreparationStep, Long> {
    List<PreparationStep> findByRecipeOrderByOrderNumberAsc(Recipe recipe);
}
