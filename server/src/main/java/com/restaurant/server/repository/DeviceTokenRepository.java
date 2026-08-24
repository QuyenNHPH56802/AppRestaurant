package com.restaurant.server.repository;

import com.restaurant.server.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    /**
     * Idempotent upsert lookup. Returns the existing row if the same
     * (user_id, token) pair was registered before, otherwise empty.
     */
    Optional<DeviceToken> findFirstByUserIdAndToken(Long userId, String token);

    /** All active tokens for a user — used by NotificationService. */
    List<DeviceToken> findAllByUserIdAndIsActive(Long userId, Integer isActive);

    /** All tokens for a user (including disabled) — used by logout-all-devices. */
    List<DeviceToken> findAllByUserId(Long userId);

    /** Reverse lookup when FCM reports UNREGISTERED. */
    Optional<DeviceToken> findFirstByToken(String token);

    long countByUserIdAndIsActive(Long userId, Integer isActive);

    /** Cleanup job: disable tokens last-seen before the cutoff. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DeviceToken dt SET dt.isActive = 0, dt.updatedAt = :now " +
           "WHERE dt.isActive = 1 AND dt.lastSeenAt < :cutoff")
    int deactivateStale(@Param("cutoff") Instant cutoff, @Param("now") Instant now);
}
