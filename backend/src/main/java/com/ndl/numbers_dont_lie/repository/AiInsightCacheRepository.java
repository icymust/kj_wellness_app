package com.ndl.numbers_dont_lie.repository;

import com.ndl.numbers_dont_lie.entity.AiInsightCache;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiInsightCacheRepository extends JpaRepository<AiInsightCache, Long> {
  Optional<AiInsightCache> findFirstByUserAndScopeAndGoalKeyOrderByGeneratedAtDesc(UserEntity user, String scope, String goalKey);
}
