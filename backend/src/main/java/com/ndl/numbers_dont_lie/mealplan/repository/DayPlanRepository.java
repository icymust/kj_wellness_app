package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DayPlanRepository extends JpaRepository<DayPlan, Long> {
	Optional<DayPlan> findByUserIdAndDate(Long userId, LocalDate date);
	
	/**
	 * Find DayPlan with meals eagerly loaded (JOIN FETCH).
	 * Use this when you need to access meals outside of a transaction.
	 */
	@Query("SELECT dp FROM DayPlan dp LEFT JOIN FETCH dp.meals WHERE dp.userId = :userId AND dp.date = :date")
	Optional<DayPlan> findByUserIdAndDateWithMeals(@Param("userId") Long userId, @Param("date") LocalDate date);
}
