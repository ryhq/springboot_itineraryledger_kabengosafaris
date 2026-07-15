package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

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
}
