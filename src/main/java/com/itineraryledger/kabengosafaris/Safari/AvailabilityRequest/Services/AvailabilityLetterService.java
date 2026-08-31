package com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Services;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationEmail;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequest;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Entity.AvailabilityRequestStay;
import com.itineraryledger.kabengosafaris.Safari.AvailabilityRequest.Repository.AvailabilityRequestRepository;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Entity.SafariDayAccommodation;
import com.itineraryledger.kabengosafaris.Safari.SafariDay.SafariDayAccommodation.Repository.SafariDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Entity.SafariPax;
import com.itineraryledger.kabengosafaris.Safari.SafariPax.Repository.SafariPaxRepository;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The letter itself: subject, body, and who it goes to.
 *
 * Rendered HERE rather than in the browser so one wording serves every sender. The office edits it
 * as an email template (event AVAILABILITY_REQUEST) without a deploy, and anything that later sends
 * unattended — a scheduled chase, a job — produces exactly what a person would have sent. Two
 * renderers would have drifted apart inside a month.
 *
 * The facts are assembled from the safari, never taken from the caller: dates from the days, rooms
 * and board from the stays, guests from the pax bands, addresses from the property AND its
 * headquarters. A caller that could pass its own dates could pass wrong ones.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AvailabilityLetterService {

    private final AvailabilityRequestRepository requestRepository;
    private final SafariRepository safariRepository;
    private final AccommodationRepository accommodationRepository;
    private final SafariDayAccommodationRepository stayRepository;
    private final SafariPaxRepository paxRepository;
    private final EmailTemplateRenderer templateRenderer;
    /** to quote what was actually sent, rather than re-rendering it */
    private final com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailMessageGetService messageGetService;
    private final IdObfuscator idObfuscator;

    private static final String EVENT = "AVAILABILITY_REQUEST";
    private static final String CHASE_EVENT = "AVAILABILITY_REQUEST_CHASE";
    private static final DateTimeFormatter SLASH = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM yyyy");
    /** "18 Aug 2026, 08:42" — the stamp above a quoted message. */
    private static final DateTimeFormatter QUOTE_STAMP = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    /** The company this deployment is, for the greeting line. */
    @Value("${app.company.name:}")
    private String brandName;

    /** The accent the headings and the panel rule are drawn in. */
    @Value("${app.brand.accent:#1c7a58}")
    private String accentColor;

    public ResponseEntity<ApiResponse<?>> preview(String safariIdObfuscated, String accommodationIdObfuscated,
                                                  List<String> stayIdsObfuscated) {
        try {
            Long safariId = idObfuscator.decodeId(safariIdObfuscated);
            Safari safari = safariId == null ? null : safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND"));
            }

            Long accommodationId = idObfuscator.decodeId(accommodationIdObfuscated);
            Accommodation property = accommodationId == null
                ? null : accommodationRepository.findById(accommodationId).orElse(null);
            if (property == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Accommodation not found", "ACCOMMODATION_NOT_FOUND"));
            }

            /* the nights, in date order, and only ones belonging to this safari */
            List<SafariDayAccommodation> stays = new ArrayList<>();
            for (String stayIdObfuscated : stayIdsObfuscated == null ? List.<String>of() : stayIdsObfuscated) {
                Long stayId = idObfuscator.decodeId(stayIdObfuscated);
                SafariDayAccommodation stay = stayId == null ? null : stayRepository.findById(stayId).orElse(null);
                if (stay == null || stay.getSafariDay() == null || stay.getSafariDay().getSafari() == null
                    || !stay.getSafariDay().getSafari().getId().equals(safariId)) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(400,
                        "One of those nights does not belong to this safari", "STAY_NOT_ON_SAFARI"));
                }
                stays.add(stay);
            }
            if (stays.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "There are no nights to ask about", "NO_STAYS"));
            }
            stays.sort((a, b) -> nightOf(a).compareTo(nightOf(b)));

            /* grouped by date, because one night can hold several rooms */
            Map<LocalDate, List<SafariDayAccommodation>> byNight = new LinkedHashMap<>();
            for (SafariDayAccommodation stay : stays) {
                byNight.computeIfAbsent(nightOf(stay), key -> new ArrayList<>()).add(stay);
            }

            /*
             * Split into CONSECUTIVE stretches before anything is written.
             *
             * A property used on two separate nights is not one booking. Reading the first night and
             * the last as check-in and check-out produced a letter saying "26/01 to 29/01, 2 nights"
             * — three nights of dates against a count of two — for guests who are somewhere else on
             * the 27th. A reservations desk either blocks a night nobody wants or writes back to ask
             * what we meant, and both cost more than getting it right here.
             */
            List<List<LocalDate>> blocks = consecutiveBlocks(byNight.keySet());
            List<LocalDate> firstBlock = blocks.get(0);

            LocalDate checkIn = firstBlock.get(0);
            LocalDate checkOut = firstBlock.get(firstBlock.size() - 1).plusDays(1);

            Pax pax = paxOf(safari.getId());
            Recipients recipients = recipientsFor(property);

            Map<String, String> variables = new HashMap<>();
            variables.put("greetingName", greetingName(recipients.label()));
            variables.put("brandName", brandName);
            variables.put("accommodationName", property.getName() != null ? property.getName() : "your property");
            variables.put("checkIn", checkIn.format(SLASH));
            variables.put("checkOut", checkOut.format(SLASH));
            /* this block's nights, not the trip's — the table describes one visit */
            variables.put("nights", String.valueOf(firstBlock.size()));
            variables.put("guestCount", pax.total() + (pax.total() == 1 ? " Guest" : " Guests"));
            variables.put("paxBreakdown", pax.text());
            variables.put("roomConfiguration", roomLines(byNight.get(checkIn)));
            variables.put("mealPlan", mealPlan(subMap(byNight, firstBlock)));
            /*
             * The second and later visits, each with its own dates, rooms and board — which is what
             * the template's {{stayBlocks}} was always for and what this used to leave empty.
             */
            variables.put("stayBlocks", laterBlocks(byNight, blocks));
            variables.put("reference", reference(safari));
            variables.put("accentColor", accentColor);

            String html = templateRenderer.renderTemplate(EVENT, variables);

            /* every stretch in the subject: a desk filing by date should see both */
            StringBuilder spans = new StringBuilder();
            for (List<LocalDate> block : blocks) {
                if (spans.length() > 0) spans.append(" & ");
                spans.append(range(block.get(0), block.get(block.size() - 1).plusDays(1)));
            }
            String subject = "Availability Request · " + variables.get("accommodationName")
                + " · " + spans;

            Map<String, Object> response = new HashMap<>();
            response.put("subject", subject);
            response.put("html", html);
            response.put("to", recipients.to());
            response.put("cc", recipients.cc());
            response.put("viaHeadquarters", recipients.viaParent());
            response.put("headquartersName", recipients.parentName());
            return ResponseEntity.ok(ApiResponse.success(200, "Availability letter rendered", response));
        } catch (IllegalArgumentException | IllegalStateException bad) {
            /*
             * The template is missing, disabled, or short of a variable — all three fixable in the
             * templates screen, so they are reported as such. A 500 would send somebody here instead.
             */
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "The availability request template could not be rendered: " + bad.getMessage(),
                "AVAILABILITY_TEMPLATE_UNUSABLE"));
        } catch (Exception e) {
            log.error("Error rendering the availability letter", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to render the availability letter", "AVAILABILITY_LETTER_FAILED"));
        }
    }

    /**
     * The chase letter, and the envelope the first one went out in.
     *
     * The recipients come from the RECORD, not from the mail thread: the request kept the To, the Cc
     * and the Bcc, and a follow-up that quietly drops the people who were copied — or the address
     * that was blind-copied — changes who is in the conversation without saying so. Bcc especially:
     * nothing in the thread can tell you it was there.
     */
    public ResponseEntity<ApiResponse<?>> chaseLetter(String requestIdObfuscated) {
        try {
            Long requestId = idObfuscator.decodeId(requestIdObfuscated);
            AvailabilityRequest request = requestId == null
                ? null : requestRepository.findById(requestId).orElse(null);
            if (request == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Availability request not found",
                        "AVAILABILITY_REQUEST_NOT_FOUND"));
            }

            Accommodation property = request.getAccommodation();
            Safari safari = request.getSafari();

            /* the nights this ask covered, from the record rather than the safari as it stands now */
            List<SafariDayAccommodation> stays = new ArrayList<>();
            List<LocalDate> nightDates = new ArrayList<>();
            for (AvailabilityRequestStay night : request.getStays()) {
                if (night.getNightDate() != null) nightDates.add(night.getNightDate());
                if (night.getStay() != null) stays.add(night.getStay());
            }
            nightDates.sort(LocalDate::compareTo);
            if (nightDates.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "That request records no nights, so there is nothing to chase about",
                    "NO_NIGHTS_ON_REQUEST"));
            }

            LocalDate checkIn = nightDates.get(0);
            LocalDate checkOut = nightDates.get(nightDates.size() - 1).plusDays(1);
            Pax pax = paxOf(safari.getId());

            long waited = request.getSentAt() == null
                ? 0 : Duration.between(request.getSentAt(), LocalDateTime.now()).toDays();

            Map<String, String> variables = new HashMap<>();
            variables.put("greetingName", greetingName(labelOfRecordedTo(property, request.getToAddress())));
            variables.put("brandName", brandName);
            variables.put("accommodationName", property != null && property.getName() != null
                ? property.getName() : "your property");
            variables.put("askedOn", request.getSentAt() != null
                ? request.getSentAt().toLocalDate().format(SLASH) : "an earlier date");
            variables.put("waitingDays", waited <= 1 ? "a day" : waited + " days");
            variables.put("checkIn", checkIn.format(SLASH));
            variables.put("checkOut", checkOut.format(SLASH));
            variables.put("nights", String.valueOf(nightDates.size()));
            variables.put("guestCount", pax.total() + (pax.total() == 1 ? " Guest" : " Guests"));
            variables.put("roomConfiguration", stays.isEmpty()
                /* the stay rows may be gone; the request still knows what it asked for in words */
                ? "<li style=\"margin: 2px 0\">As per our request of " + variables.get("askedOn") + "</li>"
                : roomLines(stays));
            variables.put("reference", reference(safari));
            variables.put("accentColor", accentColor);

            String html = templateRenderer.renderTemplate(CHASE_EVENT, variables);

            String subject;
            if (request.getSubject() != null && !request.getSubject().isBlank()) {
                /* the same thread, so the same subject with Re: — not a new conversation */
                subject = request.getSubject().toLowerCase().startsWith("re:")
                    ? request.getSubject() : "Re: " + request.getSubject();
            } else {
                /* an older row kept no subject; rebuilt WITH its dates rather than left vague */
                subject = "Re: Availability Request · " + variables.get("accommodationName")
                    + " · " + range(checkIn, checkOut);
            }

            /*
             * Whom to write to: the record, and only then the property.
             *
             * Requests written before the envelope was recorded have no To at all, and a chase that
             * opens with an empty recipient box and a disabled Send button is a dead end. So the
             * property's own resolution is the fallback — the same one the first letter used.
             */
            String to = request.getToAddress();
            List<String> cc = readAddresses(request.getCcAddresses());
            List<String> bcc = readAddresses(request.getBccAddresses());
            boolean recipientsRecovered = false;
            if ((to == null || to.isBlank()) && property != null) {
                Recipients resolved = recipientsFor(property);
                to = resolved.to();
                if (cc.isEmpty()) cc = resolved.cc();
                recipientsRecovered = true;
            }

            /*
             * The request quoted underneath, as a reply would — what was actually SENT rather than a
             * re-render, because an edited letter is what the property read. Absent when the stored
             * .eml cannot be read, in which case the chase still repeats the dates and the rooms.
             */
            String quoted = request.getEmailMessageId() != null
                ? messageGetService.htmlBodyOf(request.getEmailMessageId()) : null;
            String quotedIntro = request.getSentAt() != null
                ? "On " + request.getSentAt().format(QUOTE_STAMP) + ", we wrote:"
                : "Our earlier request:";

            Map<String, Object> response = new HashMap<>();
            response.put("subject", subject);
            response.put("html", html);
            response.put("to", to);
            response.put("cc", cc);
            response.put("bcc", bcc);
            response.put("recipientsRecovered", recipientsRecovered);
            response.put("quotedHtml", quoted);
            response.put("quotedIntro", quoted != null ? quotedIntro : null);
            response.put("inReplyToMessageId", request.getEmailMessageId() != null
                ? idObfuscator.encodeId(request.getEmailMessageId()) : null);
            response.put("chasesSoFar", request.chasesSoFar());
            return ResponseEntity.ok(ApiResponse.success(200, "Chase letter rendered", response));
        } catch (IllegalArgumentException | IllegalStateException bad) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "The chase template could not be rendered: " + bad.getMessage(),
                "AVAILABILITY_CHASE_TEMPLATE_UNUSABLE"));
        } catch (Exception e) {
            log.error("Error rendering the chase letter", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to render the chase letter", "AVAILABILITY_CHASE_FAILED"));
        }
    }

    /** The label on whichever address the first letter went to, for the greeting. */
    private String labelOfRecordedTo(Accommodation property, String toAddress) {
        if (property == null || toAddress == null) return null;
        for (AccommodationEmail email : property.getEmails()) {
            if (toAddress.equalsIgnoreCase(email.getEmail())) return email.getLabel();
        }
        Accommodation parent = property.getParentAccommodation();
        if (parent != null) {
            for (AccommodationEmail email : parent.getEmails()) {
                if (toAddress.equalsIgnoreCase(email.getEmail())) return email.getLabel();
            }
        }
        return null;
    }

    /** Stored as a JSON array by the composer; a comma list by anything older. */
    private List<String> readAddresses(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        String trimmed = stored.trim();
        if (trimmed.startsWith("[")) {
            try {
                return new ObjectMapper().readValue(trimmed, new TypeReference<List<String>>() {});
            } catch (Exception ignored) {
                /* fall through to the comma reading */
            }
        }
        return List.of(trimmed.split("\\s*,\\s*"));
    }

    /* -------------------------------------------------------------- pieces */

    /**
     * Nights grouped into consecutive stretches.
     *
     * 26th, 28th is two stretches, not one span with a hole in it. Always returns at least one
     * block when there is at least one night, so the caller can read block zero without checking.
     */
    private List<List<LocalDate>> consecutiveBlocks(Collection<LocalDate> nights) {
        List<LocalDate> ordered = new ArrayList<>(new TreeSet<>(nights));
        List<List<LocalDate>> blocks = new ArrayList<>();
        List<LocalDate> current = new ArrayList<>();

        for (LocalDate night : ordered) {
            if (!current.isEmpty() && !current.get(current.size() - 1).plusDays(1).equals(night)) {
                blocks.add(current);
                current = new ArrayList<>();
            }
            current.add(night);
        }
        if (!current.isEmpty()) blocks.add(current);
        return blocks;
    }

    private Map<LocalDate, List<SafariDayAccommodation>> subMap(
            Map<LocalDate, List<SafariDayAccommodation>> byNight, List<LocalDate> block) {
        Map<LocalDate, List<SafariDayAccommodation>> out = new LinkedHashMap<>();
        for (LocalDate night : block) {
            List<SafariDayAccommodation> rooms = byNight.get(night);
            if (rooms != null) out.put(night, rooms);
        }
        return out;
    }

    /**
     * Visits after the first, written out in full.
     *
     * Each one repeats its own dates, nights, rooms and board rather than referring back, because a
     * reservations desk reads these as separate bookings and quotes them separately. Empty for a
     * property visited once, which is the common case and leaves the letter exactly as it was.
     */
    private String laterBlocks(Map<LocalDate, List<SafariDayAccommodation>> byNight,
                               List<List<LocalDate>> blocks) {
        if (blocks.size() < 2) return "";

        StringBuilder out = new StringBuilder();
        out.append("<p style=\"margin: 16px 0 6px; font-size: 13px; color: #6b7280\">")
           .append("The guests return to the property later in the trip. ")
           .append("These are separate nights, with a gap in between — please treat each as its own booking.")
           .append("</p>");

        for (int i = 1; i < blocks.size(); i++) {
            List<LocalDate> block = blocks.get(i);
            LocalDate from = block.get(0);
            LocalDate to = block.get(block.size() - 1).plusDays(1);

            out.append("<table style=\"border-collapse: collapse; width: 100%; margin: 10px 0 0;")
               .append(" background-color: #f7faf8; border-left: 3px solid ").append(accentColor)
               .append("\"><tbody><tr><td style=\"padding: 10px 14px\">")
               .append("<p style=\"margin: 0 0 6px; font-size: 12px; font-weight: 700;")
               .append(" letter-spacing: 0.08em; text-transform: uppercase; color: ").append(accentColor)
               .append("\">Second stay</p>")
               .append("<table style=\"border-collapse: collapse\"><tbody>")
               .append(factRow("Check-in", from.format(SLASH)))
               .append(factRow("Check-out", to.format(SLASH)))
               .append(factRow("Number of Nights", String.valueOf(block.size())))
               .append("</tbody></table>")
               .append("<p style=\"margin: 10px 0 4px; font-size: 12px; font-weight: 700;")
               .append(" letter-spacing: 0.08em; text-transform: uppercase; color: ").append(accentColor)
               .append("\">Room Configuration</p><ul style=\"margin: 0 0 4px; padding-left: 20px\">")
               .append(roomLines(byNight.get(from)))
               .append("</ul>")
               .append("<p style=\"margin: 10px 0 4px; font-size: 12px; font-weight: 700;")
               .append(" letter-spacing: 0.08em; text-transform: uppercase; color: ").append(accentColor)
               .append("\">Meal Plan Arrangement</p><ul style=\"margin: 0 0 4px; padding-left: 20px\">")
               .append(mealPlan(subMap(byNight, block)))
               .append("</ul></td></tr></tbody></table>");
        }
        return out.toString();
    }

    private String factRow(String label, String value) {
        return "<tr><td style=\"padding: 3px 18px 3px 0; color: #6b7280; font-size: 13px;"
            + " white-space: nowrap\">" + escape(label) + "</td>"
            + "<td style=\"padding: 3px 0; font-size: 14px\"><strong style=\"color: #111827\">"
            + escape(value) + "</strong></td></tr>";
    }

    private LocalDate nightOf(SafariDayAccommodation stay) {
        LocalDate date = stay.getSafariDay() != null ? stay.getSafariDay().getActualDate() : null;
        return date != null ? date : LocalDate.MIN;
    }

    /** A room line's identity: type and standard, so counting needs no delimiter. */
    private record RoomKey(String type, String standard) {}

    private String roomLines(List<SafariDayAccommodation> rooms) {
        Map<RoomKey, Integer> counted = new LinkedHashMap<>();
        for (SafariDayAccommodation room : rooms) {
            String type = room.getRoomType() != null ? room.getRoomType().getName() : "Room";
            String standard = room.getRoomStandard() != null ? room.getRoomStandard().getName() : null;
            counted.merge(new RoomKey(type, standard),
                room.getRoomCount() != null ? room.getRoomCount() : 1, Integer::sum);
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<RoomKey, Integer> entry : counted.entrySet()) {
            out.append("<li style=\"margin: 2px 0\"><strong style=\"color: #111827\">")
               .append(entry.getValue()).append(" &times;</strong> ")
               .append(escape(entry.getKey().type()));
            if (entry.getKey().standard() != null) {
                out.append(" <span style=\"color: #6b7280\">&middot; ")
                   .append(escape(entry.getKey().standard())).append("</span>");
            }
            out.append("</li>");
        }
        return out.toString();
    }

    /** One line per night: board changes mid-stay often enough that a desk needs to see which is which. */
    private String mealPlan(Map<LocalDate, List<SafariDayAccommodation>> byNight) {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<LocalDate, List<SafariDayAccommodation>> night : byNight.entrySet()) {
            Set<String> boards = new TreeSet<>();
            for (SafariDayAccommodation room : night.getValue()) {
                if (room.getBoardType() != null && room.getBoardType().getName() != null) {
                    boards.add(room.getBoardType().getName());
                }
            }
            out.append("<li style=\"margin: 2px 0\"><span style=\"color: #6b7280\">")
               .append(night.getKey().format(SLASH))
               .append("</span> &ndash; <strong style=\"color: #111827\">")
               .append(escape(boards.isEmpty() ? "To be confirmed" : String.join(" / ", boards)))
               .append("</strong></li>");
        }
        return out.toString();
    }

    private record Pax(int total, String text) {}

    private Pax paxOf(Long safariId) {
        List<SafariPax> rows = paxRepository.findBySafariId(safariId);
        int total = rows.stream().mapToInt(row -> row.getCount() != null ? row.getCount() : 0).sum();
        if (rows.isEmpty()) return new Pax(total, "Not recorded on this safari yet");

        Map<String, Map<String, Integer>> byNation = new LinkedHashMap<>();
        for (SafariPax row : rows) {
            String nation = row.getNationCategory() != null && row.getNationCategory().getName() != null
                ? row.getNationCategory().getName() : "Unspecified";
            String age = row.getAgeCategory() != null && row.getAgeCategory().getName() != null
                ? row.getAgeCategory().getName() : "Guest";
            byNation.computeIfAbsent(nation, key -> new LinkedHashMap<>())
                .merge(age, row.getCount() != null ? row.getCount() : 0, Integer::sum);
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> nation : byNation.entrySet()) {
            int nationTotal = nation.getValue().values().stream().mapToInt(Integer::intValue).sum();
            if (nation.getValue().size() == 1) {
                Map.Entry<String, Integer> only = nation.getValue().entrySet().iterator().next();
                /* "4 non-resident adults" reads better than "4 non-residents (4 adults)" */
                parts.add(nationTotal + " " + nation.getKey().toLowerCase()
                    + " " + plural(only.getKey(), only.getValue()));
            } else {
                List<String> ages = new ArrayList<>();
                nation.getValue().forEach((age, count) -> ages.add(count + " " + plural(age, count)));
                parts.add(nationTotal + " " + plural(nation.getKey(), nationTotal)
                    + " (" + String.join(", ", ages) + ")");
            }
        }
        return new Pax(total, String.join(", ", parts));
    }

    private String plural(String word, int count) {
        String lower = word.toLowerCase();
        return count == 1 ? lower : lower + "s";
    }

    private record Recipients(String to, List<String> cc, boolean viaParent, String parentName, String label) {}

    /**
     * The property's addresses, then its headquarters'.
     *
     * A branch often keeps none of its own — everything for it is answered by the group — so a To
     * taken from the property alone reports a camp as unreachable when its group can be written to
     * today. Sibling branches are left out: another camp has no part in this booking.
     */
    private Recipients recipientsFor(Accommodation property) {
        List<AccommodationEmail> mine = active(property);
        Accommodation parent = property.getParentAccommodation();
        List<AccommodationEmail> theirs = parent != null ? active(parent) : List.of();

        AccommodationEmail own = best(mine);
        AccommodationEmail chosen = own != null ? own : best(theirs);

        List<String> cc = new ArrayList<>();
        Set<String> seen = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        if (chosen != null && chosen.getEmail() != null) seen.add(chosen.getEmail());
        for (List<AccommodationEmail> list : List.of(mine, theirs)) {
            for (AccommodationEmail email : list) {
                if (email.getEmail() == null || seen.contains(email.getEmail())) continue;
                seen.add(email.getEmail());
                cc.add(email.getEmail());
            }
        }

        return new Recipients(
            chosen != null ? chosen.getEmail() : null,
            cc,
            chosen != null && own == null,
            parent != null ? parent.getName() : null,
            chosen != null ? chosen.getLabel() : null);
    }

    private List<AccommodationEmail> active(Accommodation property) {
        List<AccommodationEmail> list = new ArrayList<>();
        for (AccommodationEmail email : property.getEmails()) {
            if (email.getEmail() != null && !Boolean.FALSE.equals(email.getIsActive())) list.add(email);
        }
        /* sorted before anything is chosen, so a group with reservations1/2/3@ picks predictably */
        list.sort((a, b) -> a.getEmail().compareToIgnoreCase(b.getEmail()));
        return list;
    }

    private AccommodationEmail best(List<AccommodationEmail> list) {
        AccommodationEmail reservationsPrimary = null;
        AccommodationEmail reservations = null;
        AccommodationEmail primary = null;
        AccommodationEmail general = null;
        for (AccommodationEmail email : list) {
            boolean isReservations = email.getEmailType() == AccommodationEmail.EmailType.RESERVATIONS;
            boolean isPrimary = Boolean.TRUE.equals(email.getIsPrimary());
            if (isReservations && isPrimary && reservationsPrimary == null) reservationsPrimary = email;
            if (isReservations && reservations == null) reservations = email;
            if (isPrimary && primary == null) primary = email;
            if (email.getEmailType() == AccommodationEmail.EmailType.GENERAL && general == null) general = email;
        }
        if (reservationsPrimary != null) return reservationsPrimary;
        if (reservations != null) return reservations;
        if (primary != null) return primary;
        if (general != null) return general;
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * "Dear Glory" when the label names a person; "Dear Reservations Team" when it names a job.
     *
     * Greeting a lodge by the wrong word is a small thing. Greeting it as "Dear TWC General
     * Inquiries" is not, so anything job-shaped falls back to the desk.
     */
    private String greetingName(String label) {
        if (label == null || label.isBlank() || label.contains("@")) return "Reservations Team";
        String lower = label.toLowerCase();
        String[] jobWords = {"reserv", "general", "info", "sales", "enquir", "inquir", "booking", "team",
            "office", "management", "support", "billing", "account", "admin", "front", "desk", "contact",
            "manager", "department"};
        for (String word : jobWords) {
            if (lower.contains(word)) return "Reservations Team";
        }
        return label.trim().split("\\s+")[0];
    }

    private String reference(Safari safari) {
        List<String> parts = new ArrayList<>();
        if (safari.getCode() != null) parts.add(safari.getCode());
        if (safari.getName() != null) parts.add(safari.getName());
        return String.join(" · ", parts);
    }

    /** "29 Jan – 1 Feb 2027" for the subject line. */
    private String range(LocalDate from, LocalDate to) {
        if (from.getYear() == to.getYear() && from.getMonth() == to.getMonth()) {
            return from.getDayOfMonth() + "–" + to.format(SHORT);
        }
        return from.format(SHORT) + " – " + to.format(SHORT);
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
