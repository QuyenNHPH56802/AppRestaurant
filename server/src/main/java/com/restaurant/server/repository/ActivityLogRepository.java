package com.restaurant.server.repository;

import com.restaurant.server.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findAllByActorUserIdOrderByCreatedAtDesc(Long actorUserId);

    List<ActivityLog> findAllByActionOrderByCreatedAtDesc(String action);

    List<ActivityLog> findAllByEntityAndEntityIdOrderByCreatedAtDesc(String entity, String entityId);

    List<ActivityLog> findAllByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);
}
