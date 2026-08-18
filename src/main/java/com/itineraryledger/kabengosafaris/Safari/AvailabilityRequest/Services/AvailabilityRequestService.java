package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services;

import java.time.DayOfWeek;
import java.time.LocalDate;
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
    /*
     * Our own copy of what went out, so a record can be repaired from it.
     *
     * A plain repository rather than a relation: mail lives under an email ACCOUNT and a safari has
     * no business owning a foreign key into somebody's mailbox.
     */
    private final com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository messageRepository;
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

            /*
             * The Message-ID of what we sent, which is what a reply's In-Reply-To will name.
             *
             * Taken from our own copy of the sent mail rather than asked of the caller: the browser
             * does not see the header, and without it the matcher had nothing to match on — it read
             * `rfcMessageId` while the create path only ever wrote `emailMessageId`, so every reply
             * arrived, was looked at, and was silently ignored.
             */
            fillFromSentMessage(request);

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

    /**
     * A chase has gone out: the clock starts again.
     *
     * Without this the request keeps its original due date and the list asks for a chase that has
     * already been sent — so the same lodge gets nudged every morning by whoever opens the list
     * first. The count is kept because "we have chased three times" is the fact behind deciding a
     * property has stopped answering.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> recordChase(String requestIdObfuscated) {
        try {
            AvailabilityRequest request = find(requestIdObfuscated);
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Availability request not found",
                        "AVAILABILITY_REQUEST_NOT_FOUND"));
            }
            LocalDateTime now = LocalDateTime.now();
            request.setLastChasedAt(now);
            request.setChaseCount(request.chasesSoFar() + 1);
            /* three working days again — the same rule as the first send */
            request.setChaseDueAt(chaseDueFrom(now));
            /*
             * Still SENT, deliberately. A chase is us writing again, not them answering, and moving
             * the status would make the list look like progress that has not happened.
             */
            return ResponseEntity.ok(ApiResponse.success(200, "Chase recorded",
                toDTO(requestRepository.save(request))));
        } catch (Exception e) {
            log.error("Error recording a chase", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to record the chase", "AVAILABILITY_CHASE_RECORD_FAILED"));
        }
    }

    /* --------------------------------------------------------------- hooks */

    /**
     * A message has arrived: does it answer something we asked?
     *
     * Four ways, in order of how much they prove:
     *
     *  1. In-Reply-To / References naming the Message-ID we sent — the only one the standards
     *     guarantee, and the only one that was implemented. It never fired, because nothing wrote
     *     `rfcMessageId` in the first place.
     *  2. the same thread id.
     *  3. the same subject, allowing for the "Re:" a reply adds.
     *  4. the sender belongs to the property, and exactly one request to that property is waiting.
     *     A lodge answering from reservations@ when we wrote to info@, through a client that drops
     *     In-Reply-To, is not an edge case; it is Tuesday. "Exactly one" is what keeps this honest:
     *     with two open asks to the same lodge, a person has to say which.
     *
     * Automated mail is never a reply — a bounce for our own message would otherwise close the very
     * request it failed to deliver.
     */
    @Transactional
    public void noticeIncomingMessage(Long messageId, String inReplyTo, String references, String threadId,
                                      LocalDateTime receivedAt) {
        noticeIncomingMessage(messageId, inReplyTo, references, threadId, null, null, receivedAt);
    }

    @Transactional
    public void noticeIncomingMessage(Long messageId, String inReplyTo, String references, String threadId,
                                      String fromAddress, String subject, LocalDateTime receivedAt) {
        try {
            if (isAutomated(fromAddress, subject)) return;

            /* history first: a request written before the headers were stored can now be matched */
            repairMailDetails();

            AvailabilityRequest match = matchMessage(inReplyTo, references, threadId, fromAddress, subject);
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
     * Go looking for replies that have already arrived.
     *
     * Matching happens as mail is fetched, so anything received while that hook did not exist — or
     * while the app was down, or before a record kept the headers it needed — sits unanswered on the
     * chase list for ever. This re-reads the inbox for every open ask and applies the same rules, so
     * the state can be recovered instead of corrected by hand, one request at a time.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> rescanForReplies() {
        try {
            repairMailDetails();

            List<AvailabilityRequest> waiting = requestRepository.findChaseDueOrAwaiting();
            if (waiting.isEmpty()) {
                Map<String, Object> nothing = new HashMap<>();
                nothing.put("matched", 0);
                nothing.put("scanned", 0);
                nothing.put("waiting", 0);
                return ResponseEntity.ok(ApiResponse.success(200, "Nothing is waiting on a reply", nothing));
            }

            /* no request can be answered before it was sent, so the oldest one bounds the read */
            LocalDateTime since = waiting.stream()
                .map(AvailabilityRequest::getSentAt)
                .filter(java.util.Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusDays(60));

            var messages = messageRepository.findReceivedSince(since);
            int matched = 0;
            /* oldest first: the first answer to an ask is the one that answered it */
            for (int i = messages.size() - 1; i >= 0; i--) {
                var message = messages.get(i);
                if (isAutomated(message.getFromAddress(), message.getSubject())) continue;
                AvailabilityRequest hit = matchMessage(
                    message.getInReplyTo(), message.getReferences(), message.getThreadId(),
                    message.getFromAddress(), message.getSubject());
                if (hit == null || !hit.isOpen()) continue;
                if (message.getReceivedAt() != null && hit.getSentAt() != null
                    && message.getReceivedAt().isBefore(hit.getSentAt())) continue;
                markReplied(hit, message.getId(), message.getReceivedAt() != null
                    ? message.getReceivedAt() : LocalDateTime.now());
                requestRepository.save(hit);
                matched++;
            }

            Map<String, Object> report = new HashMap<>();
            report.put("matched", matched);
            report.put("scanned", messages.size());
            report.put("waiting", waiting.size());
            log.info("Reply hunt: {} of {} open availability requests matched from {} messages",
                matched, waiting.size(), messages.size());
            return ResponseEntity.ok(ApiResponse.success(200, matched == 0
                ? "No new replies matched"
                : matched + (matched == 1 ? " reply matched" : " replies matched"), report));
        } catch (Exception e) {
            log.error("Error hunting for availability replies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to look for replies", "AVAILABILITY_RESCAN_FAILED"));
        }
    }

    /** The matching rules themselves, so live mail and a rescan can never disagree. */
    private AvailabilityRequest matchMessage(String inReplyTo, String references, String threadId,
                                             String fromAddress, String subject) {
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
        if (match == null) match = matchBySubject(subject);
        if (match == null) match = matchBySender(fromAddress, subject);
        return match;
    }

    /** Mailer daemons and delivery reports are about a message, not answers to it. */
    private boolean isAutomated(String fromAddress, String subject) {
        String from = fromAddress == null ? "" : fromAddress.toLowerCase();
        String subj = subject == null ? "" : subject.toLowerCase();
        return from.contains("mailer-daemon") || from.contains("postmaster")
            || from.startsWith("no-reply") || from.startsWith("noreply")
            || subj.startsWith("delivery status notification")
            || subj.startsWith("undeliverable")
            || subj.contains("mail delivery failed")
            || subj.contains("returning message to sender");
    }

    /** "Re: Availability Request · Outpost Lodge · 23–24 Jan 2027" is our own subject, answered. */
    private static String bareSubject(String subject) {
        if (subject == null) return "";
        String out = subject.trim();
        /* strip any run of Re:/Fwd:/Fw: prefixes, however the client stacked them */
        while (true) {
            String lower = out.toLowerCase();
            if (lower.startsWith("re:") || lower.startsWith("aw:")) out = out.substring(3).trim();
            else if (lower.startsWith("fwd:")) out = out.substring(4).trim();
            else if (lower.startsWith("fw:")) out = out.substring(3).trim();
            else break;
        }
        return out.replaceAll("\\s+", " ").toLowerCase();
    }

    private AvailabilityRequest matchBySubject(String subject) {
        String bare = bareSubject(subject);
        if (bare.isEmpty()) return null;
        List<AvailabilityRequest> candidates = requestRepository.findChaseDueOrAwaiting();
        for (AvailabilityRequest request : candidates) {
            String ours = bareSubject(request.getSubject());
            if (!ours.isEmpty() && (ours.equals(bare) || bare.contains(ours))) return request;
        }
        return null;
    }

    /**
     * The sender belongs to the property, and exactly one ask to it is waiting.
     *
     * The address is compared whole and by domain: a group's reservations desk answers for its
     * camps, and the camp's own address is often on the same domain as the one we wrote to.
     */
    private AvailabilityRequest matchBySender(String fromAddress, String subject) {
        if (fromAddress == null || fromAddress.isBlank()) return null;
        String from = fromAddress.trim().toLowerCase();
        String domain = from.contains("@") ? from.substring(from.indexOf('@') + 1) : "";

        List<AvailabilityRequest> waiting = requestRepository.findChaseDueOrAwaiting();
        List<AvailabilityRequest> hits = new ArrayList<>();
        for (AvailabilityRequest request : waiting) {
            if (senderBelongsToRequest(request, from, domain)) hits.add(request);
        }
        if (hits.size() == 1) return hits.get(0);
        if (hits.size() > 1) {
            /* several asks to the same property: only the subject can say which, and it did not */
            log.info("A reply from {} could match {} open availability requests — leaving it for a person",
                from, hits.size());
        }
        return null;
    }

    private boolean senderBelongsToRequest(AvailabilityRequest request, String from, String domain) {
        List<String> known = new ArrayList<>();
        if (request.getToAddress() != null) known.add(request.getToAddress());
        for (String cc : readList(request.getCcAddresses())) known.add(cc);
        Accommodation accommodation = request.getAccommodation();
        if (accommodation != null) {
            try {
                accommodation.getEmails().forEach(e -> {
                    if (e.getEmail() != null) known.add(e.getEmail());
                });
            } catch (Exception ignored) {
                /* a lazy collection outside a session tells us nothing; the addresses above still do */
            }
        }
        for (String candidate : known) {
            if (candidate == null || candidate.isBlank()) continue;
            String value = candidate.trim().toLowerCase();
            if (value.equals(from)) return true;
            if (!domain.isEmpty() && value.endsWith("@" + domain)) return true;
        }
        return false;
    }

    /**
     * Fill in what a record should have kept about the mail it names.
     *
     * The Message-ID, the thread, the subject and the addresses all sit on our own copy of the sent
     * message. A record written before the create path read them shows "written to —" and can never
     * be matched to a reply, and there is no reason for either: the message is right there.
     */
    private void repairMailDetails() {
        for (AvailabilityRequest request : requestRepository.findNeedingMailRepair()) {
            if (fillFromSentMessage(request)) requestRepository.save(request);
        }
    }

    /** Returns true when something was actually filled in. */
    private boolean fillFromSentMessage(AvailabilityRequest request) {
        if (request.getEmailMessageId() == null) return false;
        var sent = messageRepository.findById(request.getEmailMessageId()).orElse(null);
        if (sent == null) return false;
        boolean changed = false;
        if (request.getRfcMessageId() == null && sent.getMessageId() != null) {
            request.setRfcMessageId(sent.getMessageId());
            changed = true;
        }
        if (request.getThreadId() == null && sent.getThreadId() != null) {
            request.setThreadId(sent.getThreadId());
            changed = true;
        }
        if ((request.getSubject() == null || request.getSubject().isBlank()) && sent.getSubject() != null) {
            request.setSubject(sent.getSubject());
            changed = true;
        }
        if ((request.getToAddress() == null || request.getToAddress().isBlank()) && sent.getToAddresses() != null) {
            String first = firstAddress(sent.getToAddresses());
            if (first != null) {
                request.setToAddress(first);
                changed = true;
            }
        }
        if ((request.getCcAddresses() == null || request.getCcAddresses().isBlank())
            && sent.getCcAddresses() != null && !sent.getCcAddresses().isBlank()) {
            request.setCcAddresses(sent.getCcAddresses());
            changed = true;
        }
        return changed;
    }

    /**
     * The first address out of however the message stored them — a JSON array for mail this app
     * composed, a raw header for mail it synced.
     */
    private String firstAddress(String stored) {
        String value = stored.trim();
        if (value.startsWith("[")) {
            List<String> parsed = readList(value);
            return parsed.isEmpty() ? null : parsed.get(0);
        }
        String[] parts = value.split(",");
        return parts.length == 0 ? null : parts[0].trim();
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
        row.put("lastChasedAt", request.getLastChasedAt());
        row.put("chaseCount", request.chasesSoFar());
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

    /**
     * One row of the chase list — flat, and carrying what the row itself has to say.
     *
     * The nights are summarised rather than listed: a list page needs "3 nights, 29 Jan – 1 Feb", and
     * whoever wants the detail opens the request. Counts of Cc and Bcc rather than the addresses,
     * because a table is not the place to spill who was blind-copied.
     */
    public Map<String, Object> toRow(AvailabilityRequest request, LocalDateTime now) {
        List<LocalDate> nights = request.getStays().stream()
            .map(AvailabilityRequestStay::getNightDate)
            .filter(java.util.Objects::nonNull)
            .sorted()
            .toList();

        Map<String, Object> row = new HashMap<>();
        row.put("id", idObfuscator.encodeId(request.getId()));
        row.put("safariId", request.getSafari() != null ? idObfuscator.encodeId(request.getSafari().getId()) : null);
        row.put("safariCode", request.getSafari() != null ? request.getSafari().getCode() : null);
        row.put("safariName", request.getSafari() != null ? request.getSafari().getName() : null);
        row.put("accommodationId", request.getAccommodation() != null
            ? idObfuscator.encodeId(request.getAccommodation().getId()) : null);
        row.put("accommodationName", request.getAccommodation() != null
            ? request.getAccommodation().getName() : null);
        row.put("status", request.getStatus().name());
        row.put("closedReason", request.getClosedReason() != null ? request.getClosedReason().name() : null);
        row.put("sentAt", request.getSentAt());
        row.put("chaseDueAt", request.getChaseDueAt());
        row.put("chaseDue", isChaseDue(request, now));
        row.put("repliedAt", request.getRepliedAt());
        row.put("lastChasedAt", request.getLastChasedAt());
        row.put("chaseCount", request.chasesSoFar());
        row.put("closedAt", request.getClosedAt());
        row.put("nightCount", nights.size());
        row.put("firstNight", nights.isEmpty() ? null : nights.get(0));
        row.put("lastNight", nights.isEmpty() ? null : nights.get(nights.size() - 1));
        row.put("subject", request.getSubject());
        row.put("toAddress", request.getToAddress());
        row.put("ccCount", readList(request.getCcAddresses()).size());
        row.put("bccCount", readList(request.getBccAddresses()).size());
        row.put("emailAccountId", request.getEmailAccountId() != null
            ? idObfuscator.encodeId(request.getEmailAccountId()) : null);
        row.put("emailMessageId", request.getEmailMessageId() != null
            ? idObfuscator.encodeId(request.getEmailMessageId()) : null);
        row.put("replyMessageId", request.getReplyMessageId() != null
            ? idObfuscator.encodeId(request.getReplyMessageId()) : null);
        return row;
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
