package com.itineraryledger.kabengosafaris.Customer.DTOs.CustomerNoteDTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NoteType;
import com.itineraryledger.kabengosafaris.Customer.Entity.CustomerNote.NotePriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CustomerNoteDTO - Response DTO for customer note data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerNoteDTO {

    private String id; // Encoded ID
    private String customerId; // Encoded customer ID
    private String customerDisplayName;

    // Note details
    private NoteType noteType;
    private String noteTypeDisplayName;
    private String noteTypeDescription;
    private String subject;
    private String content;

    // Follow-up
    private LocalDateTime followUpDate;
    private Boolean followUpCompleted;
    private LocalDateTime followUpCompletedAt;
    private Boolean isFollowUpOverdue;

    // Visibility & Priority
    private Boolean isPinned;
    private Boolean isPrivate;
    private NotePriority priority;
    private String priorityDisplayName;
    private String priorityDescription;

    // Metadata
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
