package com.ndl.numbers_dont_lie.repository;

import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.entity.WeightEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface WeightEntryRepository extends JpaRepository<WeightEntry, Long> {
  List<WeightEntry> findAllByUserOrderByAtAsc(UserEntity user);
  Optional<WeightEntry> findByUserAndAt(UserEntity user, Instant at);
}
