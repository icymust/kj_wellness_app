package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

	/**
	 * Find a day plan by id with meals eagerly loaded.
	 */
	@Query("SELECT dp FROM DayPlan dp LEFT JOIN FETCH dp.meals WHERE dp.id = :id")
	Optional<DayPlan> findByIdWithMeals(@Param("id") Long id);

	/**
	 * Find a single day plan by date and duration (e.g., DAILY), with meals eagerly loaded.
	 */
	@Query("SELECT dp FROM DayPlan dp " +
		   "LEFT JOIN FETCH dp.meals " +
		   "JOIN dp.mealPlanVersion v " +
		   "JOIN v.mealPlan p " +
		   "WHERE dp.userId = :userId AND dp.date = :date AND p.duration = :duration")
	Optional<DayPlan> findByUserIdAndDateWithMealsAndDuration(
			@Param("userId") Long userId,
			@Param("date") LocalDate date,
			@Param("duration") PlanDuration duration);

	/**
	 * Find day plans in a date range with meals eagerly loaded, filtered by plan duration.
	 * Used to load persisted weekly plans without regeneration.
	 */
	@Query("SELECT DISTINCT dp FROM DayPlan dp " +
		   "LEFT JOIN FETCH dp.meals " +
		   "JOIN dp.mealPlanVersion v " +
		   "JOIN v.mealPlan p " +
		   "WHERE dp.userId = :userId AND p.duration = :duration AND dp.date BETWEEN :startDate AND :endDate " +
		   "ORDER BY dp.date ASC")
	List<DayPlan> findByUserIdAndDateRangeWithMealsAndDuration(
			@Param("userId") Long userId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("duration") PlanDuration duration);

	/**
	 * Find day plans for a specific meal plan (weekly) in a date range with meals.
	 */
	@Query("SELECT DISTINCT dp FROM DayPlan dp " +
		   "LEFT JOIN FETCH dp.meals " +
		   "JOIN dp.mealPlanVersion v " +
		   "JOIN v.mealPlan p " +
		   "WHERE p.id = :mealPlanId AND dp.date BETWEEN :startDate AND :endDate " +
		   "ORDER BY dp.date ASC")
	List<DayPlan> findByMealPlanIdAndDateRangeWithMeals(
			@Param("mealPlanId") Long mealPlanId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);
}
