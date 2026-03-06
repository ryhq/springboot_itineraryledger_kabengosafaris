package com.itineraryledger.kabengosafaris.Testimony.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Testimony.Entity.Testimony;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

@Repository
public interface TestimonyRepository extends JpaRepository<Testimony, Long>, JpaSpecificationExecutor<Testimony> {

    List<Testimony> findBySource(TestimonySource source);

    List<Testimony> findByIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();

    List<Testimony> findByIsFeaturedTrueAndIsApprovedTrueAndIsActiveTrueOrderByDisplayOrderAsc();

    long countBySource(TestimonySource source);

    long countByIsApprovedTrue();

    long countByIsFeaturedTrue();

    long countByIsActiveTrue();

    @Query("SELECT t.id FROM Testimony t WHERE t.id > :currentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT t.id FROM Testimony t WHERE t.id < :currentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT t.id FROM Testimony t ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT t.id FROM Testimony t ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
