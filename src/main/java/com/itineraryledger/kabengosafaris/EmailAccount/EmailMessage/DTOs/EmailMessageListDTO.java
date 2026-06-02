package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailMessageListDTO {
    private String id;
    private String fromAddress;
    private String fromName;
    private String toAddresses;
    private String subject;
    private String snippet;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean isFlagged;
    private Boolean isDraft;
    private Boolean hasAttachments;
    private Integer attachmentCount;
    private LocalDateTime sentAt;
    private LocalDateTime snoozeUntil;
    private String threadId;
    /** Count of messages in this thread. Only set when the row represents
     *  the thread head (see §2 in EMAIL_INBOX_API.md). */
    private Integer threadCount;
    /** Label ids attached to this message. Populated once §1 is wired. */
    private List<String> labels;
    /**
     * For outgoing Resend-sent messages, the latest delivery state reported
     * via webhook: SENT / DELIVERY_DELAYED / DELIVERED / BOUNCED / COMPLAINED.
     * Null for inbound / IMAP-fetched messages. Frontend renders a chip
     * from this field.
     */
    private String deliveryStatus;
    private LocalDateTime deliveredAt;
    private LocalDateTime bouncedAt;
}
