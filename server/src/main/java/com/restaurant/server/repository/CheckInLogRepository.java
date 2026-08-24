package com.restaurant.server.repository;

import com.restaurant.server.entity.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckInLogRepository extends JpaRepository<CheckInLog, Long> {

    List<CheckInLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<CheckInLog> findAllByZoneIdOrderByCreatedAtDesc(Long zoneId);

    /** Returns the latest CHECK_IN event for a user, or empty if there is none. */
    java.util.Optional<CheckInLog> findFirstByUserIdAndActionOrderByCreatedAtDesc(
            Long userId, CheckInLog.Action action);

    /** Phase G — admin browse: paginated global feed, newest first. */
    List<CheckInLog> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
