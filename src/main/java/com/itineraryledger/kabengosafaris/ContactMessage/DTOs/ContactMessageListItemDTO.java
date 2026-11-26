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
    /*
     * Three fields, not the whole arrival: a table needs a badge, the record needs the story.
     */
    private com.itineraryledger.kabengosafaris.Attribution.AcquisitionChannel channel;
    private String channelDisplayName;
    private String campaign;

    private LocalDateTime createdAt;
}
