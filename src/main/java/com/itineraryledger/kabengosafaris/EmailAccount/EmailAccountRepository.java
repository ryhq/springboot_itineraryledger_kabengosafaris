package com.itineraryledger.kabengosafaris.EmailAccount;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;

/**
 * Repository for EmailAccount entity
 * Provides CRUD operations, custom queries, and specification-based filtering for email accounts
 */
@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long>, JpaSpecificationExecutor<EmailAccount> {

    boolean existsByProviderType(EmailAccountProvider providerType);

    Optional<EmailAccount> findByEmail(String email);

    Optional<EmailAccount> findByName(String name);

    /**
     * Update the default configuration flag
     * Sets all to false except the one with given id
     */
    @Modifying
    @Query("UPDATE EmailAccount SET isDefault = false WHERE id != :id")
    void setOnlyOneDefault(@Param("id") Long id);

    /**
     * Find the first enabled and default email account ordered by creation date descending
     */
    Optional<EmailAccount> findFirstByEnabledTrueAndIsDefaultTrueOrderByCreatedAtDesc();
}
