package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ReferenceResolver;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Entity.ItineraryDay;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.Repository.ItineraryDayRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Entity.ItineraryDayAccommodation;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayAccommodation.Repository.ItineraryDayAccommodationRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Entity.ItineraryDayActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayActivity.Repository.ItineraryDayActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Entity.ItineraryDayPark;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.Repository.ItineraryDayParkRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Entity.ItineraryDayParkActivity;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkActivity.Repository.ItineraryDayParkActivityRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Entity.ItineraryDayParkTariff;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryDay.ItineraryDayPark.ItineraryDayParkTariff.Repository.ItineraryDayParkTariffRepository;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Entity.ItineraryPax;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryPax.Repository.ItineraryPaxRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Itineraries, with their days and everything hanging off them.
 *
 * The first thing in the bundle with real depth. An accommodation goes three levels down; an
 * itinerary goes five, and it is the first module where a partial import produces something that
 * LOOKS complete and is not: five days of six, or a day whose lodge quietly failed to resolve, is a
 * product somebody will quote from.
 *
 * So an itinerary is all or nothing. Anything it names that this company has not got, and the whole
 * itinerary is refused with the missing reference named. A rate row can be skipped on its own
 * because the other 2,663 still make a rate sheet; a day cannot.
 *
 * WHAT DOES NOT TRAVEL, deliberately
 *
 *  - Prices. The structure travels and the cost is derived from THIS company's rates, so the same
 *    six day trip is priced in Kabengo's money in Kabengo and Jatelo's in Jatelo. That is the point.
 *  - Status. Everything arrives as a DRAFT. A published itinerary is a statement that this company
 *    has checked it and will sell it, which is not a thing another company can assert on its behalf.
 *
 * IDENTITY
 *
 * The code, which is the only stable name an itinerary has. Same code and same name means the same
 * itinerary and it is left alone. Same code and a DIFFERENT name means the code has been taken
 * locally by something else, which is easy to arrange because codes are derived from a row id: the
 * itinerary is still imported, the local generator gives it a fresh code, and the report says so
 * rather than leaving somebody to notice.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItineraryTransfer implements ModuleTransfer {

    private final ItineraryRepository itineraries;
    private final ItineraryDayRepository days;
    private final ItineraryDayParkRepository dayParks;
    private final ItineraryDayParkTariffRepository dayParkTariffs;
    private final ItineraryDayParkActivityRepository dayParkActivities;
    private final ItineraryDayActivityRepository dayActivities;
    private final ItineraryDayAccommodationRepository dayAccommodations;
    private final ItineraryPaxRepository pax;
    private final ParkTariffRepository parkTariffs;
    private final ParkActivityRepository parkActivities;
    private final AccommodationRepository accommodations;
    private final AccommodationRoomTypeRepository roomTypes;
    private final AccommodationRoomStandardRepository roomStandards;
    private final AccommodationBoardTypeRepository boardTypes;
    private final ReferenceResolver resolver;
    private final ObjectMapper mapper;

    @Override public String name() { return "itineraries"; }
    @Override public String label() { return "Itineraries and their days"; }
    @Override public int order() { return 70; }
    @Override public long count() { return itineraries.count(); }

    @Override
    public List<String> requires() {
        return List.of("tariffs", "pax-categories", "seasons", "parks", "activities",
            "park-activities", "accommodations");
    }

    @Override
    public String detail() {
        long n = days.count();
        return n + (n == 1 ? " day" : " days");
    }

    // ---- Export --------------------------------------------------------------------------------

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();
        for (Itinerary itinerary : itineraries.findAll()) {
            ObjectNode row = Scalars.of(mapper, itinerary);
            row.put("code", itinerary.getCode());

            ArrayNode paxRows = row.putArray("pax");
            for (ItineraryPax p : pax.findByItineraryId(itinerary.getId())) {
                ObjectNode paxRow = Scalars.of(mapper, p);
                paxRow.put("nation", p.getNationCategory() == null ? null : p.getNationCategory().getName());
                paxRow.put("age", p.getAgeCategory() == null ? null : p.getAgeCategory().getName());
                paxRows.add(paxRow);
            }

            ArrayNode dayRows = row.putArray("days");
            for (ItineraryDay day : days.findByItineraryIdOrderByDayNumberAsc(itinerary.getId())) {
                ObjectNode dayRow = Scalars.of(mapper, day);

                ArrayNode parkRows = dayRow.putArray("parks");
                for (ItineraryDayPark visit : dayParks.findByItineraryDayIdOrderBySortOrderAsc(day.getId())) {
                    ObjectNode visitRow = Scalars.of(mapper, visit);
                    visitRow.put("park", visit.getPark() == null ? null : visit.getPark().getSlug());

                    ArrayNode feeRows = visitRow.putArray("tariffs");
                    for (ItineraryDayParkTariff fee : dayParkTariffs.findByItineraryDayParkId(visit.getId())) {
                        ObjectNode feeRow = Scalars.of(mapper, fee);
                        Tariff tariff = fee.getParkTariff() == null ? null : fee.getParkTariff().getTariff();
                        feeRow.put("tariff", tariff == null ? null : tariff.getSlug());
                        feeRows.add(feeRow);
                    }

                    ArrayNode inParkRows = visitRow.putArray("activities");
                    for (ItineraryDayParkActivity act
                            : dayParkActivities.findByItineraryDayParkIdOrderBySortOrderAsc(visit.getId())) {
                        ObjectNode actRow = Scalars.of(mapper, act);
                        Activity activity = act.getParkActivity() == null ? null : act.getParkActivity().getActivity();
                        actRow.put("activity", activity == null ? null : activity.getSlug());
                        inParkRows.add(actRow);
                    }
                    parkRows.add(visitRow);
                }

                ArrayNode actRows = dayRow.putArray("activities");
                for (ItineraryDayActivity act : dayActivities.findByItineraryDayIdOrderBySortOrderAsc(day.getId())) {
                    ObjectNode actRow = Scalars.of(mapper, act);
                    actRow.put("activity", act.getActivity() == null ? null : act.getActivity().getSlug());
                    actRows.add(actRow);
                }

                ArrayNode stayRows = dayRow.putArray("accommodations");
                for (ItineraryDayAccommodation stay : dayAccommodations.findByItineraryDayId(day.getId())) {
                    ObjectNode stayRow = Scalars.of(mapper, stay);
                    stayRow.put("accommodation", stay.getAccommodation() == null ? null : stay.getAccommodation().getSlug());
                    stayRow.put("roomType", stay.getRoomType() == null ? null : stay.getRoomType().getName());
                    stayRow.put("roomStandard", stay.getRoomStandard() == null ? null : stay.getRoomStandard().getName());
                    stayRow.put("boardType", stay.getBoardType() == null ? null : stay.getBoardType().getName());
                    stayRows.add(stayRow);
                }

                dayRows.add(dayRow);
            }
            rows.add(row);
        }
        return rows;
    }

    // ---- Import --------------------------------------------------------------------------------

    /** Raised as soon as anything an itinerary names is missing, so nothing half-built is written. */
    private static final class Missing extends RuntimeException {
        Missing(String what) { super(what); }
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data) {
            String code = row.path("code").asText(null);
            String label = row.path("name").asText(code);

            Itinerary existing = code == null ? null : itineraries.findByCode(code).orElse(null);
            boolean codeTakenByAnother = existing != null && !label.equals(existing.getName());

            if (existing != null && !codeTakenByAnother) {
                if (!context.mayOverwrite()) {
                    outcome.skip(label, "already here as " + code);
                    continue;
                }
                /*
                 * Replacing the tree rather than reconciling it. A day that moved from third to
                 * second has no identity of its own to match on, and a half-reconciled itinerary
                 * is the failure this module exists to avoid.
                 */
                days.deleteAll(days.findByItineraryIdOrderByDayNumberAsc(existing.getId()));
                pax.deleteAll(pax.findByItineraryId(existing.getId()));
            }

            try {
                Itinerary target = existing != null && !codeTakenByAnother ? existing : new Itinerary();
                Scalars.apply(mapper, row, target, "code", "status");
                /* Never published on arrival: see the class note. */
                target.setStatus(Itinerary.ItineraryStatus.DRAFT);
                target = itineraries.save(target);

                if (target.getCode() == null) {
                    target.setCode(codeTakenByAnother || code == null ? target.generateCode() : code);
                    target = itineraries.save(target);
                }

                writePax(row, target, context);
                writeDays(row, target, context);

                if (existing == null) {
                    if (codeTakenByAnother) {
                        outcome.created();
                        outcome.skip(label, "the code " + code + " is used here by \"" + existing.getName()
                            + "\", so this one was given " + target.getCode());
                    } else {
                        outcome.created();
                    }
                } else if (codeTakenByAnother) {
                    outcome.created();
                    outcome.skip(label, "the code " + code + " is used here by \"" + existing.getName()
                        + "\", so this one was given " + target.getCode());
                } else {
                    outcome.updated();
                }
            } catch (Missing missing) {
                outcome.unresolved(label, missing.getMessage());
            }
        }
    }

    private void writePax(JsonNode row, Itinerary itinerary, TransferContext context) {
        for (JsonNode p : row.path("pax")) {
            String nation = p.path("nation").asText(null);
            String age = p.path("age").asText(null);
            var nationCategory = resolver.nation(context, nation);
            if (nationCategory == null) throw new Missing("no guest residency called '" + nation + "' here");
            var ageCategory = resolver.age(context, age);
            if (ageCategory == null) throw new Missing("no guest age band called '" + age + "' here");

            ItineraryPax entry = new ItineraryPax();
            Scalars.apply(mapper, p, entry, "nation", "age");
            entry.setItinerary(itinerary);
            entry.setNationCategory(nationCategory);
            entry.setAgeCategory(ageCategory);
            pax.save(entry);
        }
    }

    private void writeDays(JsonNode row, Itinerary itinerary, TransferContext context) {
        for (JsonNode dayRow : row.path("days")) {
            ItineraryDay day = new ItineraryDay();
            Scalars.apply(mapper, dayRow, day, "parks", "activities", "accommodations");
            day.setItinerary(itinerary);
            day = days.save(day);

            for (JsonNode visitRow : dayRow.path("parks")) {
                String parkSlug = visitRow.path("park").asText(null);
                Park park = resolver.park(context, parkSlug);
                if (park == null) throw new Missing("no park '" + parkSlug + "' here");

                ItineraryDayPark visit = new ItineraryDayPark();
                Scalars.apply(mapper, visitRow, visit, "park", "tariffs", "activities");
                visit.setItineraryDay(day);
                visit.setPark(park);
                visit = dayParks.save(visit);

                for (JsonNode feeRow : visitRow.path("tariffs")) {
                    String tariffSlug = feeRow.path("tariff").asText(null);
                    Tariff tariff = resolver.tariff(context, tariffSlug);
                    if (tariff == null) throw new Missing("no tariff '" + tariffSlug + "' here");
                    ParkTariff link = parkTariffs
                        .findByParkIdAndTariffId(park.getId(), tariff.getId()).orElse(null);
                    if (link == null) {
                        throw new Missing(parkSlug + " does not charge '" + tariffSlug + "' here");
                    }
                    ItineraryDayParkTariff fee = new ItineraryDayParkTariff();
                    Scalars.apply(mapper, feeRow, fee, "tariff");
                    fee.setItineraryDayPark(visit);
                    fee.setParkTariff(link);
                    dayParkTariffs.save(fee);
                }

                for (JsonNode actRow : visitRow.path("activities")) {
                    String activitySlug = actRow.path("activity").asText(null);
                    Activity activity = resolver.activity(context, activitySlug);
                    if (activity == null) throw new Missing("no activity '" + activitySlug + "' here");
                    ParkActivity link = parkActivities
                        .findByParkIdAndActivityId(park.getId(), activity.getId()).orElse(null);
                    /*
                     * This is the link the park-activities module carries. Without it the panel's
                     * own endpoint answers 200 and stores nothing, so refusing loudly here is the
                     * whole improvement.
                     */
                    if (link == null) {
                        throw new Missing(parkSlug + " does not offer '" + activitySlug
                            + "' here; the park-activities module carries that link");
                    }
                    ItineraryDayParkActivity act = new ItineraryDayParkActivity();
                    Scalars.apply(mapper, actRow, act, "activity");
                    act.setItineraryDayPark(visit);
                    act.setParkActivity(link);
                    dayParkActivities.save(act);
                }
            }

            for (JsonNode actRow : dayRow.path("activities")) {
                String activitySlug = actRow.path("activity").asText(null);
                Activity activity = resolver.activity(context, activitySlug);
                if (activity == null) throw new Missing("no activity '" + activitySlug + "' here");
                ItineraryDayActivity act = new ItineraryDayActivity();
                Scalars.apply(mapper, actRow, act, "activity");
                act.setItineraryDay(day);
                act.setActivity(activity);
                dayActivities.save(act);
            }

            for (JsonNode stayRow : dayRow.path("accommodations")) {
                String lodgeSlug = stayRow.path("accommodation").asText(null);
                Accommodation lodge = accommodations.findBySlug(lodgeSlug).orElse(null);
                if (lodge == null) throw new Missing("no accommodation '" + lodgeSlug + "' here");

                String typeName = stayRow.path("roomType").asText(null);
                String standardName = stayRow.path("roomStandard").asText(null);
                String boardName = stayRow.path("boardType").asText(null);
                AccommodationRoomType type = roomTypes
                    .findByAccommodationIdAndName(lodge.getId(), typeName).orElse(null);
                if (type == null) throw new Missing(lodgeSlug + " has no room type '" + typeName + "' here");
                AccommodationRoomStandard standard = roomStandards
                    .findByAccommodationIdAndName(lodge.getId(), standardName).orElse(null);
                if (standard == null) throw new Missing(lodgeSlug + " has no room standard '" + standardName + "' here");
                AccommodationBoardType board = boardTypes
                    .findByAccommodationIdAndName(lodge.getId(), boardName).orElse(null);
                if (board == null) throw new Missing(lodgeSlug + " has no board type '" + boardName + "' here");

                ItineraryDayAccommodation stay = new ItineraryDayAccommodation();
                Scalars.apply(mapper, stayRow, stay, "accommodation", "roomType", "roomStandard", "boardType");
                stay.setItineraryDay(day);
                stay.setAccommodation(lodge);
                stay.setRoomType(type);
                stay.setRoomStandard(standard);
                stay.setBoardType(board);
                dayAccommodations.save(stay);
            }
        }
    }
}
