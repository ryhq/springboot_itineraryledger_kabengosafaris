package com.itineraryledger.kabengosafaris.BookingInquiry.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.BookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.UpdateBookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.BookingInquiry;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.BookingInquiry.Repository.BookingInquiryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class BookingInquiryUpdateService {

    private final BookingInquiryRepository repository;
    private final IdObfuscator idObfuscator;
    private final BookingInquiryGetService getService;

    @Autowired
    public BookingInquiryUpdateService(
        BookingInquiryRepository repository,
        IdObfuscator idObfuscator,
        BookingInquiryGetService getService
    ) {
        this.repository = repository;
        this.idObfuscator = idObfuscator;
        this.getService = getService;
    }

    @AuditLogAnnotation(action = "UPDATE_BOOKING_INQUIRY", description = "Updating booking inquiry", entityType = "BookingInquiry", entityIdParamName = "idObfuscated")
    public ResponseEntity<ApiResponse<?>> updateInquiry(String idObfuscated, UpdateBookingInquiryDTO updateDTO) {
        log.info("Updating booking inquiry with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode inquiry ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid inquiry ID", "INVALID_INQUIRY_ID")
                );
            }

            BookingInquiry inquiry = repository.findById(id).orElse(null);
            if (inquiry == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Booking inquiry not found", "INQUIRY_NOT_FOUND")
                );
            }

            if (updateDTO.getAdminNotes() != null) inquiry.setAdminNotes(updateDTO.getAdminNotes());

            if (updateDTO.getStatus() != null) {
                InquiryStatus oldStatus = inquiry.getStatus();
                // the status arrives as a String so a blank can CLEAR it; parse it once
                InquiryStatus requestedStatus = updateDTO.getStatus().isBlank()
                    ? null
                    : InquiryStatus.valueOf(updateDTO.getStatus().trim());
                inquiry.setStatus(requestedStatus);

                if (requestedStatus == InquiryStatus.CONTACTED && oldStatus != InquiryStatus.CONTACTED && inquiry.getContactedAt() == null) {
                    inquiry.setContactedAt(LocalDateTime.now());
                }
                if (requestedStatus == InquiryStatus.CONVERTED && oldStatus != InquiryStatus.CONVERTED && inquiry.getConvertedAt() == null) {
                    inquiry.setConvertedAt(LocalDateTime.now());
                }
            }

            inquiry = repository.save(inquiry);

            BookingInquiryDTO dto = getService.convertToDTO(inquiry);

            log.info("Booking inquiry updated successfully: {}", inquiry.getCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Booking inquiry updated successfully", dto)
            );

        } catch (Exception e) {
            log.error("Error updating booking inquiry", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update booking inquiry", "INQUIRY_UPDATE_FAILED")
            );
        }
    }
}
