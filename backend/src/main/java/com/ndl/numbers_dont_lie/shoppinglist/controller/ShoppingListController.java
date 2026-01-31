package com.ndl.numbers_dont_lie.shoppinglist.controller;

import com.ndl.numbers_dont_lie.shoppinglist.dto.DailyShoppingListResponse;
import com.ndl.numbers_dont_lie.shoppinglist.dto.MealShoppingListResponse;
import com.ndl.numbers_dont_lie.shoppinglist.dto.WeeklyShoppingListResponse;
import com.ndl.numbers_dont_lie.shoppinglist.service.ShoppingListService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shopping-list")
public class ShoppingListController {
    private final ShoppingListService shoppingListService;

    public ShoppingListController(ShoppingListService shoppingListService) {
        this.shoppingListService = shoppingListService;
    }

    @GetMapping("/day")
    public ResponseEntity<?> getDailyShoppingList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String date) {
        if (userId == null || date == null || date.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "userId and date are required"
            ));
        }

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid date format. Use YYYY-MM-DD."
            ));
        }

        DailyShoppingListResponse response = shoppingListService.buildDailyShoppingList(userId, parsedDate);
        if (response.getItems() == null) {
            response.setItems(Collections.emptyList());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/week")
    public ResponseEntity<?> getWeeklyShoppingList(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startDate) {
        if (userId == null || startDate == null || startDate.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "userId and startDate are required"
            ));
        }

        LocalDate parsedStartDate;
        try {
            parsedStartDate = LocalDate.parse(startDate);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Invalid date format. Use YYYY-MM-DD."
            ));
        }

        WeeklyShoppingListResponse response = shoppingListService.buildWeeklyShoppingList(userId, parsedStartDate);
        if (response.getItems() == null) {
            response.setItems(Collections.emptyList());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/meal")
    public ResponseEntity<?> getMealShoppingList(
            @RequestParam(required = false) Long mealId) {
        if (mealId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "mealId is required"
            ));
        }

        MealShoppingListResponse response = shoppingListService.buildMealShoppingList(mealId);
        if (response.getItems() == null) {
            response.setItems(Collections.emptyList());
        }
        return ResponseEntity.ok(response);
    }
}
