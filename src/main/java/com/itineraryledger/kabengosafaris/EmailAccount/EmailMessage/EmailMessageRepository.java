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

    List<EmailMessage> findByEmailAccountIdAndThreadIdOrderBySentAtAsc(Long emailAccountId, String threadId);

    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.folder.id = :folderId AND m.isRead = false")
    long countUnreadByFolderId(@Param("folderId") Long folderId);

    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.folder.id = :folderId")
    long countByFolderId(@Param("folderId") Long folderId);

    List<EmailMessage> findBySentAtBeforeAndFolderEmailAccountId(LocalDateTime before, Long accountId);

    @Query("SELECT m FROM EmailMessage m WHERE m.folder.type = 'TRASH' AND m.updatedAt < :before")
    List<EmailMessage> findTrashOlderThan(@Param("before") LocalDateTime before);

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
