package com.restaurant.server.repository;

import com.restaurant.server.entity.ChecklistCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ChecklistCompletionRepository extends JpaRepository<ChecklistCompletion, Long> {

    List<ChecklistCompletion> findAllByUserIdOrderByCompletedAtDesc(Long userId);

    List<ChecklistCompletion> findAllByTaskIdOrderByCompletedAtDesc(Long taskId);

    List<ChecklistCompletion> findAllByChecklistIdAndCompletedAtBetweenOrderByCompletedAtDesc(
            Long checklistId, Instant from, Instant to);

    long countByChecklistIdAndCompletedAtBetween(Long checklistId, Instant from, Instant to);
}
