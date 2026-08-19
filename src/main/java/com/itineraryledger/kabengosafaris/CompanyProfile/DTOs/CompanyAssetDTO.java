package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One asset slot. The slot always appears, filled or not, because the page shows five slots and
 * an empty one is the thing the user needs to see.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyAssetDTO {
    private String assetKind;
    /** human label for the slot, e.g. "Logo (light mode)". */
    private String label;
    private String hint;
    private Boolean present;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    /** where to fetch it; null when the slot is empty. */
    private String url;
    /** false for an SVG in the email slot — Outlook does not render SVG. */
    private Boolean safeForEmail;
    private Boolean isActive;
    private LocalDateTime updatedAt;
}
