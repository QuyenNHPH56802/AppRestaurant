package com.restaurant.server.repository;

import com.restaurant.server.entity.ShiftAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findAllByUserIdOrderByDateDesc(Long userId);

    List<ShiftAssignment> findAllByShiftIdAndDate(Long shiftId, String date);
}
