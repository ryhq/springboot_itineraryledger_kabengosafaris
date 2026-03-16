package com.itineraryledger.kabengosafaris.EmailEvent;

import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for EmailTemplate entity operations
 */
@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long>, JpaSpecificationExecutor<EmailTemplate> {

    /**
     * Find all templates for a specific email event
     * @param emailEventId The email event ID
     * @return List of templates
     */
    List<EmailTemplate> findByEmailEventId(Long emailEventId);

    /**
     * Find a template by email event ID and template name
     * @param emailEventId The email event ID
     * @param name The template name
     * @return Optional containing the template if found
     */
    Optional<EmailTemplate> findByEmailEventIdAndName(Long emailEventId, String name);

    /**
     * Find a template by email event ID and file name
     * @param emailEventId The email event ID
     * @param fileName The file name
     * @return Optional containing the template if found
     */
    Optional<EmailTemplate> findByEmailEventIdAndFileName(Long emailEventId, String fileName);

    /**
     * Find the default template for an email event
     * @param emailEventId The email event ID
     * @param isDefault Must be true
     * @param enabled Must be true
     * @return Optional containing the default template if found
     */
    Optional<EmailTemplate> findByEmailEventIdAndIsDefaultAndEnabled(Long emailEventId, Boolean isDefault, Boolean enabled);

    /**
     * Find the system default template for an email event
     * @param emailEventId The email event ID
     * @param isSystemDefault Must be true
     * @return Optional containing the system default template if found
     */
    Optional<EmailTemplate> findByEmailEventIdAndIsSystemDefault(Long emailEventId, Boolean isSystemDefault);

    /**
     * Check if a template name exists for a specific email event
     * @param emailEventId The email event ID
     * @param name The template name
     * @return true if exists, false otherwise
     */
    boolean existsByEmailEventIdAndName(Long emailEventId, String name);

    /**
     * Check if a file name exists for a specific email event
     * @param emailEventId The email event ID
     * @param fileName The file name
     * @return true if exists, false otherwise
     */
    boolean existsByEmailEventIdAndFileName(Long emailEventId, String fileName);

    /**
     * Count templates for a specific email event
     * @param emailEventId The email event ID
     * @return Number of templates
     */
    long countByEmailEventId(Long emailEventId);

    /**
     * Check if an email event has a system default template
     * @param emailEventId The email event ID
     * @return true if exists, false otherwise
     */
    @Query("SELECT COUNT(t) > 0 FROM EmailTemplate t WHERE t.emailEvent.id = :emailEventId AND t.isSystemDefault = true")
    boolean hasSystemDefaultTemplate(@Param("emailEventId") Long emailEventId);

    // ========================
    // NAVIGATION QUERIES - Global (circular next/previous)
    // ========================

    @Query("SELECT t.id FROM EmailTemplate t WHERE t.id > :currentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT t.id FROM EmailTemplate t WHERE t.id < :currentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT t.id FROM EmailTemplate t ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT t.id FROM EmailTemplate t ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findLastId();

    // ========================
    // NAVIGATION QUERIES - Scoped by Email Event (circular next/previous within same event)
    // ========================

    @Query("SELECT t.id FROM EmailTemplate t WHERE t.emailEvent.id = :eventId AND t.id > :currentId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findNextIdByEventId(@Param("eventId") Long eventId, @Param("currentId") Long currentId);

    @Query("SELECT t.id FROM EmailTemplate t WHERE t.emailEvent.id = :eventId AND t.id < :currentId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findPreviousIdByEventId(@Param("eventId") Long eventId, @Param("currentId") Long currentId);

    @Query("SELECT t.id FROM EmailTemplate t WHERE t.emailEvent.id = :eventId ORDER BY t.id ASC LIMIT 1")
    Optional<Long> findFirstIdByEventId(@Param("eventId") Long eventId);

    @Query("SELECT t.id FROM EmailTemplate t WHERE t.emailEvent.id = :eventId ORDER BY t.id DESC LIMIT 1")
    Optional<Long> findLastIdByEventId(@Param("eventId") Long eventId);
}
