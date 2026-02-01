package com.ndl.numbers_dont_lie.mealplan.repository;

import com.ndl.numbers_dont_lie.mealplan.entity.MealPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.MealPlanVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.ndl.numbers_dont_lie.mealplan.entity.PlanDuration;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Long> {
    
    List<MealPlan> findByUserId(Long userId);

    Optional<MealPlan> findFirstByUserId(Long userId);

    Optional<MealPlan> findTopByUserIdAndDurationOrderByIdDesc(Long userId, PlanDuration duration);

    @Modifying
    @Query("UPDATE MealPlan p SET p.currentVersion = (SELECT v FROM MealPlanVersion v WHERE v.id = :versionId) WHERE p.id = :planId")
    int updateCurrentVersion(@Param("planId") Long planId, @Param("versionId") Long versionId);
}
