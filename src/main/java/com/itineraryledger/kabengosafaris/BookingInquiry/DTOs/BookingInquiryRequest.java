package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingInquiryRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @Size(max = 50)
    private String phone;

    @Size(max = 100)
    private String country;

    @Min(value = 1, message = "At least 1 adult is required")
    private Integer adults = 1;

    @Min(0)
    private Integer children = 0;

    private String preferredStartDate;
    private String preferredEndDate;

    private String budgetCategory;
    private String tripType;

    /** Experiences from planner step 1 (e.g. ["SAFARI","GREAT_MIGRATION"]). */
    private List<String> interests;

    /** Preferred trip length in days from the planner. */
    private Integer preferredDurationDays;

    /** Destination park identifiers (obfuscated id or slug) from the "Where" step. */
    private List<String> destinationParkIds;

    private String specialRequests;
    private String message;

    private String safariIdentifier;

    private String locale = "en";
}
