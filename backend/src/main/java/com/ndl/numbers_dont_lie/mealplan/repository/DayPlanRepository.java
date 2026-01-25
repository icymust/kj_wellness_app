package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DayPlanRepository extends JpaRepository<DayPlan, Long> {
	Optional<DayPlan> findByUserIdAndDate(Long userId, LocalDate date);
}
