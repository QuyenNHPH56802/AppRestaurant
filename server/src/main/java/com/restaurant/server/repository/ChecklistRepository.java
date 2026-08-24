package com.restaurant.server.repository;

import com.restaurant.server.entity.Checklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistRepository extends JpaRepository<Checklist, Long> {

    List<Checklist> findAllByZoneIdOrderBySortOrderAsc(Long zoneId);

    List<Checklist> findAllByZoneIdAndIsActiveOrderBySortOrderAsc(Long zoneId, Integer isActive);

    List<Checklist> findAllByIsActiveOrderBySortOrderAsc(Integer isActive);
}
