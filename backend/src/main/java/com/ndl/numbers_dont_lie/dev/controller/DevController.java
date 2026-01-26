package com.ndl.numbers_dont_lie.dev.controller;

import com.ndl.numbers_dont_lie.repository.PasswordResetTokenRepository;
import com.ndl.numbers_dont_lie.email.service.EmailService;
import com.ndl.numbers_dont_lie.mealplan.repository.MealRepository;
import com.ndl.numbers_dont_lie.mealplan.repository.DayPlanRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/dev")
public class DevController {
  private final PasswordResetTokenRepository tokens;
  private final EmailService emailService;
  private final MealRepository mealRepository;
  private final DayPlanRepository dayPlanRepository;

  public DevController(PasswordResetTokenRepository tokens, EmailService emailService,
                       MealRepository mealRepository, DayPlanRepository dayPlanRepository){
    this.tokens = tokens;
    this.emailService = emailService;
    this.mealRepository = mealRepository;
    this.dayPlanRepository = dayPlanRepository;
  }

  @GetMapping("/outbox")
  public Map<String,Object> outbox(){
    return tokens.findTopByOrderByIdDesc().map(t -> {
      Map<String,Object> m = new HashMap<>();
      m.put("token", t.getToken());
      m.put("email", t.getUser().getEmail());
      m.put("expiresAt", t.getExpiresAt());
      m.put("usedAt", t.getUsedAt());
      return m;
    }).orElse(Collections.emptyMap());
  }

  // Временная проверка отправки письма через Mailtrap
  @GetMapping("/test/email")
  public Map<String, Object> testEmail(@RequestParam("to") String to){
    emailService.sendTestEmail(to);
    return Map.of("status", "sent", "to", to);
  }

  // Debug: Clear meals and day plans to force regeneration with new code
  @GetMapping("/clear-meal-plans")
  public Map<String, Object> clearMealPlans() {
    long mealCount = mealRepository.count();
    long planCount = dayPlanRepository.count();
    
    mealRepository.deleteAll();
    dayPlanRepository.deleteAll();
    
    return Map.of(
      "status", "cleared",
      "mealsDeleted", mealCount,
      "dayPlansDeleted", planCount
    );
  }
}
