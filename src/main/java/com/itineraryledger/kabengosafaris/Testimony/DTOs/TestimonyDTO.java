package com.itineraryledger.kabengosafaris.Testimony.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;
import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonyDTO {

    private String id;
    private String authorName;
    private String authorTitle;
    private String authorCountry;
    @Translatable private String message;
    private Integer rating;
    private String adminResponse;
    private LocalDateTime adminResponseDate;
    private LocalDate reviewDate;
    private TestimonySource source;
    private String sourceDisplayName;
    private Boolean isVerifiedBooking;
    private Boolean isApproved;
    private Boolean isFeatured;
    private Boolean isActive;
    private Integer displayOrder;
    private String sentimentTags;

    private String customerId;
    private String customerName;
    private String safariId;
    private String safariName;

    private String primaryImageUrl;
    private Long imageCount;

    private String createdById;
    private String createdByName;
    private String updatedById;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
