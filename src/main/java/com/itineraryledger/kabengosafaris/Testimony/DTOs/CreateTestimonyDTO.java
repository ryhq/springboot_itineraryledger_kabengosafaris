package com.itineraryledger.kabengosafaris.Testimony.DTOs;

import java.time.LocalDate;

import com.itineraryledger.kabengosafaris.Testimony.Enums.TestimonySource;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestimonyDTO {

    @NotBlank(message = "Author name is required")
    private String authorName;

    private String authorTitle;

    private String authorCountry;

    private String authorEmail;

    @NotBlank(message = "Message is required")
    private String message;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @NotNull(message = "Source is required")
    private TestimonySource source;

    private LocalDate reviewDate;

    @Builder.Default
    private Boolean isVerifiedBooking = false;

    @Builder.Default
    private Boolean isApproved = false;

    @Builder.Default
    private Boolean isFeatured = false;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private Integer displayOrder = 0;

    private String sentimentTags;

    private String customerId;

    private String safariId;
}
