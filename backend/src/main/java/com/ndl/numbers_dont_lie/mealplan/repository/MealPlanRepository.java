package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    
    List<MealPlan> findByUserId(Long userId);

    Optional<MealPlan> findFirstByUserId(Long userId);

    Optional<MealPlan> findTopByUserIdAndDurationOrderByIdDesc(Long userId, PlanDuration duration);
}
