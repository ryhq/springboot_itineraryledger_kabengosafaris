package com.itineraryledger.kabengosafaris.ContactMessage.DTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessageDTO {

    private String id;
    private String code;
    private String name;
    private String email;
    private String phone;
    private String subject;
    private String message;
    private ContactMessageStatus status;
    private String statusDisplayName;
    private String source;
    private String preferredLocale;
    private String customerId;
    private String customerName;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime respondedAt;

    /**
     * How they arrived.
     *
     * <p>It has been stored on every contact message since lead attribution shipped and read
     * back by nothing at all, so an ad click that landed here rather than on the planner was
     * recorded and then invisible - and the campaign that paid for it looked like it produced
     * nothing. Null for anything taken before that, so the panel can say "not recorded"
     * instead of quietly reporting it as direct traffic.
     */
    private com.itineraryledger.kabengosafaris.Attribution.AttributionDTO attribution;
}
