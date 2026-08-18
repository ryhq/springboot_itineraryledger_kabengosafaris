package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.CloseAvailabilityRequestDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.CreateAvailabilityRequestDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.LinkReplyDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services.AvailabilityLetterService;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services.AvailabilityRequestListService;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services.AvailabilityRequestService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Availability requests, read and written where they belong: under the safari they were asked for.
 *
 * Permissions are their own entity rather than borrowed from SAFARI: writing to suppliers is a
 * different job from editing a trip, and a name the catalogue does not contain 403s everybody —
 * superadmin included — so PERM_*_AVAILABILITY_REQUEST goes into entities.json in the same change.
 */
@RestController
@Validated
@RequiredArgsConstructor
public class AvailabilityRequestController {

    private final AvailabilityRequestService availabilityRequestService;
    private final AvailabilityLetterService availabilityLetterService;
    private final AvailabilityRequestListService availabilityRequestListService;

    @PostMapping("/api/safaris/{safariId}/availability-requests")
    @PreAuthorize("hasAuthority('PERM_CREATE_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> create(
            @PathVariable("safariId") String safariId,
            @Valid @RequestBody CreateAvailabilityRequestDTO body) {
        return availabilityRequestService.create(safariId, body);
    }

    /**
     * The letter, ready to send: subject, HTML, To and Cc.
     *
     * A read, so it is a GET with the nights in the query — nothing is written until the mail has
     * actually gone. Rendered from the AVAILABILITY_REQUEST template, which the office owns.
     */
    @GetMapping("/api/safaris/{safariId}/availability-requests/letter")
    @PreAuthorize("hasAuthority('PERM_CREATE_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> letter(
            @PathVariable("safariId") String safariId,
            @RequestParam String accommodationId,
            @RequestParam List<String> stayIds) {
        return availabilityLetterService.preview(safariId, accommodationId, stayIds);
    }

    @GetMapping("/api/safaris/{safariId}/availability-requests")
    @PreAuthorize("hasAuthority('PERM_READ_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> list(@PathVariable("safariId") String safariId) {
        return availabilityRequestService.list(safariId);
    }

    /** One row per night asked about — what the day screen draws its chips from. */
    @GetMapping("/api/safaris/{safariId}/availability-request-coverage")
    @PreAuthorize("hasAuthority('PERM_READ_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> coverage(@PathVariable("safariId") String safariId) {
        return availabilityRequestService.coverage(safariId);
    }

    /**
     * The chase list: every ask, across every trip.
     *
     * Flat rather than under a safari, because "who owes us an answer?" is not a question about one
     * trip. Sorted by chase date ascending, so the longest wait is first.
     */
    @GetMapping("/api/availability-requests")
    @PreAuthorize("hasAuthority('PERM_READ_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> listAll(
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Boolean chaseDue,
            @RequestParam(required = false) Boolean awaiting,
            @RequestParam(required = false) Boolean repliedUndecided,
            @RequestParam(required = false) String safariId,
            @RequestParam(required = false) String accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentBefore,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean includeStats,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        return availabilityRequestListService.list(statuses, chaseDue, awaiting, repliedUndecided,
            safariId, accommodationId, sentAfter, sentBefore, keyword, includeStats,
            page, size, sortBy, sortDirection);
    }

    /** One request, with the arrows walking the same filtered set the list showed. */
    @GetMapping("/api/availability-requests/{requestId}")
    @PreAuthorize("hasAuthority('PERM_READ_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> getOne(
            @PathVariable("requestId") String requestId,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(required = false) Boolean chaseDue,
            @RequestParam(required = false) Boolean awaiting,
            @RequestParam(required = false) Boolean repliedUndecided,
            @RequestParam(required = false) String safariId,
            @RequestParam(required = false) String accommodationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentBefore,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        return availabilityRequestListService.getOne(requestId, statuses, chaseDue, awaiting,
            repliedUndecided, safariId, accommodationId, sentAfter, sentBefore, keyword,
            sortBy, sortDirection);
    }

    @PostMapping("/api/availability-requests/{requestId}/close")
    @PreAuthorize("hasAuthority('PERM_UPDATE_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> close(
            @PathVariable("requestId") String requestId,
            @Valid @RequestBody CloseAvailabilityRequestDTO body) {
        return availabilityRequestService.close(requestId, body);
    }

    @PostMapping("/api/availability-requests/{requestId}/link-reply")
    @PreAuthorize("hasAuthority('PERM_UPDATE_AVAILABILITY_REQUEST')")
    public ResponseEntity<ApiResponse<?>> linkReply(
            @PathVariable("requestId") String requestId,
            @Valid @RequestBody LinkReplyDTO body) {
        return availabilityRequestService.linkReply(requestId, body);
    }
}
