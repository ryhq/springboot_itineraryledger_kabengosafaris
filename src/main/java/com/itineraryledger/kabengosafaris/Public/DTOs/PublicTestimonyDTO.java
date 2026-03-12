package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Public-safe Testimony DTO.
 * Excludes: adminResponse, adminResponseDate, isApproved, isFeatured, isActive,
 * displayOrder, sentimentTags, customerId, customerName, safariId,
 * createdById, createdByName, updatedById, updatedByName, createdAt, updatedAt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicTestimonyDTO {

    private String id;
    private String authorName;
    private String authorTitle;
    private String authorCountry;
    @Translatable private String message;
    private Integer rating;
    private LocalDate reviewDate;
    private String sourceDisplayName;
    private Boolean isVerifiedBooking;
    private String safariName;
    private String primaryImageUrl;
    private Long imageCount;
}
