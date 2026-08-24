package com.restaurant.server.repository;

import com.restaurant.server.entity.Food;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * STAFF-visible foods: HIDDEN is excluded. Includes SOLD_OUT (so staff can see "hết món").
     * Translations are fetched eagerly in a single query.
     */
    @Query(value = """
        SELECT DISTINCT f FROM Food f
        JOIN FETCH f.category c
        LEFT JOIN FETCH f.translations
        WHERE f.status <> com.restaurant.server.entity.Food$Status.HIDDEN
        """,
        countQuery = """
        SELECT COUNT(f) FROM Food f
        WHERE f.status <> com.restaurant.server.entity.Food$Status.HIDDEN
        """)
    Page<Food> findAllVisible(Pageable pageable);

    @Query("""
        SELECT DISTINCT f FROM Food f
        JOIN FETCH f.category c
        LEFT JOIN FETCH f.translations
        WHERE f.id = :id
        """)
    Optional<Food> findByIdWithTranslations(@Param("id") Long id);

    @Query(value = """
        SELECT DISTINCT f FROM Food f
        JOIN FETCH f.category c
        LEFT JOIN FETCH f.translations
        WHERE f.featured = true
          AND f.status = com.restaurant.server.entity.Food$Status.AVAILABLE
        """,
        countQuery = """
        SELECT COUNT(f) FROM Food f
        WHERE f.featured = true
          AND f.status = com.restaurant.server.entity.Food$Status.AVAILABLE
        """)
    Page<Food> findFeatured(Pageable pageable);

    /**
     * Admin view: returns all foods regardless of status. Used by /api/admin/foods.
     */
    @Query(value = """
        SELECT DISTINCT f FROM Food f
        JOIN FETCH f.category c
        LEFT JOIN FETCH f.translations
        """,
        countQuery = "SELECT COUNT(f) FROM Food f")
    Page<Food> findAllAdmin(Pageable pageable);

    /**
     * Search by translated name/description/ingredients. Case-insensitive substring match on the
     * requested language. STAFF does not see HIDDEN.
     */
    @Query(value = """
        SELECT DISTINCT f FROM Food f
        JOIN FETCH f.category c
        JOIN f.translations t
        WHERE f.status <> com.restaurant.server.entity.Food$Status.HIDDEN
          AND t.languageCode = :lang
          AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.ingredients) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
        countQuery = """
        SELECT COUNT(f) FROM Food f
        JOIN f.translations t
        WHERE f.status <> com.restaurant.server.entity.Food$Status.HIDDEN
          AND t.languageCode = :lang
          AND (LOWER(t.name) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(t.ingredients) LIKE LOWER(CONCAT('%', :q, '%')))
        """)
    Page<Food> searchByLanguage(@Param("q") String q, @Param("lang") String lang, Pageable pageable);

    long countByStatus(Food.Status status);
}