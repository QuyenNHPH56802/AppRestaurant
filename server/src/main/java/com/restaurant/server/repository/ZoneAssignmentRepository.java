package com.restaurant.server.repository;

import com.restaurant.server.entity.ZoneAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneAssignmentRepository extends JpaRepository<ZoneAssignment, Long> {

    Optional<ZoneAssignment> findFirstByUserIdAndIsCurrentOrderByEffectiveFromDesc(
            Long userId, Integer isCurrent);

    List<ZoneAssignment> findAllByUserIdOrderByEffectiveFromDesc(Long userId);

    List<ZoneAssignment> findAllByZoneIdAndIsCurrent(Long zoneId, Integer isCurrent);

    /** All currently-active assignments across all zones (is_current = 1). */
    List<ZoneAssignment> findAllByIsCurrent(Integer isCurrent);
}
