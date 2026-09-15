package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

import com.itineraryledger.kabengosafaris.Attribution.AcquisitionChannel;

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
    /*
     * Only the three the table needs. The full arrival is on the record; a list row
     * that carried every tag would be paying for fifteen columns to render one.
     */
    private AcquisitionChannel channel;
    private String channelDisplayName;
    private String campaign;

    private LocalDateTime createdAt;
}
