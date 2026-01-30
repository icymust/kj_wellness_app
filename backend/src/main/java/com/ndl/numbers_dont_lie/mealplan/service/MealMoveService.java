package com.ndl.numbers_dont_lie.mealplan.service;

import com.ndl.numbers_dont_lie.mealplan.entity.DayPlan;
import com.ndl.numbers_dont_lie.mealplan.entity.Meal;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class MealMoveService {
    private final MealRepository mealRepository;
    private final DayPlanRepository dayPlanRepository;

    public MealMoveService(MealRepository mealRepository, DayPlanRepository dayPlanRepository) {
        this.mealRepository = mealRepository;
        this.dayPlanRepository = dayPlanRepository;
    }

    @Transactional
    public DayPlan moveMeal(Long mealId, String direction) {
        if (direction == null || (!direction.equalsIgnoreCase("up") && !direction.equalsIgnoreCase("down"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction must be 'up' or 'down'");
        }

        Optional<Meal> mealOpt = mealRepository.findById(mealId);
        if (mealOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found");
        }

        Meal meal = mealOpt.get();
        DayPlan dayPlan = meal.getDayPlan();
        if (dayPlan == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meal has no day plan");
        }

        List<Meal> meals = dayPlan.getMeals();
        meals.sort(Comparator.comparing(Meal::getPlannedTime));

        int index = -1;
        for (int i = 0; i < meals.size(); i++) {
            if (meals.get(i).getId().equals(mealId)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Meal not found in day plan");
        }

        int targetIndex = direction.equalsIgnoreCase("up") ? index - 1 : index + 1;
        if (targetIndex < 0 || targetIndex >= meals.size()) {
            meals.sort(Comparator.comparing(Meal::getPlannedTime));
            return dayPlan;
        }

        Meal other = meals.get(targetIndex);
        LocalDateTime tmpTime = meal.getPlannedTime();
        meal.setPlannedTime(other.getPlannedTime());
        other.setPlannedTime(tmpTime);

        meals.sort(Comparator.comparing(Meal::getPlannedTime));
        return dayPlanRepository.findByIdWithMeals(dayPlan.getId()).orElse(dayPlan);
    }
}
