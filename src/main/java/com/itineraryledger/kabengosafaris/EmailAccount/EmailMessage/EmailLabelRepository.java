package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabel;

@Repository
public interface EmailLabelRepository extends JpaRepository<EmailLabel, Long> {

    List<EmailLabel> findByEmailAccountIdOrderByIsSystemDescNameAsc(Long emailAccountId);

    Optional<EmailLabel> findByEmailAccountIdAndName(Long emailAccountId, String name);

    boolean existsByEmailAccountIdAndName(Long emailAccountId, String name);

    /**
     * Per-label message count for the rail badges. Excludes drafts and
     * trashed messages so the count matches what the user actually sees.
     */
    @Query(value = """
        SELECT l.id, COUNT(m.id)
          FROM email_labels l
          LEFT JOIN email_message_labels ml ON ml.email_label_id = l.id
          LEFT JOIN email_messages m ON m.id = ml.email_message_id
            AND m.is_draft = false
            AND (m.snooze_until IS NULL OR m.snooze_until <= CURRENT_TIMESTAMP)
         WHERE l.email_account_id = :accountId
         GROUP BY l.id
        """, nativeQuery = true)
    List<Object[]> countMessagesPerLabel(@Param("accountId") Long accountId);
}
