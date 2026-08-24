package com.restaurant.server.repository;

import com.restaurant.server.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.translations
        WHERE c.status = com.restaurant.server.entity.Category$Status.ACTIVE
        ORDER BY c.sortOrder ASC, c.id ASC
        """)
    List<Category> findAllActiveWithTranslations();

    @Query("""
        SELECT c FROM Category c
        LEFT JOIN FETCH c.translations
        WHERE c.id = :id
        """)
    java.util.Optional<Category> findByIdWithTranslations(@Param("id") Long id);

    @Query("""
        SELECT c FROM Category c
        LEFT JOIN FETCH c.translations
        ORDER BY c.sortOrder ASC, c.id ASC
        """)
    List<Category> findAllWithTranslations();

    @Query(value = """
        SELECT DISTINCT c FROM Category c
        LEFT JOIN FETCH c.translations
        """,
        countQuery = "SELECT COUNT(c) FROM Category c")
    org.springframework.data.domain.Page<Category> findAllAdmin(org.springframework.data.domain.Pageable pageable);
}