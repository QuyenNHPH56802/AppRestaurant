package com.restaurant.server.repository;

import com.restaurant.server.entity.ChecklistTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChecklistTaskRepository extends JpaRepository<ChecklistTask, Long> {

    List<ChecklistTask> findAllByChecklistIdOrderBySortOrderAsc(Long checklistId);

    List<ChecklistTask> findAllByChecklistIdAndIsActiveOrderBySortOrderAsc(
            Long checklistId, Integer isActive);
}
