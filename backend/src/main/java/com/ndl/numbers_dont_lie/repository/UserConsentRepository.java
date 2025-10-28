package com.ndl.numbers_dont_lie.repository;

import com.ndl.numbers_dont_lie.entity.UserConsent;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserConsentRepository extends JpaRepository<UserConsent, Long> {
    Optional<UserConsent> findByUser(UserEntity user);
}
