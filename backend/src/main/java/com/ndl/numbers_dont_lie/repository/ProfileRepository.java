package com.ndl.numbers_dont_lie.repository;

import com.ndl.numbers_dont_lie.entity.ProfileEntity;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Long> {
  Optional<ProfileEntity> findByUser(UserEntity user);
}
