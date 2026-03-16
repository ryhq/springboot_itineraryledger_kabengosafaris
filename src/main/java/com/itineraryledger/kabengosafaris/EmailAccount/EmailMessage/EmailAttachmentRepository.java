package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailAttachment;

@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {

    List<EmailAttachment> findByEmailMessageId(Long emailMessageId);

    void deleteByEmailMessageId(Long emailMessageId);
}
