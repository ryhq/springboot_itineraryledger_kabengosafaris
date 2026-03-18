package com.itineraryledger.kabengosafaris.BookingInquiry.DTOs;

import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingInquiryDTO {

    private InquiryStatus status;
    private String adminNotes;
}
