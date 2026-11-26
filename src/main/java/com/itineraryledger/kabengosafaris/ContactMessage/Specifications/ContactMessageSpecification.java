package com.itineraryledger.kabengosafaris.ContactMessage.Specifications;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Attribution.AcquisitionChannel;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessage;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;

public class ContactMessageSpecification {

    public static Specification<ContactMessage> byStatus(ContactMessageStatus status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<ContactMessage> byEmail(String email) {
        return (root, query, cb) -> {
            if (email == null || email.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ContactMessage> bySubject(String subject) {
        return (root, query, cb) -> {
            if (subject == null || subject.trim().isEmpty()) return cb.conjunction();
            return cb.like(cb.lower(root.get("subject")), "%" + subject.toLowerCase().trim() + "%");
        };
    }

    public static Specification<ContactMessage> createdAfter(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<ContactMessage> createdBefore(LocalDateTime dateTime) {
        return (root, query, cb) -> dateTime == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("createdAt"), dateTime);
    }

    public static Specification<ContactMessage> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) return cb.conjunction();
            String pattern = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("subject")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("message")), pattern)
            );
        };
    }

    public static Specification<ContactMessage> byStatuses(
            java.util.List<ContactMessageStatus> statuses) {
        return (root, query, cb) -> statuses == null || statuses.isEmpty()
            ? cb.conjunction()
            : root.get("status").in(statuses);
    }

    public static Specification<ContactMessage> byCustomerId(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("id"), customerId);
    }

    /* The work queues - what the list is actually for. */

    /** Nobody has opened it yet. */
    public static Specification<ContactMessage> unread() {
        return (root, query, cb) -> cb.equal(root.get("status"), ContactMessageStatus.NEW);
    }

    /**
     * Read, and still not replied to.
     *
     * The one that gets forgotten: somebody opened it, meant to come back, and
     * the message has been sitting in READ ever since.
     */
    public static Specification<ContactMessage> unanswered() {
        return (root, query, cb) -> root.get("status")
            .in(ContactMessageStatus.NEW, ContactMessageStatus.READ);
    }

    /** Unanswered and older than the window. */
    public static Specification<ContactMessage> staleFor(int days) {
        return (root, query, cb) -> cb.and(
            root.get("status").in(ContactMessageStatus.NEW, ContactMessageStatus.READ),
            cb.lessThan(root.get("createdAt"), LocalDateTime.now().minusDays(days)));
    }

    /** From somebody already on our books - it reads differently, so it is worth a card. */
    public static Specification<ContactMessage> fromKnownCustomer(boolean known) {
        return (root, query, cb) -> known
            ? cb.isNotNull(root.get("customer"))
            : cb.isNull(root.get("customer"));
    }

    /* ---------- where the message came from ---------- */

    /*
     * The arrival has been stored on every contact message since lead attribution shipped and
     * nothing has ever read it back: no field on the DTO, no dimension on the filter. So an ad
     * click that landed on the contact page instead of the planner was recorded and then
     * invisible, and the campaign that paid for it looked like it produced nothing.
     *
     * Identical rules to the inquiry side rather than a second opinion about them - a campaign
     * counted one way here and another way there is worse than not counting it at all.
     */

    public static Specification<ContactMessage> byChannel(AcquisitionChannel channel) {
        return (root, query, cb) -> channel == null
            ? cb.conjunction()
            : cb.equal(root.get("attribution").get("channel"), channel);
    }

    public static Specification<ContactMessage> byChannels(List<AcquisitionChannel> channels) {
        return (root, query, cb) -> channels == null || channels.isEmpty()
            ? cb.conjunction()
            : root.get("attribution").get("channel").in(channels);
    }

    /** Matched whole, so "serengeti-jul" is never folded into "serengeti-july-retarget". */
    public static Specification<ContactMessage> byCampaigns(List<String> campaigns) {
        return (root, query, cb) -> campaigns == null || campaigns.isEmpty()
            ? cb.conjunction()
            : root.get("attribution").get("campaign").in(campaigns);
    }

    public static Specification<ContactMessage> bySources(List<String> sources) {
        return (root, query, cb) -> sources == null || sources.isEmpty()
            ? cb.conjunction()
            : cb.lower(root.get("attribution").get("source")).in(
                sources.stream().map(v -> v.toLowerCase().trim()).toList());
    }

    public static Specification<ContactMessage> paidChannels() {
        List<AcquisitionChannel> paid = java.util.Arrays.stream(AcquisitionChannel.values())
            .filter(AcquisitionChannel::isPaid)
            .toList();
        return (root, query, cb) -> root.get("attribution").get("channel").in(paid);
    }

    /** No arrival at all: taken before this shipped, or a browser with storage blocked. */
    public static Specification<ContactMessage> untracked() {
        return (root, query, cb) -> cb.isNull(root.get("attribution").get("channel"));
    }

    /** An unknown value narrows nothing rather than erroring, so an old bookmark still lists. */
    public static Specification<ContactMessage> byTracking(List<String> tracking) {
        if (tracking == null || tracking.isEmpty()) return (root, query, cb) -> cb.conjunction();
        boolean paid = tracking.contains("paid");
        boolean untracked = tracking.contains("untracked");
        if (paid && untracked) return (root, query, cb) -> cb.conjunction();
        if (paid) return paidChannels();
        if (untracked) return untracked();
        return (root, query, cb) -> cb.conjunction();
    }
}
