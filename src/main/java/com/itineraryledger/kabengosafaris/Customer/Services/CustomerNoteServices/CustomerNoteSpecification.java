package com.itineraryledger.kabengosafaris.Customer.Services.CustomerNoteServices;

import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * CustomerNoteSpecification - Provides reusable Specification objects for filtering CustomerNote entities
 */
public class CustomerNoteSpecification {

    /**
     * Filter by customer ID
     */
    public static Specification<CustomerNote> hasCustomerId(Long customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("customer").get("id"), customerId);
        };
    }

    /**
     * Filter by note type (enum)
     */
    public static Specification<CustomerNote> hasNoteType(NoteType noteType) {
        return (root, query, cb) -> {
            if (noteType == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("noteType"), noteType);
        };
    }

    /**
     * Filter by subject (case-insensitive partial match)
     */
    public static Specification<CustomerNote> subjectLike(String subject) {
        return (root, query, cb) -> {
            if (subject == null || subject.isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("subject")), "%" + subject.toLowerCase() + "%");
        };
    }

    /**
     * Filter by pinned status
     */
    public static Specification<CustomerNote> isPinned(Boolean isPinned) {
        return (root, query, cb) -> {
            if (isPinned == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPinned"), isPinned);
        };
    }

    /**
     * Filter by private status
     */
    public static Specification<CustomerNote> isPrivate(Boolean isPrivate) {
        return (root, query, cb) -> {
            if (isPrivate == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("isPrivate"), isPrivate);
        };
    }

    /**
     * Filter by priority (enum)
     */
    public static Specification<CustomerNote> hasPriority(NotePriority priority) {
        return (root, query, cb) -> {
            if (priority == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("priority"), priority);
        };
    }

    /**
     * Filter by follow-up completed status
     */
    public static Specification<CustomerNote> isFollowUpCompleted(Boolean followUpCompleted) {
        return (root, query, cb) -> {
            if (followUpCompleted == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("followUpCompleted"), followUpCompleted);
        };
    }

    /**
     * Filter notes with pending follow-ups (has follow-up date and not completed)
     */
    public static Specification<CustomerNote> hasPendingFollowUp() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("followUpDate")),
            cb.equal(root.get("followUpCompleted"), false)
        );
    }

    /**
     * Filter notes with overdue follow-ups
     */
    public static Specification<CustomerNote> hasOverdueFollowUp() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("followUpDate")),
            cb.equal(root.get("followUpCompleted"), false),
            cb.lessThan(root.get("followUpDate"), LocalDateTime.now())
        );
    }

    /**
     * Search across subject and content fields
     */
    public static Specification<CustomerNote> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("subject")), likePattern),
                cb.like(cb.lower(root.get("content")), likePattern)
            );
        };
    }
}
