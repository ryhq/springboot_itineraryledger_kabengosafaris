package com.itineraryledger.kabengosafaris.ContactMessage.Specifications;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.itineraryledger.kabengosafaris.Attribution.AcquisitionChannel;
import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;

import lombok.Data;

/**
 * Everything a caller can narrow the message list by, in one object.
 *
 * The rows, the stat cards and the record walk are all built from this, so a card
 * cannot report a figure the table would contradict. Bound from the query string
 * with {@code @ModelAttribute}, so every parameter the old signature took is
 * still spelled the same on the wire.
 */
@Data
public class ContactMessageFilter {

    /** Free text across the name, the email, the subject and the message. */
    private String keyword;

    private String email;
    private String subject;

    private ContactMessageStatus status;
    private List<ContactMessageStatus> statuses;

    /** Obfuscated id, as the list page sends it. */
    private String customerId;

    /**
     * What needs doing.
     *
     * unread - nobody has opened it. unanswered - read but not replied to, which
     * is the one that gets forgotten. stale - unanswered and more than two days
     * old. known - from somebody already on our books, which changes how it reads.
     */
    private List<String> queues;

    /* Where it came from. Same dimensions, same names as the inquiry list. */
    private AcquisitionChannel channel;
    private List<AcquisitionChannel> channels;
    private List<String> campaigns;
    private List<String> sources;

    /**
     * Whether the click was bought, and whether anything was recorded at all.
     *
     * <p>A dimension of its own rather than another queue: the queues are OR'd, so "not replied
     * to" plus "came from an ad" would return the union and answer neither question.
     */
    private List<String> tracking;

    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;

    public List<ContactMessageStatus> allStatuses() {
        List<ContactMessageStatus> out = new ArrayList<>();
        if (statuses != null) statuses.stream().filter(Objects::nonNull).forEach(out::add);
        if (status != null && !out.contains(status)) out.add(status);
        return out;
    }

    public boolean wants(String queue) {
        return queues != null && queues.contains(queue);
    }
}
