package com.itineraryledger.kabengosafaris.EmailEvent;

import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for EmailEvent entity operations
 */
@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<EmailEvent> {

    /**
     * Find an email event by its name
     * @param name The event name
     * @return Optional containing the event if found
     */
    Optional<EmailEvent> findByName(String name);

    /**
     * Check if an event exists by name
     * @param name The event name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

    @Query("SELECT e.id FROM EmailEvent e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM EmailEvent e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM EmailEvent e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM EmailEvent e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
