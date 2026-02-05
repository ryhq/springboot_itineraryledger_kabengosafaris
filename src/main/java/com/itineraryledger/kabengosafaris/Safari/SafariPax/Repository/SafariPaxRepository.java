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
}
