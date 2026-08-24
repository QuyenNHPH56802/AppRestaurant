package com.restaurant.server.repository;

import com.restaurant.server.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {

    Optional<Zone> findByCode(String code);

    List<Zone> findAllByStatusOrderBySortOrderAsc(Zone.Status status);

    List<Zone> findAllByOrderBySortOrderAsc();

    Optional<Zone> findByQrToken(String qrToken);

    /** Fetch zone + translations in one query to avoid Lazy init issues in controllers. */
    @Query("SELECT z FROM Zone z LEFT JOIN FETCH z.translations WHERE z.code = :code")
    Optional<Zone> findByCodeWithTranslations(String code);
}
