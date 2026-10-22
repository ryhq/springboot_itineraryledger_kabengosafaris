package com.itineraryledger.kabengosafaris.Legal.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A notice as the panel and the websites read it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentDTO {

    private String id;
    private String documentType;
    private String documentTypeDisplayName;
    private String version;
    private String title;

    /** The source text, for editing. */
    private String body;

    /** The same text rendered to safe HTML, for displaying. Never round-tripped back in. */
    private String html;

    private LocalDate effectiveFrom;
    private Boolean published;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
