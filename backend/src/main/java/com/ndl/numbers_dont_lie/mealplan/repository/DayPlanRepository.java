package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DayPlanRepository extends JpaRepository<DayPlan, Long> {
}
