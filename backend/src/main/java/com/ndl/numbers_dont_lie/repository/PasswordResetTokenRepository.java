package com.ndl.numbers_dont_lie.repository;

import com.ndl.numbers_dont_lie.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
  Optional<PasswordResetToken> findByToken(String token);
  Optional<PasswordResetToken> findTopByOrderByIdDesc();
}
