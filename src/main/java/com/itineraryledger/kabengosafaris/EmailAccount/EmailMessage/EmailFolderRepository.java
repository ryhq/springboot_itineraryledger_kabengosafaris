package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolder;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;

@Repository
public interface EmailFolderRepository extends JpaRepository<EmailFolder, Long>, JpaSpecificationExecutor<EmailFolder> {

    List<EmailFolder> findByEmailAccountIdOrderByTypeAsc(Long emailAccountId);

    Optional<EmailFolder> findByEmailAccountIdAndType(Long emailAccountId, EmailFolderType type);

    Optional<EmailFolder> findByEmailAccountIdAndName(Long emailAccountId, String name);

    boolean existsByEmailAccountIdAndName(Long emailAccountId, String name);

    List<EmailFolder> findByEmailAccountIdAndIsSystemTrue(Long emailAccountId);

    @Modifying
    @Query("UPDATE EmailFolder f SET f.messageCount = f.messageCount + :delta WHERE f.id = :folderId")
    void incrementMessageCount(@Param("folderId") Long folderId, @Param("delta") int delta);

    @Modifying
    @Query("UPDATE EmailFolder f SET f.unreadCount = f.unreadCount + :delta WHERE f.id = :folderId")
    void incrementUnreadCount(@Param("folderId") Long folderId, @Param("delta") int delta);
}
