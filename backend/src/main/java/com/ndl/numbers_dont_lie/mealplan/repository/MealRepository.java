package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {
}
