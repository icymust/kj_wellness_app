package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    
    List<MealPlan> findByUserId(Long userId);
}
