package com.restaurant.server.repository;

import com.restaurant.server.entity.NotificationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {

    List<NotificationEvent> findAllByNotificationIdOrderByCreatedAtDesc(Long notificationId);

    /**
     * Idempotency check: has this notification already been pushed (or attempted)
     * on this channel? If a PENDING or SENT row exists, the dispatcher skips.
     */
    Optional<NotificationEvent> findFirstByNotificationIdAndChannelOrderByCreatedAtDesc(
            Long notificationId, NotificationEvent.Channel channel);

    /** Recent events for the admin dashboard. */
    List<NotificationEvent> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);

    /** Cleanup: events still PENDING longer than this are considered stale. */
    List<NotificationEvent> findAllByStatusAndCreatedAtBefore(
            NotificationEvent.Status status, Instant cutoff);

    /**
     * Retry sweep: PUSH events that ended in FAILED but haven't exhausted
     * {@code maxAttempts} and whose {@code last_attempt_at} is older than
     * {@code since}. Results are ordered oldest-first so the scheduler
     * can stop early once it hits a recently-tried event.
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT e FROM NotificationEvent e
             WHERE e.channel = :channel
               AND e.status  = :status
               AND e.attempts < :maxAttempts
               AND e.lastAttemptAt < :since
             ORDER BY e.lastAttemptAt ASC
            """)
    List<NotificationEvent> findRetryCandidates(
            @org.springframework.data.repository.query.Param("channel") NotificationEvent.Channel channel,
            @org.springframework.data.repository.query.Param("status") NotificationEvent.Status status,
            @org.springframework.data.repository.query.Param("maxAttempts") int maxAttempts,
            @org.springframework.data.repository.query.Param("since") Instant since);
}
