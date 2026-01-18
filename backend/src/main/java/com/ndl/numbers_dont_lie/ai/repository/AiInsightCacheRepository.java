package com.ndl.numbers_dont_lie.ai.repository;
import com.ndl.numbers_dont_lie.entity.UserEntity;
import com.ndl.numbers_dont_lie.ai.entity.AiInsightCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiInsightCacheRepository extends JpaRepository<AiInsightCache, Long> {
  Optional<AiInsightCache> findFirstByUserAndScopeAndGoalKeyOrderByGeneratedAtDesc(UserEntity user, String scope, String goalKey);
}
