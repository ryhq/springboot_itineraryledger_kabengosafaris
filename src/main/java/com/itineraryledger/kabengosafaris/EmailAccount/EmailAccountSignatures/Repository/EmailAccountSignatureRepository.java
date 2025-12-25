package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;

/**
 * Repository for EmailSignature entity
 * Handles database operations for email signature templates
 * Supports dynamic filtering via JpaSpecificationExecutor
 */
@Repository
public interface EmailAccountSignatureRepository extends JpaRepository<EmailAccountSignature, Long>, JpaSpecificationExecutor<EmailAccountSignature> {

    /**
     * Find all signatures for a specific email account
     *
     * @param emailAccountId The email account ID
     * @return List of signatures for the account
     */
    List<EmailAccountSignature> findByEmailAccountId(Long emailAccountId);

    /**
     * Find a signature by email account and signature name
     *
     * @param emailAccountId The email account ID
     * @param name The signature name
     * @return Optional containing the signature if found
     */
    Optional<EmailAccountSignature> findByEmailAccountIdAndName(Long emailAccountId, String name);

    /**
     * Find the default signature for an email account
     *
     * @param emailAccountId The email account ID
     * @return Optional containing the default signature if it exists
     */
    Optional<EmailAccountSignature> findByEmailAccountIdAndIsDefaultTrue(Long emailAccountId);

    /**
     * Find a signature by filename
     *
     * @param fileName The filename
     * @return Optional containing the signature if found
     */
    Optional<EmailAccountSignature> findByFileName(String fileName);

    /**
     * Find a signature by filename and email account ID
     *
     * @param fileName The filename
     * @param emailAccountId The email account ID
     * @return The signature if found, null otherwise
     */
    EmailAccountSignature findByFileNameAndEmailAccountId(String fileName, Long emailAccountId);

    /**
     * Check if a signature name already exists for an account
     *
     * @param emailAccountId The email account ID
     * @param name The signature name
     * @return true if signature exists, false otherwise
     */
    boolean existsByEmailAccountIdAndName(Long emailAccountId, String name);

    /**
     * Check if a filename already exists
     *
     * @param fileName The filename
     * @return true if filename exists, false otherwise
     */
    boolean existsByFileName(String fileName);

    /**
     * Set all signatures for an account as non-default
     *
     * @param emailAccountId The email account ID
     */
    @Modifying
    @Query("UPDATE EmailAccountSignature es SET es.isDefault = false WHERE es.emailAccount.id = :emailAccountId")
    void clearAllDefaults(@Param("emailAccountId") Long emailAccountId);

    /**
     * Count signatures for an email account
     *
     * @param emailAccountId The email account ID
     * @return Number of signatures for the account
     */
    long countByEmailAccountId(Long emailAccountId);

    /**
     * Find all enabled signatures for an account
     *
     * @param emailAccountId The email account ID
     * @return List of enabled signatures
     */
    List<EmailAccountSignature> findByEmailAccountIdAndEnabledTrue(Long emailAccountId);

    /**
     * Delete a signature by ID
     * Note: The associated file should be deleted before calling this
     *
     * @param id The signature ID
     */
    void deleteById(Long id);

    /**
     * Delete all signatures for an email account
     * Note: Associated files should be deleted before calling this
     *
     * @param emailAccountId The email account ID
     */
    @Modifying
    @Query("DELETE FROM EmailAccountSignature es WHERE es.emailAccount.id = :emailAccountId")
    void deleteByEmailAccountId(@Param("emailAccountId") Long emailAccountId);
}
