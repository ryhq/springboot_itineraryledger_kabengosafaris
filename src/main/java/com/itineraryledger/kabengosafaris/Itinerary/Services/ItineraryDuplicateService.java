package com.itineraryledger.kabengosafaris.Itinerary.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.DTOs.DuplicateItineraryDTO;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary.ItineraryStatus;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies a finished itinerary into a fresh draft.
 *
 * A catalogue is built by variation, not from scratch — the fly-out version of
 * the 6-day classic differs by one day. Without this, that means retyping every
 * day, park, tariff, activity and stay, and the copy silently drifts from the
 * original.
 *
 * <p><b>One itinerary per call, and only a finished one.</b> The source must be
 * COMPLETE or PUBLISHED: those are the states that guarantee a full day list and
 * at least one pax band, so the copy is a working draft rather than an
 * inheritance of somebody's half-finished edit. A draft has nothing settled
 * worth copying; an archived one was deliberately retired.
 *
 * <p><b>The copy is always a DRAFT</b>, whatever the source was. It has a new
 * name and no history of being reviewed, so publishing it is a decision someone
 * has to make again.
 *
 * <p>Depth is chosen per call — see {@link DuplicateItineraryDTO}. What is never
 * copied is images and documents: those are files on disk referenced by row, and
 * a copy pointing at the originals would delete the originals' files when it was
 * deleted. The response says what was copied AND what was left out, so a flag
 * cannot quietly produce an empty itinerary.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryDuplicateService {

    /** '<name> (Copy)' has to fit the column, and the suffix is the part that matters. */
    private static final int NAME_LIMIT = 200;
    /** enough attempts to get past a run of earlier copies; past that, ask for a name */
    private static final int MAX_NAME_ATTEMPTS = 50;

    private final ItineraryRepository itineraryRepository;
    private final ItineraryGetService itineraryGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "DUPLICATE_ITINERARY", description = "Duplicating an itinerary", entityType = "Itinerary")
    public ResponseEntity<ApiResponse<?>> duplicateItinerary(String idObfuscated, DuplicateItineraryDTO request) {
        log.info("Duplicating itinerary: {}", idObfuscated);

        try {
            DuplicateItineraryDTO options = request != null ? request : new DuplicateItineraryDTO();

            Long id = idObfuscator.decodeId(idObfuscated);
            Itinerary source = itineraryRepository.findById(id).orElse(null);

            if (source == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
            }

            /*
             * The state gate. Both refusals name the state the caller is in and the
             * move that gets them out of it, because "cannot duplicate" alone leaves
             * them guessing.
             */
            if (source.getStatus() == ItineraryStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Only a complete or published itinerary can be duplicated. This one is still a draft — finish it and mark it complete first.",
                        "ITINERARY_NOT_FINISHED"));
            }
            if (source.getStatus() == ItineraryStatus.ARCHIVED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "This itinerary is archived. Unarchive it first if you want to build a new one from it.",
                        "ITINERARY_ARCHIVED"));
            }

            String name = resolveName(source, options);
            if (name == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Could not name the copy — too many itineraries already share this name. Send a name with the request.",
                        "ITINERARY_NAME_EXISTS"));
            }

            Itinerary copy = Itinerary.builder()
                .name(name)
                // a copy always starts as a draft: it is a starting point, not a product
                .status(ItineraryStatus.DRAFT)
                .tripType(source.getTripType())
                .budgetCategory(source.getBudgetCategory())
                /*
                 * totalDays is kept even when the days are not copied: it is the
                 * shape the template intends, and the copy cannot be completed
                 * until that many days exist. Zeroing it would lose the target.
                 */
                .totalDays(source.getTotalDays())
                .totalNights(source.getTotalNights())
                .carCount(source.getCarCount())
                .description(source.getDescription())
                .highlights(source.getHighlights())
                .inclusions(source.getInclusions())
                .exclusions(source.getExclusions())
                .startLocation(source.getStartLocation())
                .endLocation(source.getEndLocation())
                .isActive(true)
                .internalNotes(source.getInternalNotes())
                // images are not copied, so the copy cannot claim one of them as primary
                .createdBy(source.getCreatedBy())
                .build();

            Counts counts = new Counts();

            if (options.days()) {
                for (ItineraryDay day : source.getDays()) {
                    copy.addDay(copyDay(day, options, counts));
                    counts.days++;
                }
            }

            if (options.pax()) {
                for (ItineraryPax pax : source.getPaxList()) {
                    copy.addPax(ItineraryPax.builder()
                        .nationCategory(pax.getNationCategory())
                        .ageCategory(pax.getAgeCategory())
                        .count(pax.getCount())
                        .notes(pax.getNotes())
                        .build());
                    counts.paxBands++;
                }
            }

            // saved once for the id the code is built from, then again with the code
            Itinerary saved = itineraryRepository.save(copy);
            saved.setCode(saved.generateCode());
            saved = itineraryRepository.save(saved);

            log.info("Itinerary {} duplicated as {} — {}", source.getCode(), saved.getCode(), counts);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, summarise(counts, options, source),
                    itineraryGetService.convertToDTO(saved)));

        } catch (Exception e) {
            log.error("Error duplicating itinerary: {}", idObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to duplicate itinerary", "ITINERARY_DUPLICATE_FAILED"));
        }
    }

    /* ------------------------------- copying -------------------------------- */

    /** One day and whatever the flags allow to hang off it. Both sides of each link are set. */
    private ItineraryDay copyDay(ItineraryDay day, DuplicateItineraryDTO options, Counts counts) {
        ItineraryDay copy = ItineraryDay.builder()
            .dayNumber(day.getDayNumber())
            .dayTag(day.getDayTag())
            .title(day.getTitle())
            .description(day.getDescription())
            .morningActivities(day.getMorningActivities())
            .afternoonActivities(day.getAfternoonActivities())
            .eveningActivities(day.getEveningActivities())
            .wildlifeHighlights(day.getWildlifeHighlights())
            .scenicHighlights(day.getScenicHighlights())
            .specialNotes(day.getSpecialNotes())
            .startLocation(day.getStartLocation())
            .endLocation(day.getEndLocation())
            .distanceKm(day.getDistanceKm())
            .isOvernight(day.getIsOvernight())
            .mealsIncluded(day.getMealsIncluded())
            .internalNotes(day.getInternalNotes())
            .build();

        // standalone activities are part of what a day says it is; they follow it
        for (ItineraryDayActivity activity : day.getActivities()) {
            copy.addActivity(ItineraryDayActivity.builder()
                .activity(activity.getActivity())
                .sortOrder(activity.getSortOrder())
                .durationHours(activity.getDurationHours())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .notes(activity.getNotes())
                .isIncludedInPrice(activity.getIsIncludedInPrice())
                .isOptional(activity.getIsOptional())
                .build());
            counts.activities++;
        }

        if (options.parks()) {
            for (ItineraryDayPark park : day.getParks()) {
                copy.addPark(copyPark(park, options, counts));
                counts.parks++;
            }
        }

        if (options.accommodations()) {
            for (ItineraryDayAccommodation stay : day.getAccommodations()) {
                copy.addAccommodation(ItineraryDayAccommodation.builder()
                    .accommodation(stay.getAccommodation())
                    .roomType(stay.getRoomType())
                    .roomStandard(stay.getRoomStandard())
                    .boardType(stay.getBoardType())
                    .roomCount(stay.getRoomCount())
                    .isAlternative(stay.getIsAlternative())
                    .notes(stay.getNotes())
                    .build());
                counts.stays++;
            }
        }

        return copy;
    }

    /** A park visit, with the activities chosen for it and — if asked — its fee categories. */
    private ItineraryDayPark copyPark(ItineraryDayPark park, DuplicateItineraryDTO options, Counts counts) {
        ItineraryDayPark copy = ItineraryDayPark.builder()
            .park(park.getPark())
            .entryType(park.getEntryType())
            .sortOrder(park.getSortOrder())
            .arrivalTime(park.getArrivalTime())
            .departureTime(park.getDepartureTime())
            .notes(park.getNotes())
            .build();

        for (ItineraryDayParkActivity activity : park.getParkActivities()) {
            ItineraryDayParkActivity activityCopy = ItineraryDayParkActivity.builder()
                .parkActivity(activity.getParkActivity())
                .sortOrder(activity.getSortOrder())
                .durationHours(activity.getDurationHours())
                .startTime(activity.getStartTime())
                .endTime(activity.getEndTime())
                .notes(activity.getNotes())
                .isIncludedInPrice(activity.getIsIncludedInPrice())
                .build();
            activityCopy.setItineraryDayPark(copy);
            copy.getParkActivities().add(activityCopy);
            counts.parkActivities++;
        }

        if (options.parkTariffs()) {
            for (ItineraryDayParkTariff tariff : park.getParkTariffs()) {
                ItineraryDayParkTariff tariffCopy = ItineraryDayParkTariff.builder()
                    .parkTariff(tariff.getParkTariff())
                    .notes(tariff.getNotes())
                    .isIncludedInPrice(tariff.getIsIncludedInPrice())
                    .build();
                tariffCopy.setItineraryDayPark(copy);
                copy.getParkTariffs().add(tariffCopy);
                counts.parkTariffs++;
            }
        }

        return copy;
    }

    /* -------------------------------- naming -------------------------------- */

    /**
     * A free name for the copy: the caller's, else '<name> (Copy)', then
     * '(Copy 2)' and up. Names are unique on this table, so the alternative to
     * searching is a 400 the caller can do nothing about.
     */
    private String resolveName(Itinerary source, DuplicateItineraryDTO request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            String requested = request.getName().trim();
            return itineraryRepository.existsByNameIgnoreCase(requested) ? null : requested;
        }
        for (int attempt = 1; attempt <= MAX_NAME_ATTEMPTS; attempt++) {
            String suffix = attempt == 1 ? " (Copy)" : " (Copy " + attempt + ")";
            String base = source.getName();
            if (base.length() + suffix.length() > NAME_LIMIT) {
                base = base.substring(0, NAME_LIMIT - suffix.length()).trim();
            }
            String candidate = base + suffix;
            if (!itineraryRepository.existsByNameIgnoreCase(candidate)) return candidate;
        }
        return null;
    }

    /* ------------------------------- reporting ------------------------------ */

    private static class Counts {
        int days, parks, parkActivities, parkTariffs, activities, stays, paxBands;

        @Override
        public String toString() {
            return days + " days, " + parks + " park visits, " + parkTariffs + " tariffs, "
                + (activities + parkActivities) + " activities, " + stays + " stays, "
                + paxBands + " pax bands";
        }
    }

    /**
     * What the copy got, and what it deliberately did not.
     *
     * The omissions are the half that matters: a copy made without its
     * accommodations looks finished until someone quotes from it.
     */
    private String summarise(Counts counts, DuplicateItineraryDTO options, Itinerary source) {
        List<String> copied = new ArrayList<>();
        if (counts.days > 0) copied.add(plural(counts.days, "day", "days"));
        if (counts.parks > 0) copied.add(plural(counts.parks, "park visit", "park visits"));
        if (counts.parkTariffs > 0) copied.add(plural(counts.parkTariffs, "park fee", "park fees"));
        int activities = counts.activities + counts.parkActivities;
        if (activities > 0) copied.add(plural(activities, "activity", "activities"));
        if (counts.stays > 0) copied.add(plural(counts.stays, "stay", "stays"));
        if (counts.paxBands > 0) copied.add(plural(counts.paxBands, "pax band", "pax bands"));

        List<String> left = new ArrayList<>();
        if (!options.days() && !source.getDays().isEmpty()) left.add("the days");
        if (options.days() && !options.parks()) left.add("the park visits");
        if (options.parks() && !options.parkTariffs()) left.add("the park fees");
        if (options.days() && !options.accommodations()) left.add("the accommodations");
        if (!options.pax() && !source.getPaxList().isEmpty()) left.add("the pax split");
        left.add("images and documents");

        return "Copied as a draft"
            + (copied.isEmpty() ? " with no contents" : " with " + join(copied))
            + ". Not copied: " + join(left) + ".";
    }

    private String plural(int n, String one, String many) {
        return n + " " + (n == 1 ? one : many);
    }

    /** 'a, b and c' — a list a person would read aloud. */
    private String join(List<String> parts) {
        if (parts.size() == 1) return parts.get(0);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
    }
}
