package com.itineraryledger.kabengosafaris.Testimony.DTOs;

import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTestimonyDTO {

    private String authorName;
    private String authorTitle;
    private String authorCountry;
    private String authorEmail;
    private String message;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    private TestimonySource source;
    private LocalDate reviewDate;
    private Boolean isVerifiedBooking;
    private Boolean isApproved;
    private Boolean isFeatured;
    private Boolean isActive;
    private Integer displayOrder;
    private String sentimentTags;
    private String customerId;
    private String safariId;
}
