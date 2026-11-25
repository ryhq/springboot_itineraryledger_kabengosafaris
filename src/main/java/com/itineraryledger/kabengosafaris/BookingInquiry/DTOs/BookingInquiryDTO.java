package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

import com.itineraryledger.kabengosafaris.Attribution.AttributionDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripInterest;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingInquiryDTO {

    private String id;
    private String code;
    private String firstName;
    private String lastName;
    private String displayName;
    private String email;
    private String phone;
    private String country;
    private Integer adults;
    private Integer children;
    private Integer totalTravelers;
    private LocalDate preferredStartDate;
    private LocalDate preferredEndDate;
    private BudgetCategory budgetCategory;
    private String budgetCategoryDisplayName;
    private TripType tripType;
    private String tripTypeDisplayName;
    private Set<TripInterest> interests;
    private List<String> interestDisplayNames;
    private Integer preferredDurationDays;
    private List<String> destinationParkIds;
    private List<String> destinationParkNames;
    private String specialRequests;
    private String message;
    private InquiryStatus status;
    private String statusDisplayName;
    private String source;

    /**
     * How they actually got here. Null when nothing was recorded, which is every
     * inquiry taken before this shipped and everything entered by hand in the office
     * - the panel says so rather than reporting it as direct traffic.
     */
    private AttributionDTO attribution;
    private String preferredLocale;
    private String itineraryId;
    private String itineraryName;
    private String customerId;
    private String customerName;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime contactedAt;
    private LocalDateTime convertedAt;

    /**
     * When they finished the form, or null if they never did.
     *
     * <p>A null here is a real lead with half an answer, not a broken row: the form saves as
     * it is typed, so somebody who gave their name and their email and then stopped is
     * recorded rather than lost. It is deliberately not a sixth {@code InquiryStatus} - a
     * half-written lead is not a stage of the sale.
     */
    private LocalDateTime submittedAt;

    /** Which step they reached, so an abandoned lead says where it was lost. */
    private Integer lastStep;
    private LocalDateTime lastStepAt;
}
