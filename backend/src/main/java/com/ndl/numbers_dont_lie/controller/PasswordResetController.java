package com.ndl.numbers_dont_lie.controller;

import com.ndl.numbers_dont_lie.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class PasswordResetController {
  private record ForgotDto(String email) {}
  private record ResetDto(String token, String password) {}
  private final PasswordResetService service;
  public PasswordResetController(PasswordResetService service){ this.service = service; }

  @PostMapping("/forgot")
  public ResponseEntity<?> forgot(@RequestBody ForgotDto dto) {
    service.requestReset(dto.email());
    return ResponseEntity.ok(Map.of("ok", true));
  }

  @PostMapping("/reset")
  public ResponseEntity<?> reset(@RequestBody ResetDto dto) {
    try { service.reset(dto.token(), dto.password()); return ResponseEntity.ok(Map.of("ok", true)); }
    catch (IllegalArgumentException e){ return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    catch (IllegalStateException e){ return ResponseEntity.status(410).body(Map.of("error", e.getMessage())); }
  }
}
