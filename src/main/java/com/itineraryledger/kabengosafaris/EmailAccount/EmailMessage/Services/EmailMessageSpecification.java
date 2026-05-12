package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;

public class EmailMessageSpecification {

    public static Specification<EmailMessage> forAccount(Long accountId) {
        return (root, query, cb) -> cb.equal(root.get("emailAccount").get("id"), accountId);
    }

    public static Specification<EmailMessage> inFolder(Long folderId) {
        return (root, query, cb) -> cb.equal(root.get("folder").get("id"), folderId);
    }

    public static Specification<EmailMessage> isRead(Boolean read) {
        return (root, query, cb) -> cb.equal(root.get("isRead"), read);
    }

    public static Specification<EmailMessage> isStarred(Boolean starred) {
        return (root, query, cb) -> cb.equal(root.get("isStarred"), starred);
    }

    public static Specification<EmailMessage> isFlagged(Boolean flagged) {
        return (root, query, cb) -> cb.equal(root.get("isFlagged"), flagged);
    }

    public static Specification<EmailMessage> hasAttachments(Boolean has) {
        return (root, query, cb) -> cb.equal(root.get("hasAttachments"), has);
    }

    /**
     * Hide messages that have a future snoozeUntil (so they disappear from
     * inbox-style queries). Messages with snoozeUntil == null also pass.
     */
    public static Specification<EmailMessage> notSnoozed() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("snoozeUntil")),
            cb.lessThanOrEqualTo(root.get("snoozeUntil"), java.time.LocalDateTime.now())
        );
    }

    public static Specification<EmailMessage> subjectLike(String subject) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("subject")), "%" + subject.toLowerCase() + "%");
    }

    public static Specification<EmailMessage> fromAddressLike(String from) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("fromAddress")), "%" + from.toLowerCase() + "%");
    }

    public static Specification<EmailMessage> toAddressLike(String to) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("toAddresses")), "%" + to.toLowerCase() + "%");
    }

    public static Specification<EmailMessage> sentAfter(LocalDateTime date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("sentAt"), date);
    }

    public static Specification<EmailMessage> sentBefore(LocalDateTime date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("sentAt"), date);
    }

    public static Specification<EmailMessage> searchAll(String query) {
        String pattern = "%" + query.toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
            cb.like(cb.lower(root.get("subject")), pattern),
            cb.like(cb.lower(root.get("fromAddress")), pattern),
            cb.like(cb.lower(root.get("toAddresses")), pattern),
            cb.like(cb.lower(root.get("snippet")), pattern)
        );
    }
}
