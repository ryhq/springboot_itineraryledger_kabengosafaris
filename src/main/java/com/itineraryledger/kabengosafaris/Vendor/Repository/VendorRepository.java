package com.itineraryledger.kabengosafaris.Vendor.Repository;

import com.itineraryledger.kabengosafaris.Vendor.Entity.Vendor;
import com.itineraryledger.kabengosafaris.Vendor.Enums.VendorType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    Optional<Vendor> findByCode(String code);
    Optional<Vendor> findByCodeIgnoreCase(String code);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByType(VendorType type);
    long countByIsActiveTrue();

    List<Vendor> findByType(VendorType type);
    List<Vendor> findByIsActiveTrue();

    @Query("SELECT v FROM Vendor v " +
           "WHERE LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Vendor> searchVendors(@Param("search") String search);

    // Navigation (mirrors InvoiceRepository pattern)
    @Query("SELECT v.id FROM Vendor v WHERE v.id > :currentId ORDER BY v.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT v.id FROM Vendor v WHERE v.id < :currentId ORDER BY v.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT v.id FROM Vendor v ORDER BY v.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT v.id FROM Vendor v ORDER BY v.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
