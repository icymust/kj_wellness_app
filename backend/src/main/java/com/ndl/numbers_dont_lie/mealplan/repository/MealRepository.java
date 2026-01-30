package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Long> {
    @Query("select m.dayPlan.userId from Meal m where m.recipeId = :recipeId order by m.id desc")
    List<Long> findUserIdsByRecipeIdOrderByIdDesc(@Param("recipeId") String recipeId, Pageable pageable);
}
