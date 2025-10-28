package com.ndl.numbers_dont_lie.repository;

import com.ndl.numbers_dont_lie.entity.ActivityEntry;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ActivityEntryRepository extends JpaRepository<ActivityEntry, Long> {
  Optional<ActivityEntry> findByUserAndAt(UserEntity user, Instant at);
  List<ActivityEntry> findAllByUserAndAtBetweenOrderByAtAsc(UserEntity user, Instant from, Instant to);
}
