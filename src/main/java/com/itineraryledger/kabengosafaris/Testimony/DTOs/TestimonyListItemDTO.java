package com.itineraryledger.kabengosafaris.Testimony.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestimonyListItemDTO {

    private String id;
    private String authorName;
    private String authorTitle;
    private String authorCountry;
    private String authorEmail;
    private String message;
    private Integer rating;
    private TestimonySource source;
    private String sourceDisplayName;
    private Boolean isApproved;
    private Boolean isFeatured;
    private Boolean isVerifiedBooking;
    private Boolean isActive;
    private Integer displayOrder;
    private LocalDate reviewDate;
    private String customerName;
    private String safariName;
    private String primaryImageUrl;
    private Long imageCount;
    private LocalDateTime createdAt;
}
