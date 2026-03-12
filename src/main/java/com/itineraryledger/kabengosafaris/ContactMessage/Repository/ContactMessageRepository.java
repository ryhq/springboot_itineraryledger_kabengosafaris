package com.itineraryledger.kabengosafaris.ContactMessage.Repository;

import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    @Query("SELECT COUNT(c) FROM ContactMessage c WHERE c.createdAt >= :startOfMonth AND c.createdAt < :startOfNextMonth")
    long countByMonth(@Param("startOfMonth") LocalDateTime startOfMonth,
                      @Param("startOfNextMonth") LocalDateTime startOfNextMonth);
}
