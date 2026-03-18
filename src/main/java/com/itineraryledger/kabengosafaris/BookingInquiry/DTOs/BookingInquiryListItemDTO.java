package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

import java.time.LocalDateTime;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingInquiryListItemDTO {

    private String id;
    private String code;
    private String displayName;
    private String email;
    private String country;
    private Integer totalTravelers;
    private String budgetCategoryDisplayName;
    private String tripTypeDisplayName;
    private InquiryStatus status;
    private String statusDisplayName;
    private String itineraryName;
    private LocalDateTime createdAt;
}
