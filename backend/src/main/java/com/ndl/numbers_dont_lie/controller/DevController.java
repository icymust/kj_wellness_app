package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.repository.PasswordResetTokenRepository;
import com.ndl.numbers_dont_lie.service.EmailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/dev") //Is this only for dev? How to switc on/off in production?
public class DevController {
  private final PasswordResetTokenRepository tokens;
  private final EmailService emailService;

  public DevController(PasswordResetTokenRepository tokens, EmailService emailService){
    this.tokens = tokens;
    this.emailService = emailService;
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
}
