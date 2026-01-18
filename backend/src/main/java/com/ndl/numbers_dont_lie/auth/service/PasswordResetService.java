package com.ndl.numbers_dont_lie.auth.service;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.PasswordResetToken;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import com.ndl.numbers_dont_lie.repository.PasswordResetTokenRepository;
import com.ndl.numbers_dont_lie.email.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {
  private final UserRepository users;
  private final PasswordResetTokenRepository tokens;
  private final EmailService emailService;

  // Base URL of the frontend used to compose password reset links.
  // Can be overridden via environment variable APP_FRONTEND_BASE_URL or property app.frontend.base-url
  @Value("${app.frontend.base-url:${APP_FRONTEND_BASE_URL:http://localhost:8080}}")
  private String frontendBaseUrl;

  public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens, EmailService emailService) {
    this.users = users; this.tokens = tokens; this.emailService = emailService;
  }

  public void requestReset(String email) {
    users.findByEmail(email).filter(UserEntity::isEmailVerified).ifPresent(user -> {
      PasswordResetToken t = new PasswordResetToken();
      t.setUser(user);
      t.setToken(UUID.randomUUID().toString());
      t.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
      tokens.save(t);
  String base = frontendBaseUrl.replaceAll("/+\\z", "");
  String link = String.format("%s/reset?token=%s", base, t.getToken());
      String subject = "Password reset";
      String text = "Click the link to reset your password: " + link + "\n\nIf you did not request this, ignore this email.";
      emailService.sendSimpleEmail(user.getEmail(), subject, text);
    });
  }

  public void reset(String token, String newPassword) {
    PasswordResetToken t = tokens.findByToken(token).orElseThrow(() -> new IllegalArgumentException("invalid token"));
    if (t.getUsedAt() != null || Instant.now().isAfter(t.getExpiresAt()))
      throw new IllegalStateException("token expired or used");
    UserEntity u = t.getUser();
    // enforce same minimal password rules as registration
    if (newPassword == null || newPassword.length() < 6) {
      throw new IllegalArgumentException("Invalid input");
    }
    u.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
    t.setUsedAt(Instant.now());
    users.save(u);
    tokens.save(t);
  }
}
