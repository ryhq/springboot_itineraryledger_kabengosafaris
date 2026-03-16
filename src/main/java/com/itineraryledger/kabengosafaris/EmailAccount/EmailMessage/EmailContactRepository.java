package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;

@Repository
public interface EmailContactRepository extends JpaRepository<EmailContact, Long>, JpaSpecificationExecutor<EmailContact> {

    Optional<EmailContact> findByEmailAccountIdAndEmailAddress(Long emailAccountId, String emailAddress);

    /**
     * Search contacts by email address or display name (case-insensitive), ordered by frequency desc
     */
    @Query("SELECT c FROM EmailContact c WHERE c.emailAccount.id = :accountId " +
           "AND (LOWER(c.emailAddress) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(c.displayName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY c.frequency DESC, c.lastContactedAt DESC")
    List<EmailContact> searchContacts(@Param("accountId") Long accountId,
                                      @Param("search") String search,
                                      Pageable pageable);

    Page<EmailContact> findByEmailAccountId(Long emailAccountId, Pageable pageable);

    List<EmailContact> findByEmailAccountIdAndIsStarredTrueOrderByFrequencyDesc(Long emailAccountId);

    long countByEmailAccountId(Long emailAccountId);

    // Circular navigation — scoped to email account
    @Query("SELECT c.id FROM EmailContact c WHERE c.emailAccount.id = :accountId AND c.id > :currentId ORDER BY c.id ASC LIMIT 1")
    Optional<Long> findNextIdInAccount(@Param("accountId") Long accountId, @Param("currentId") Long currentId);

    @Query("SELECT c.id FROM EmailContact c WHERE c.emailAccount.id = :accountId AND c.id < :currentId ORDER BY c.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInAccount(@Param("accountId") Long accountId, @Param("currentId") Long currentId);

    @Query("SELECT c.id FROM EmailContact c WHERE c.emailAccount.id = :accountId ORDER BY c.id ASC LIMIT 1")
    Optional<Long> findFirstIdInAccount(@Param("accountId") Long accountId);

    @Query("SELECT c.id FROM EmailContact c WHERE c.emailAccount.id = :accountId ORDER BY c.id DESC LIMIT 1")
    Optional<Long> findLastIdInAccount(@Param("accountId") Long accountId);
}
