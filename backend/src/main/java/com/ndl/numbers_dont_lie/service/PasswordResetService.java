package com.ndl.numbers_dont_lie.service;

import com.ndl.numbers_dont_lie.entity.PasswordResetToken;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.repository.PasswordResetTokenRepository;
import com.ndl.numbers_dont_lie.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {
  private final UserRepository users;
  private final PasswordResetTokenRepository tokens;

  public PasswordResetService(UserRepository users, PasswordResetTokenRepository tokens) {
    this.users = users; this.tokens = tokens;
  }

  public void requestReset(String email) {
    users.findByEmail(email).filter(UserEntity::isEmailVerified).ifPresent(user -> {
      PasswordResetToken t = new PasswordResetToken();
      t.setUser(user);
      t.setToken(UUID.randomUUID().toString());
      t.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
      tokens.save(t);
      // emulate sending email by logging
      System.out.println("[MAIL] Reset link: http://localhost:8080/auth/reset?token=" + t.getToken());
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
