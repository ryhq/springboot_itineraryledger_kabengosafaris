package com.itineraryledger.kabengosafaris.BookingInquiry.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.UpdateBookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.Entity.InquiryStatus;
import com.itineraryledger.kabengosafaris.BookingInquiry.Services.BookingInquiryDeleteService;
import com.itineraryledger.kabengosafaris.BookingInquiry.Services.BookingInquiryGetService;
import com.itineraryledger.kabengosafaris.BookingInquiry.Services.BookingInquiryUpdateService;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.BudgetCategory;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.TripType;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/booking-inquiries")
@Slf4j
public class BookingInquiryController {

    private final BookingInquiryGetService getService;
    private final BookingInquiryUpdateService updateService;
    private final BookingInquiryDeleteService deleteService;

    @Autowired
    public BookingInquiryController(
        BookingInquiryGetService getService,
        BookingInquiryUpdateService updateService,
        BookingInquiryDeleteService deleteService
    ) {
        this.getService = getService;
        this.updateService = updateService;
        this.deleteService = deleteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_BOOKING_INQUIRY')")
    public ResponseEntity<ApiResponse<?>> listInquiries(
        @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
        @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestParam(required = false) InquiryStatus status,
        @RequestParam(required = false) BudgetCategory budgetCategory,
        @RequestParam(required = false) TripType tripType,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String country,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
        @RequestParam(required = false) String keyword
    ) {
        log.info("GET /api/booking-inquiries - Listing inquiries with filters");
        return getService.listInquiries(
            pageNumber, pageSize, sortBy, sortDirection,
            status, budgetCategory, tripType, email, country,
            createdAfter, createdBefore, keyword
        );
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_BOOKING_INQUIRY')")
    public ResponseEntity<ApiResponse<?>> getInquiryById(
        @PathVariable String idObfuscated
    ) {
        log.info("GET /api/booking-inquiries/{} - Fetching inquiry by ID", idObfuscated);
        return getService.getInquiryById(idObfuscated);
    }

    @PutMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BOOKING_INQUIRY')")
    public ResponseEntity<ApiResponse<?>> updateInquiry(
        @PathVariable String idObfuscated,
        @Valid @RequestBody UpdateBookingInquiryDTO updateDTO
    ) {
        log.info("PUT /api/booking-inquiries/{} - Updating inquiry", idObfuscated);
        return updateService.updateInquiry(idObfuscated, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_BOOKING_INQUIRY')")
    public ResponseEntity<ApiResponse<?>> deleteInquiries(
        @RequestBody List<String> idObfuscatedList
    ) {
        log.info("DELETE /api/booking-inquiries - Deleting {} inquiries", idObfuscatedList.size());
        return deleteService.deleteInquiries(idObfuscatedList);
    }
}
