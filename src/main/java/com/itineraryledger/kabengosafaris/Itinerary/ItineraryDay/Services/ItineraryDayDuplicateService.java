package com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Services;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.DTOs.DuplicateItineraryDayDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies a day, with everything on it, one or more times.
 *
 * Three nights in the Serengeti are three days that differ by a sentence — the
 * same park visit, the same fees, the same lodge. Building them by hand is where
 * the time in a 13-day itinerary actually goes, and each retyping is a chance
 * for the fees to drift apart.
 *
 * <p>The copies land directly after the source by default, because "another
 * night here" means next, not last; the days after them are renumbered. The
 * itinerary's day count is a hard ceiling — {@code POST /days} already refuses
 * to exceed {@code totalDays} — so this refuses in the same terms rather than
 * quietly making an itinerary that can never be completed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryDayDuplicateService {

    private static final int MAX_COPIES = 20;

    private final ItineraryDayRepository itineraryDayRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    @AuditLogAnnotation(action = "DUPLICATE_ITINERARY_DAY", description = "Duplicating an itinerary day", entityType = "ItineraryDay")
    public ResponseEntity<ApiResponse<?>> duplicateDay(
        String itineraryIdObfuscated,
        String dayIdObfuscated,
        DuplicateItineraryDayDTO request
    ) {
        try {
            DuplicateItineraryDayDTO options = request != null ? request : new DuplicateItineraryDayDTO();

            Long itineraryId;
            Long dayId;
            try {
                itineraryId = idObfuscator.decodeId(itineraryIdObfuscated);
                dayId = idObfuscator.decodeId(dayIdObfuscated);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid itinerary or day ID", "INVALID_ID"));
            }

            ItineraryDay source = itineraryDayRepository.findById(dayId).orElse(null);
            if (source == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Day not found", "DAY_NOT_FOUND"));
            }
            if (!source.getItinerary().getId().equals(itineraryId)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "That day belongs to a different itinerary", "DAY_ITINERARY_MISMATCH"));
            }

            int copies = options.copyCount();
            if (copies < 1 || copies > MAX_COPIES) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Copies must be between 1 and " + MAX_COPIES, "INVALID_COPY_COUNT"));
            }

            Itinerary itinerary = source.getItinerary();
            List<ItineraryDay> days = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);
            int free = itinerary.getTotalDays() - days.size();

            /*
             * The ceiling is the itinerary's own day count, not an arbitrary
             * limit — an itinerary with more days than it declares can never be
             * completed, so the refusal names both numbers and the way out.
             */
            if (copies > free) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        free <= 0
                            ? "This itinerary is full at " + itinerary.getTotalDays() + " days. Raise its day count first, or remove a day."
                            : "Only " + free + " day slot" + (free == 1 ? "" : "s") + " left of "
                                + itinerary.getTotalDays() + ". Asked for " + copies + ".",
                        "ITINERARY_DAYS_FULL"));
            }

            // appended first, then moved into place — one unique day number at a time
            int nextNumber = days.stream().mapToInt(ItineraryDay::getDayNumber).max().orElse(0) + 1;
            List<ItineraryDay> created = new ArrayList<>();
            Counts counts = new Counts();

            for (int i = 0; i < copies; i++) {
                ItineraryDay copy = copyDay(source, options, counts);
                copy.setItinerary(itinerary);
                copy.setDayNumber(nextNumber++);
                created.add(itineraryDayRepository.save(copy));
            }

            if (options.after()) {
                placeAfterSource(itineraryId, source, created);
            }

            List<Map<String, Object>> summary = created.stream()
                .sorted(Comparator.comparing(ItineraryDay::getDayNumber))
                .map(day -> {
                    Map<String, Object> row = new LinkedHashMap<String, Object>();
                    row.put("id", idObfuscator.encodeId(day.getId()));
                    row.put("dayNumber", day.getDayNumber());
                    row.put("title", day.getTitle());
                    return row;
                })
                .toList();

            log.info("Day {} of itinerary {} duplicated ×{} — {}",
                source.getDayNumber(), itinerary.getCode(), copies, counts);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("days", summary);
            data.put("copies", copies);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, summarise(source, copies, counts), data));

        } catch (Exception e) {
            log.error("Error duplicating day {}", dayIdObfuscated, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to duplicate the day", "DAY_DUPLICATE_FAILED"));
        }
    }

    /**
     * Moves the new days to sit directly behind the source.
     *
     * Day numbers are unique per itinerary, so they cannot be shuffled in place:
     * every day goes negative first and comes back in the intended order — the
     * same two-pass the reorder endpoint uses, for the same reason.
     */
    private void placeAfterSource(Long itineraryId, ItineraryDay source, List<ItineraryDay> created) {
        List<ItineraryDay> all = itineraryDayRepository.findByItineraryIdOrderByDayNumberAsc(itineraryId);

        List<ItineraryDay> ordered = new ArrayList<>();
        for (ItineraryDay day : all) {
            if (created.stream().anyMatch(c -> c.getId().equals(day.getId()))) continue;
            ordered.add(day);
            if (day.getId().equals(source.getId())) ordered.addAll(created);
        }

        int temp = -1;
        for (ItineraryDay day : ordered) day.setDayNumber(temp--);
        itineraryDayRepository.saveAll(ordered);
        itineraryDayRepository.flush();

        int number = 1;
        for (ItineraryDay day : ordered) {
            day.setDayNumber(number++);
            day.setDayTag(null); // regenerated by @PreUpdate
        }
        itineraryDayRepository.saveAll(ordered);
    }

    /** One day and whatever the flags allow to hang off it. */
    private ItineraryDay copyDay(ItineraryDay day, DuplicateItineraryDayDTO options, Counts counts) {
        ItineraryDay copy = ItineraryDay.builder()
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

        if (options.activities()) {
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

    private ItineraryDayPark copyPark(ItineraryDayPark park, DuplicateItineraryDayDTO options, Counts counts) {
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

    private static class Counts {
        int parks, parkActivities, parkTariffs, activities, stays;

        @Override
        public String toString() {
            return parks + " parks, " + parkTariffs + " fees, "
                + (activities + parkActivities) + " activities, " + stays + " stays";
        }
    }

    /** Per copy, not in total — "each with 1 park visit" is what was asked for. */
    private String summarise(ItineraryDay source, int copies, Counts counts) {
        List<String> parts = new ArrayList<>();
        if (counts.parks > 0) parts.add(each(counts.parks, copies, "park visit", "park visits"));
        if (counts.parkTariffs > 0) parts.add(each(counts.parkTariffs, copies, "fee", "fees"));
        int activities = counts.activities + counts.parkActivities;
        if (activities > 0) parts.add(each(activities, copies, "activity", "activities"));
        if (counts.stays > 0) parts.add(each(counts.stays, copies, "stay", "stays"));

        return "Day " + source.getDayNumber() + " copied "
            + (copies == 1 ? "once" : copies + " times")
            + (parts.isEmpty() ? " with nothing on it" : ", each with " + String.join(", ", parts))
            + ".";
    }

    private String each(int total, int copies, String one, String many) {
        int per = total / Math.max(copies, 1);
        return per + " " + (per == 1 ? one : many);
    }
}
