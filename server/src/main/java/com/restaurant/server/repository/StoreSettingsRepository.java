package com.restaurant.server.repository;

import com.restaurant.server.entity.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {

    @Query("""
        SELECT s FROM StoreSettings s
        LEFT JOIN FETCH s.translations
        WHERE s.id = 1
        """)
    Optional<StoreSettings> findSingleton();
}