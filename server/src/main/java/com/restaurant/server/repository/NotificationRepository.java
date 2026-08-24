package com.restaurant.server.repository;

import com.restaurant.server.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    /** V2.3 / V18: idempotency lookup — used by NotificationService.createAndDispatch. */
    Optional<Notification> findFirstByIdempotencyKey(String idempotencyKey);

    /** Phase G — admin browse: paginated global feed. */
    List<Notification> findAllByOrderByCreatedAtDesc(org.springframework.data.domain.Pageable pageable);
}
