package com.itineraryledger.kabengosafaris.BookingInquiry.Repository;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingInquiryRepository extends JpaRepository<BookingInquiry, Long>, JpaSpecificationExecutor<BookingInquiry> {

    boolean existsByCustomerId(Long customerId);
    List<BookingInquiry> findByEmailIgnoreCase(String email);
    long countByStatus(InquiryStatus status);
    List<BookingInquiry> findByItineraryId(Long itineraryId);

    @Query("SELECT COUNT(b) FROM BookingInquiry b WHERE b.createdAt >= :startOfMonth AND b.createdAt < :startOfNextMonth")
    long countByMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("startOfNextMonth") LocalDateTime startOfNextMonth);

    @Query("SELECT b.id FROM BookingInquiry b WHERE b.id > :currentId ORDER BY b.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT b.id FROM BookingInquiry b WHERE b.id < :currentId ORDER BY b.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT b.id FROM BookingInquiry b ORDER BY b.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT b.id FROM BookingInquiry b ORDER BY b.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
