package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanVersionRepository extends JpaRepository<MealPlanVersion, Long> {
    
    List<MealPlanVersion> findByMealPlanIdOrderByVersionNumberDesc(Long mealPlanId);

    MealPlanVersion findByMealPlanIdAndVersionNumber(Long mealPlanId, Integer versionNumber);
}
