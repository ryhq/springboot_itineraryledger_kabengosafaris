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
}
