package com.itineraryledger.kabengosafaris.ContactMessage.DTOs;

import com.itineraryledger.kabengosafaris.ContactMessage.Entity.ContactMessageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateContactMessageDTO {

    private ContactMessageStatus status;
    private String adminNotes;
}
