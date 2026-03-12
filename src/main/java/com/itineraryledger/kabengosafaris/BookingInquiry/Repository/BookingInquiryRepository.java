package com.itineraryledger.kabengosafaris.BookingInquiry.Repository;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingInquiryRepository extends JpaRepository<BookingInquiry, Long> {
    List<BookingInquiry> findByEmailIgnoreCase(String email);
    long countByStatus(InquiryStatus status);
    List<BookingInquiry> findByItineraryId(Long itineraryId);

    @Query("SELECT COUNT(b) FROM BookingInquiry b WHERE b.createdAt >= :startOfMonth AND b.createdAt < :startOfNextMonth")
    long countByMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("startOfNextMonth") LocalDateTime startOfNextMonth);
}
