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

    /**
     * The lead a browser halfway through the form is holding a handle to.
     *
     * <p>The token is a UUID and not an obfuscated id on purpose: the obfuscated ids rotate on
     * every restart, so a deploy mid-form would strand the visitor.
     */
    Optional<BookingInquiry> findByDraftToken(String draftToken);
    long countByStatus(InquiryStatus status);
    List<BookingInquiry> findByItineraryId(Long itineraryId);

    @Query("SELECT COUNT(b) FROM BookingInquiry b WHERE b.createdAt >= :startOfMonth AND b.createdAt < :startOfNextMonth")
    long countByMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("startOfNextMonth") LocalDateTime startOfNextMonth);

    boolean existsByCode(String code);

    /**
     * The highest sequence number already used this month, or 0 when none is.
     *
     * <p>Counting the month's rows instead gives a different answer the moment one is deleted:
     * the count drops by one, the next lead is handed a code that already exists, and it
     * collides from then on. The code format encodes a sequence, so the sequence is what gets
     * read back.
     *
     * <p>Native because it has to parse four characters out of the middle of the code.
     */
    @Query(value = "SELECT COALESCE(MAX(CAST(SUBSTRING(code, 5, 4) AS UNSIGNED)), 0) "
                 + "FROM booking_inquiries WHERE code LIKE :pattern", nativeQuery = true)
    int highestSequenceInMonth(@Param("pattern") String pattern);

    @Query("SELECT b.id FROM BookingInquiry b WHERE b.id > :currentId ORDER BY b.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT b.id FROM BookingInquiry b WHERE b.id < :currentId ORDER BY b.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT b.id FROM BookingInquiry b ORDER BY b.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT b.id FROM BookingInquiry b ORDER BY b.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
