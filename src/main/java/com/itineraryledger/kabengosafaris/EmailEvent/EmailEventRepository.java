package com.itineraryledger.kabengosafaris.EmailEvent;

import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for EmailEvent entity operations
 */
@Repository
public interface EmailEventRepository extends JpaRepository<EmailEvent, Long> {

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
}
