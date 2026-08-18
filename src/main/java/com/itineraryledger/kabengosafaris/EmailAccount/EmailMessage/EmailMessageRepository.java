package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, Long>, JpaSpecificationExecutor<EmailMessage> {

    Optional<EmailMessage> findByEmailAccountIdAndMessageId(Long emailAccountId, String messageId);

    /**
     * Used by ResendWebhookService when a delivery/bounce/complaint event
     * arrives — the webhook's {@code email_id} field is what we persisted
     * as {@code resend_email_id} at send time.
     */
    Optional<EmailMessage> findByResendEmailId(String resendEmailId);

    List<EmailMessage> findByEmailAccountIdAndThreadIdOrderBySentAtAsc(Long emailAccountId, String threadId);

    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.folder.id = :folderId AND m.isRead = false")
    long countUnreadByFolderId(@Param("folderId") Long folderId);

    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.folder.id = :folderId")
    long countByFolderId(@Param("folderId") Long folderId);

    List<EmailMessage> findBySentAtBeforeAndFolderEmailAccountId(LocalDateTime before, Long accountId);

    @Query("SELECT m FROM EmailMessage m WHERE m.folder.type = 'TRASH' AND m.updatedAt < :before")
    List<EmailMessage> findTrashOlderThan(@Param("before") LocalDateTime before);

    @Query("SELECT m FROM EmailMessage m JOIN m.labels l WHERE l.id = :labelId")
    List<EmailMessage> findAllByLabelsId(@Param("labelId") Long labelId);

    @Query("SELECT m FROM EmailMessage m WHERE m.snoozeUntil IS NOT NULL AND m.snoozeUntil <= :now")
    List<EmailMessage> findSnoozedDueBy(@Param("now") LocalDateTime now);

    /**
     * Incoming mail since a date, newest first — what a reply hunt reads.
     *
     * Drafts and our own sent copies are excluded: a request cannot be answered by the message that
     * asked it, and matching one to the other would close every ask the moment it was made.
     */
    @Query("""
        select m from EmailMessage m
        where m.receivedAt is not null and m.receivedAt >= :since
          and (m.isDraft is null or m.isDraft = false)
          and (m.folder is null or lower(m.folder.name) <> 'sent')
        order by m.receivedAt desc
        """)
    List<EmailMessage> findReceivedSince(@Param("since") LocalDateTime since);

    /**
     * Batched COUNT(*) grouped by threadId for a given account, restricted
     * to a set of threadIds. Used to fill in threadCount on list DTOs
     * without N+1.
     */
    @Query("""
        SELECT m.threadId, COUNT(m)
          FROM EmailMessage m
         WHERE m.emailAccount.id = :accountId AND m.threadId IN :threadIds
         GROUP BY m.threadId
        """)
    List<Object[]> countByThreadIds(@Param("accountId") Long accountId,
                                    @Param("threadIds") List<String> threadIds);

    // ========================
    // NAVIGATION QUERIES (circular next/previous within account+folder)
    // ========================

    @Query("SELECT m.id FROM EmailMessage m WHERE m.emailAccount.id = :accountId AND m.folder.id = :folderId AND m.id > :currentId ORDER BY m.id ASC LIMIT 1")
    Optional<Long> findNextIdInFolder(@Param("accountId") Long accountId, @Param("folderId") Long folderId, @Param("currentId") Long currentId);

    @Query("SELECT m.id FROM EmailMessage m WHERE m.emailAccount.id = :accountId AND m.folder.id = :folderId AND m.id < :currentId ORDER BY m.id DESC LIMIT 1")
    Optional<Long> findPreviousIdInFolder(@Param("accountId") Long accountId, @Param("folderId") Long folderId, @Param("currentId") Long currentId);

    @Query("SELECT m.id FROM EmailMessage m WHERE m.emailAccount.id = :accountId AND m.folder.id = :folderId ORDER BY m.id ASC LIMIT 1")
    Optional<Long> findFirstIdInFolder(@Param("accountId") Long accountId, @Param("folderId") Long folderId);

    @Query("SELECT m.id FROM EmailMessage m WHERE m.emailAccount.id = :accountId AND m.folder.id = :folderId ORDER BY m.id DESC LIMIT 1")
    Optional<Long> findLastIdInFolder(@Param("accountId") Long accountId, @Param("folderId") Long folderId);
}
