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
public class ContactMessageListItemDTO {

    private String id;
    private String code;
    private String name;
    private String email;
    private String subject;
    private ContactMessageStatus status;
    private String statusDisplayName;
    private LocalDateTime createdAt;
}
