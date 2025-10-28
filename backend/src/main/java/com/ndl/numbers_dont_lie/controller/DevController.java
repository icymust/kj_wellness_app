package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.repository.PasswordResetTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

@RestController
@RequestMapping("/dev")
public class DevController {
  private final PasswordResetTokenRepository tokens;
  public DevController(PasswordResetTokenRepository tokens){ this.tokens = tokens; }

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
}
