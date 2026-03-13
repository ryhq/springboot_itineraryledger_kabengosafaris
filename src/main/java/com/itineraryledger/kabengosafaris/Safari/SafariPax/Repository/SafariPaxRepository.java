package com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository;

import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SafariPaxRepository extends JpaRepository<SafariPax, Long>, JpaSpecificationExecutor<SafariPax> {

    List<SafariPax> findBySafariId(Long safariId);

    Optional<SafariPax> findBySafariIdAndNationCategoryIdAndAgeCategoryId(
            Long safariId, Long nationCategoryId, Long ageCategoryId);

    boolean existsBySafariIdAndNationCategoryIdAndAgeCategoryId(
            Long safariId, Long nationCategoryId, Long ageCategoryId);

    @Query("SELECT SUM(sp.count) FROM SafariPax sp WHERE sp.safari.id = :safariId")
    Integer getTotalPaxCountBySafariId(@Param("safariId") Long safariId);

    void deleteBySafariId(Long safariId);

    // ========================
    // PARENT-SCOPED NAVIGATION QUERIES (circular next/previous within safari)
    // ========================

    @Query("SELECT p.id FROM SafariPax p WHERE p.safari.id = :parentId AND p.id > :currentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findNextIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM SafariPax p WHERE p.safari.id = :parentId AND p.id < :currentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInParent(@Param("parentId") Long parentId, @Param("currentId") Long currentId);

    @Query("SELECT p.id FROM SafariPax p WHERE p.safari.id = :parentId ORDER BY p.id ASC LIMIT 1")
    Optional<Long> findFirstIdInParent(@Param("parentId") Long parentId);

    @Query("SELECT p.id FROM SafariPax p WHERE p.safari.id = :parentId ORDER BY p.id DESC LIMIT 1")
    Optional<Long> findLastIdInParent(@Param("parentId") Long parentId);
}
