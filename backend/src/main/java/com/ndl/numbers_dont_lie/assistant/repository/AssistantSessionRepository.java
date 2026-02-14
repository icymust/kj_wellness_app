package com.ndl.numbers_dont_lie.assistant.repository;

import com.ndl.numbers_dont_lie.assistant.entity.AssistantSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssistantSessionRepository extends JpaRepository<AssistantSession, Long> {

    @Query("select s from AssistantSession s where s.id = :sessionId and s.user.id = :userId")
    Optional<AssistantSession> findByIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    @Query("select s from AssistantSession s where s.user.id = :userId order by s.updatedAt desc")
    List<AssistantSession> findAllByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId);
}
