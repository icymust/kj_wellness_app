package com.ndl.numbers_dont_lie.assistant.repository;

import com.ndl.numbers_dont_lie.assistant.entity.AssistantMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, Long> {

    @Query("select m from AssistantMessage m where m.session.id = :sessionId order by m.createdAt asc")
    List<AssistantMessage> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") Long sessionId);

    @Query("select m from AssistantMessage m where m.session.id = :sessionId order by m.createdAt desc")
    List<AssistantMessage> findRecentBySessionId(@Param("sessionId") Long sessionId, Pageable pageable);
}
