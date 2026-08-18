package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.AvailabilityRequestDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.CloseAvailabilityRequestDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.CreateAvailabilityRequestDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.DTOs.LinkReplyDTO;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequestStay;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Repository.AvailabilityRequestRepository;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import com.itineraryledger.kabengosafaris.User.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Availability requests: who has been asked, about which nights, and what came back.
 *
 * The rules that matter live here rather than in the browser, because they have to hold however the
 * ask was made:
 *
 *  - covered stays go to PENDING in the SAME transaction as the record, so a stay can never say
 *    "nothing asked" while a request says otherwise;
 *  - a chase falls due three WORKING days after sending — a Friday request is not overdue on Monday;
 *  - confirming a stay closes the ask that was waiting on it, since nobody goes back to tidy up.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvailabilityRequestService {

    private final AvailabilityRequestRepository requestRepository;
    private final SafariRepository safariRepository;
    private final AccommodationRepository accommodationRepository;
    private final SafariDayAccommodationRepository stayRepository;
    private final IdObfuscator idObfuscator;
    private final ObjectMapper objectMapper;

    /** Three working days, counted as such: Saturday and Sunday are not chasing days. */
    private static final int CHASE_WORKING_DAYS = 3;

    static LocalDateTime chaseDueFrom(LocalDateTime sentAt) {
        LocalDateTime due = sentAt;
        int added = 0;
        while (added < CHASE_WORKING_DAYS) {
            due = due.plusDays(1);
            DayOfWeek day = due.getDayOfWeek();
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) added++;
        }
        return due;
    }

    /* ------------------------------------------------------------- writing */

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String safariIdObfuscated, CreateAvailabilityRequestDTO dto) {
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND"));
            }

            Long accommodationId = idObfuscator.decodeId(dto.getAccommodationId());
            Accommodation accommodation = accommodationId == null
                ? null : accommodationRepository.findById(accommodationId).orElse(null);
            if (accommodation == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }

            /*
             * Every stay must belong to THIS safari. Without the check, an id from another trip
             * would attach a request to nights nobody asked about — and the guard would then hide a
             * property that had never been written to.
             */
            List<SafariDayAccommodation> stays = new ArrayList<>();
            for (String stayIdObfuscated : dto.getStayIds()) {
                Long stayId = idObfuscator.decodeId(stayIdObfuscated);
                SafariDayAccommodation stay = stayId == null ? null : stayRepository.findById(stayId).orElse(null);
                if (stay == null || stay.getSafariDay() == null
                    || stay.getSafariDay().getSafari() == null
                    || !stay.getSafariDay().getSafari().getId().equals(safariId)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400,
                        "One of those nights does not belong to this safari", "STAY_NOT_ON_SAFARI"));
                }
                stays.add(stay);
            }

            LocalDateTime now = LocalDateTime.now();
            User user = currentUser();

            AvailabilityRequest request = AvailabilityRequest.builder()
                .safari(safari)
                .accommodation(accommodation)
                .status(AvailabilityRequest.Status.SENT)
                .emailMessageId(decodeOrNull(dto.getEmailMessageId()))
                .emailAccountId(decodeOrNull(dto.getEmailAccountId()))
                .toAddress(dto.getToAddress())
                .ccAddresses(writeList(dto.getCcAddresses()))
                .bccAddresses(writeList(dto.getBccAddresses()))
                .subject(dto.getSubject())
                .sentAt(now)
                .sentByUserId(user != null ? user.getId() : null)
                .chaseDueAt(chaseDueFrom(now))
                .notes(dto.getNotes())
                .build();

            for (SafariDayAccommodation stay : stays) {
                request.getStays().add(AvailabilityRequestStay.builder()
                    .availabilityRequest(request)
                    .stay(stay)
                    .safariDayId(stay.getSafariDay().getId())
                    .dayNumber(stay.getSafariDay().getDayNumber())
                    .nightDate(stay.getSafariDay().getActualDate())
                    .build());
            }

            AvailabilityRequest saved = requestRepository.save(request);

            /*
             * The nights now say a request is out. Done here, in this transaction, rather than by
             * the caller patching each stay in turn: half-marked nights after a dropped connection
             * are exactly the state that makes somebody ask a lodge twice.
             */
            for (SafariDayAccommodation stay : stays) {
                if (stay.getBookingStatus() == null
                    || stay.getBookingStatus() == SafariDayAccommodation.BookingStatus.CANCELLED) {
                    stay.setBookingStatus(SafariDayAccommodation.BookingStatus.PENDING);
                } else if (stay.getBookingStatus() != SafariDayAccommodation.BookingStatus.CONFIRMED) {
                    stay.setBookingStatus(SafariDayAccommodation.BookingStatus.PENDING);
                }
            }
            stayRepository.saveAll(stays);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Availability request recorded", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error recording availability request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to record the availability request", "AVAILABILITY_REQUEST_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> close(String requestIdObfuscated, CloseAvailabilityRequestDTO dto) {
        try {
            AvailabilityRequest request = find(requestIdObfuscated);
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Availability request not found", "AVAILABILITY_REQUEST_NOT_FOUND"));
            }
            AvailabilityRequest.ClosedReason reason;
            try {
                reason = AvailabilityRequest.ClosedReason.valueOf(dto.getReason().trim().toUpperCase());
            } catch (IllegalArgumentException bad) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "A reason must be one of CONFIRMED, DECLINED, SUPERSEDED, CANCELLED",
                    "AVAILABILITY_REQUEST_REASON_INVALID"));
            }
            request.setStatus(AvailabilityRequest.Status.CLOSED);
            request.setClosedReason(reason);
            request.setClosedAt(LocalDateTime.now());
            if (dto.getNotes() != null && !dto.getNotes().isBlank()) request.setNotes(dto.getNotes());
            return ResponseEntity.ok(ApiResponse.success(200, "Availability request closed",
                toDTO(requestRepository.save(request))));
        } catch (Exception e) {
            log.error("Error closing availability request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to close the request", "AVAILABILITY_REQUEST_CLOSE_FAILED"));
        }
    }

    /** By hand, for the reply that matched no header — see LinkReplyDTO. */
    @Transactional
    public ResponseEntity<ApiResponse<?>> linkReply(String requestIdObfuscated, LinkReplyDTO dto) {
        try {
            AvailabilityRequest request = find(requestIdObfuscated);
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Availability request not found", "AVAILABILITY_REQUEST_NOT_FOUND"));
            }
            Long messageId = decodeOrNull(dto.getMessageId());
            if (messageId == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "That message id could not be read", "MESSAGE_ID_INVALID"));
            }
            markReplied(request, messageId, LocalDateTime.now());
            return ResponseEntity.ok(ApiResponse.success(200, "Reply linked",
                toDTO(requestRepository.save(request))));
        } catch (Exception e) {
            log.error("Error linking a reply", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to link the reply", "AVAILABILITY_REQUEST_LINK_FAILED"));
        }
    }

    /* --------------------------------------------------------------- hooks */

    /**
     * A reply has arrived somewhere in the mailbox: does it answer an ask?
     *
     * Matched on headers, not on wording — In-Reply-To or References naming our Message-ID, else the
     * same thread. Anything else stays unmatched and the request keeps saying "no reply matched",
     * which is the truth and is what the manual link is for.
     */
    @Transactional
    public void noticeIncomingMessage(Long messageId, String inReplyTo, String references, String threadId,
                                      LocalDateTime receivedAt) {
        try {
            AvailabilityRequest match = null;

            for (String header : new String[] { inReplyTo, references }) {
                if (header == null || header.isBlank() || match != null) continue;
                for (String token : header.split("[,\\s]+")) {
                    String cleaned = token.trim();
                    if (cleaned.isEmpty()) continue;
                    match = requestRepository.findFirstByRfcMessageId(cleaned).orElse(null);
                    if (match != null) break;
                }
            }
            if (match == null && threadId != null && !threadId.isBlank()) {
                match = requestRepository.findByThreadId(threadId).stream()
                    .filter(AvailabilityRequest::isOpen).findFirst().orElse(null);
            }
            if (match == null || !match.isOpen()) return;

            markReplied(match, messageId, receivedAt != null ? receivedAt : LocalDateTime.now());
            requestRepository.save(match);
            log.info("Availability request {} has a reply", match.getId());
        } catch (Exception e) {
            /* a mailbox sync must never fail because of bookkeeping */
            log.warn("Could not match an incoming message to an availability request: {}", e.getMessage());
        }
    }

    /**
     * A stay has been confirmed, so whatever was waiting on it is finished with.
     *
     * Nobody goes back to close a request by hand once the booking is in, and an ask left open would
     * show up on the chase list for a night already secured.
     */
    @Transactional
    public void noticeStayConfirmed(Long stayId) {
        try {
            for (AvailabilityRequest request : requestRepository.findOpenForStay(stayId)) {
                request.setStatus(AvailabilityRequest.Status.CLOSED);
                request.setClosedReason(AvailabilityRequest.ClosedReason.CONFIRMED);
                request.setClosedAt(LocalDateTime.now());
                requestRepository.save(request);
            }
        } catch (Exception e) {
            log.warn("Could not auto-close availability requests for stay {}: {}", stayId, e.getMessage());
        }
    }

    /* --------------------------------------------------------------- reading */

    public ResponseEntity<ApiResponse<?>> list(String safariIdObfuscated) {
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            List<AvailabilityRequestDTO> dtos = requestRepository.findWithStaysBySafari(safariId)
                .stream().map(this::toDTO).toList();
            Map<String, Object> response = new HashMap<>();
            response.put("availabilityRequests", dtos);
            response.put("totalItems", dtos.size());
            return ResponseEntity.ok(ApiResponse.success(200, "Availability requests retrieved", response));
        } catch (Exception e) {
            log.error("Error listing availability requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list availability requests", "AVAILABILITY_REQUEST_LIST_FAILED"));
        }
    }

    /**
     * One row per NIGHT that has been asked about — the day workspace's single read.
     *
     * Shaped like the billing coverage the day screen already draws chips from, so the stay cards
     * ask one question of one payload rather than one question each.
     */
    public ResponseEntity<ApiResponse<?>> coverage(String safariIdObfuscated) {
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            LocalDateTime now = LocalDateTime.now();
            List<Map<String, Object>> rows = new ArrayList<>();

            for (AvailabilityRequest request : requestRepository.findWithStaysBySafari(safariId)) {
                for (AvailabilityRequestStay night : request.getStays()) {
                    if (night.getStay() == null) continue;
                    Map<String, Object> row = new HashMap<>();
                    row.put("stayId", idObfuscator.encodeId(night.getStay().getId()));
                    row.put("requestId", idObfuscator.encodeId(request.getId()));
                    row.put("accommodationName", request.getAccommodation() != null
                        ? request.getAccommodation().getName() : null);
                    row.put("status", request.getStatus().name());
                    row.put("closedReason", request.getClosedReason() != null
                        ? request.getClosedReason().name() : null);
                    row.put("sentAt", request.getSentAt());
                    row.put("repliedAt", request.getRepliedAt());
                    row.put("chaseDueAt", request.getChaseDueAt());
                    row.put("chaseDue", isChaseDue(request, now));
                    row.put("emailMessageId", request.getEmailMessageId() != null
                        ? idObfuscator.encodeId(request.getEmailMessageId()) : null);
                    row.put("emailAccountId", request.getEmailAccountId() != null
                        ? idObfuscator.encodeId(request.getEmailAccountId()) : null);
                    rows.add(row);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("coverage", rows);
            return ResponseEntity.ok(ApiResponse.success(200, "Availability coverage retrieved", response));
        } catch (Exception e) {
            log.error("Error reading availability coverage", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to read availability coverage", "AVAILABILITY_COVERAGE_FAILED"));
        }
    }

    /* -------------------------------------------------------------- helpers */

    private void markReplied(AvailabilityRequest request, Long messageId, LocalDateTime at) {
        request.setReplyMessageId(messageId);
        request.setRepliedAt(at);
        /* REPLIED, not CONFIRMED: an answer is not an agreement */
        if (request.getStatus() == AvailabilityRequest.Status.SENT) {
            request.setStatus(AvailabilityRequest.Status.REPLIED);
        }
    }

    private boolean isChaseDue(AvailabilityRequest request, LocalDateTime now) {
        return request.getStatus() == AvailabilityRequest.Status.SENT
            && request.getChaseDueAt() != null
            && !request.getChaseDueAt().isAfter(now);
    }

    private AvailabilityRequest find(String idObfuscated) {
        Long id = idObfuscator.decodeId(idObfuscated);
        return id == null ? null : requestRepository.findById(id).orElse(null);
    }

    private Long decodeOrNull(String obfuscated) {
        if (obfuscated == null || obfuscated.isBlank()) return null;
        try {
            return idObfuscator.decodeId(obfuscated);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeList(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception e) {
            return String.join(", ", values);
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readList(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        try {
            return objectMapper.readValue(stored, List.class);
        } catch (Exception e) {
            return List.of(stored.split("\\s*,\\s*"));
        }
    }

    private AvailabilityRequestDTO toDTO(AvailabilityRequest request) {
        List<AvailabilityRequestDTO.Night> nights = request.getStays().stream()
            .map(night -> AvailabilityRequestDTO.Night.builder()
                .stayId(night.getStay() != null ? idObfuscator.encodeId(night.getStay().getId()) : null)
                .safariDayId(night.getSafariDayId() != null ? idObfuscator.encodeId(night.getSafariDayId()) : null)
                .dayNumber(night.getDayNumber())
                .nightDate(night.getNightDate())
                .stayStillOnSafari(night.getStay() != null)
                .build())
            .toList();

        return AvailabilityRequestDTO.builder()
            .id(idObfuscator.encodeId(request.getId()))
            .safariId(request.getSafari() != null ? idObfuscator.encodeId(request.getSafari().getId()) : null)
            .safariCode(request.getSafari() != null ? request.getSafari().getCode() : null)
            .accommodationId(request.getAccommodation() != null
                ? idObfuscator.encodeId(request.getAccommodation().getId()) : null)
            .accommodationName(request.getAccommodation() != null ? request.getAccommodation().getName() : null)
            .status(request.getStatus().name())
            .closedReason(request.getClosedReason() != null ? request.getClosedReason().name() : null)
            .emailMessageId(request.getEmailMessageId() != null
                ? idObfuscator.encodeId(request.getEmailMessageId()) : null)
            .emailAccountId(request.getEmailAccountId() != null
                ? idObfuscator.encodeId(request.getEmailAccountId()) : null)
            .threadId(request.getThreadId())
            .toAddress(request.getToAddress())
            .ccAddresses(readList(request.getCcAddresses()))
            .bccAddresses(readList(request.getBccAddresses()))
            .subject(request.getSubject())
            .sentAt(request.getSentAt())
            .chaseDueAt(request.getChaseDueAt())
            .chaseDue(isChaseDue(request, LocalDateTime.now()))
            .repliedAt(request.getRepliedAt())
            .replyMessageId(request.getReplyMessageId() != null
                ? idObfuscator.encodeId(request.getReplyMessageId()) : null)
            .closedAt(request.getClosedAt())
            .notes(request.getNotes())
            .nights(nights)
            .build();
    }

    private User currentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return principal instanceof User user ? user : null;
        } catch (Exception e) {
            return null;
        }
    }
}
