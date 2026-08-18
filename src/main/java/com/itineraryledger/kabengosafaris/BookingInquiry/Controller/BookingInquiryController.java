package com.itineraryledger.kabengosafaris.BookingInquiry.Controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.BookingInquiry.DTOs.UpdateBookingInquiryDTO;
import com.itineraryledger.kabengosafaris.BookingInquiry.Specifications.BookingInquiryFilter;
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
        @ModelAttribute BookingInquiryFilter filter,
        @RequestParam(required = false) Boolean includeStats,
        @RequestParam(required = false, defaultValue = "0") Integer pageNumber,
        @RequestParam(required = false, defaultValue = "20") Integer pageSize,
        /*
         * page/size are what every other list in this API is paged by, and what the panel sends.
         * Taking only pageNumber/pageSize meant this list was permanently on page one at twenty
         * rows: the pager moved, the rows did not. Whichever arrives wins, house names first.
         */
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/booking-inquiries - Listing inquiries with filters");
        return getService.listInquiries(filter, includeStats, page != null ? page : pageNumber, size != null ? size : pageSize, sortBy, sortDirection);
    }

    @GetMapping("/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_READ_BOOKING_INQUIRY')")
    public ResponseEntity<ApiResponse<?>> getInquiryById(
        @PathVariable String idObfuscated,
        // the list's filters and sort, so prev/next walks that same set
        @ModelAttribute BookingInquiryFilter filter,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/booking-inquiries/{} - Fetching inquiry by ID", idObfuscated);
        return getService.getInquiryById(idObfuscated, filter, sortBy, sortDirection);
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

    /**
     * POST /{id}/convert — turn the enquirer into a customer.
     *
     * This is the join between the website and everything downstream. Somebody
     * asked for a safari; until they are a customer they cannot be quoted, and
     * retyping their name and email into a new customer form is how the two
     * records end up disagreeing about the same person.
     *
     * Idempotent: an inquiry already converted returns the customer it made
     * rather than making a second one.
     */
    @PostMapping("/{idObfuscated}/convert")
    @PreAuthorize("hasAuthority('PERM_UPDATE_BOOKING_INQUIRY')")
    public ResponseEntity<ApiResponse<?>> convert(@PathVariable String idObfuscated) {
        log.info("POST /api/booking-inquiries/{}/convert", idObfuscated);
        return updateService.convertToCustomer(idObfuscated);
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
