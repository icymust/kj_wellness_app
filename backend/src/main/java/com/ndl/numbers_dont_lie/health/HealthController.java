package com.ndl.numbers_dont_lie.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

   @GetMapping("/health")
   public Map<String, Object> health() {
      return Map.of(
            "ok", true,
            "service", "numbers-dont-lie",
            "time", Instant.now().toString());
   }
}
