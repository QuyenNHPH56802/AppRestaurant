package com.restaurant.server.repository;

import com.restaurant.server.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findAllByIsActiveOrderBySortOrderAsc(Integer isActive);

    List<Shift> findAllByOrderBySortOrderAsc();
}
