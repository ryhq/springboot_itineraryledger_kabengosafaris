package com.itineraryledger.kabengosafaris.ContactMessage.Repository;

import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long>, JpaSpecificationExecutor<ContactMessage> {

    @Query("SELECT COUNT(c) FROM ContactMessage c WHERE c.createdAt >= :startOfMonth AND c.createdAt < :startOfNextMonth")
    long countByMonth(@Param("startOfMonth") LocalDateTime startOfMonth,
                      @Param("startOfNextMonth") LocalDateTime startOfNextMonth);

    @Query("SELECT c.id FROM ContactMessage c WHERE c.id > :currentId ORDER BY c.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT c.id FROM ContactMessage c WHERE c.id < :currentId ORDER BY c.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT c.id FROM ContactMessage c ORDER BY c.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT c.id FROM ContactMessage c ORDER BY c.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
