package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.Activity.ActivityRepository;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.ActivityTariffRate;
import com.itineraryledger.kabengosafaris.ActivityTariffRate.Repositories.ActivityTariffRateRepository;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ImageFiles;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ReferenceResolver;
import com.itineraryledger.kabengosafaris.Activity.Entities.ActivityImage;
import com.itineraryledger.kabengosafaris.Activity.Repositories.ActivityImageRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Season.Season;

import lombok.RequiredArgsConstructor;

/**
 * Activities and what they cost.
 *
 * Nearly the same shape as parks, with one difference that decides the order everything runs in: an
 * activity rate may be PARK-SPECIFIC — a balloon flight over the Serengeti is not priced like a
 * balloon flight anywhere else — so the parks have to be in place before these are read, or every
 * park-specific rate reports a park it cannot find.
 *
 * A rate with no park is the general price for the activity, and that null is meaningful rather than
 * missing: it is the difference between "this is what it costs" and "this is what it costs there".
 */
@Component
@RequiredArgsConstructor
public class ActivityTransfer implements ModuleTransfer {

    private final ActivityRepository activities;
    private final ActivityTariffRateRepository rates;
    private final ActivityImageRepository activityImages;
    private final ImageFiles imageFiles;
    private final ReferenceResolver resolver;
    private final ObjectMapper mapper;

    @Override public String name() { return "activities"; }
    @Override public String label() { return "Activities and their rates"; }
    @Override public int order() { return 50; }
    @Override public long count() { return activities.count(); }

    @Override
    public String detail() {
        long total = rates.count();
        return total == 0 ? "no rates yet" : String.format("%,d rate%s", total, total == 1 ? "" : "s");
    }

    @Override
    public List<String> requires() {
        /* parks, because a rate may be priced for one */
        return List.of("tariffs", "pax-categories", "seasons", "parks");
    }

    private void name(ObjectNode node, String field, String value) {
        if (value == null) node.putNull(field); else node.put(field, value);
    }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();

        for (Activity activity : activities.findAll()) {
            ObjectNode node = Scalars.of(mapper, activity);

            ArrayNode rateRows = node.putArray("rates");
            for (ActivityTariffRate rate : rates.findByActivityId(activity.getId())) {
                ObjectNode row = Scalars.of(mapper, rate);
                /* null park means "anywhere", and has to survive as an explicit null */
                name(row, "park", rate.getPark() == null ? null : rate.getPark().getSlug());
                name(row, "season", rate.getSeason() == null ? null : rate.getSeason().getName());
                name(row, "nationCategory",
                    rate.getNationCategory() == null ? null : rate.getNationCategory().getName());
                name(row, "ageCategory",
                    rate.getAgeCategory() == null ? null : rate.getAgeCategory().getName());
                rateRows.add(row);
            }

            /* the gallery, only when asked for — it dwarfs the rates otherwise */
            imageFiles.attach(mapper, node, "activities", activity.getSlug(),
                activityImages.findByActivityIdOrderByDisplayOrderAsc(activity.getId()), includeImages, files);

            rows.add(node);
        }
        return rows;
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data) {
            String slug = row.path("slug").asText(null);
            String label = row.path("name").asText(slug);
            if (slug == null || slug.isBlank()) {
                outcome.unresolved(label, "the activity has no slug, so nothing can be matched on");
                continue;
            }

            Activity activity = activities.findBySlug(slug).orElse(null);
            if (activity == null) {
                activity = new Activity();
                Scalars.apply(mapper, row, activity);
                activity.setSlug(slug);
                activity = activities.save(activity);
                outcome.created();
            } else if (context.mayOverwrite()) {
                Scalars.apply(mapper, row, activity, "slug");
                activity = activities.save(activity);
                outcome.updated();
            } else {
                outcome.skip(label, "already here");
            }

            applyImages(row, activity, context);
            applyRates(row, activity, context);
        }
    }

    private void applyRates(JsonNode row, Activity activity, TransferContext context) {
        var outcome = context.getReport().forModule("activity-rates");

        for (JsonNode rateRow : row.path("rates")) {
            String parkSlug = rateRow.path("park").isNull() ? null : rateRow.path("park").asText(null);
            String seasonName = rateRow.path("season").asText(null);
            String nationName = rateRow.path("nationCategory").asText(null);
            String ageName = rateRow.path("ageCategory").isNull()
                ? null : rateRow.path("ageCategory").asText(null);

            String key = activity.getName() + (parkSlug == null ? "" : " @ " + parkSlug)
                + " · " + seasonName + " · " + nationName + (ageName == null ? "" : " · " + ageName);

            /* a NAMED park that is missing is a fault; no park at all is the general price */
            Park park = parkSlug == null ? null : resolver.park(context, parkSlug);
            if (parkSlug != null && park == null) {
                outcome.unresolved(key, "no park '" + parkSlug + "'");
                continue;
            }
            Season season = resolver.globalSeason(context, seasonName);
            if (season == null) {
                outcome.unresolved(key, "no season '" + seasonName + "'");
                continue;
            }
            var nation = resolver.nation(context, nationName);
            if (nation == null) {
                outcome.unresolved(key, "no guest residency '" + nationName + "'");
                continue;
            }
            var age = ageName == null ? null : resolver.age(context, ageName);
            if (ageName != null && age == null) {
                outcome.unresolved(key, "no age band '" + ageName + "'");
                continue;
            }

            /* One read per activity, not one per rate — see ParkTransfer.ratesAlreadyHere. */
            String rateKey = (park == null ? "-" : park.getId()) + "/" + season.getId()
                + "/" + nation.getId() + "/" + (age == null ? "-" : age.getId());
            ActivityTariffRate existing = ratesAlreadyHere(context, activity).get(rateKey);

            if (existing != null && !context.mayOverwrite()) {
                outcome.skip(key, "already here");
                continue;
            }

            ActivityTariffRate rate = existing == null ? new ActivityTariffRate() : existing;
            Scalars.apply(mapper, rateRow, rate);
            rate.setActivity(activity);
            rate.setPark(park);
            rate.setSeason(season);
            rate.setNationCategory(nation);
            rate.setAgeCategory(age);
            rates.save(rate);
            ratesAlreadyHere(context, activity).put(rateKey, rate);

            if (existing == null) outcome.created(); else outcome.updated();
        }
    }

    /**
     * The gallery, when the bundle carried one.
     *
     * A row is written only once its FILE has landed, and never the other way round: an image record
     * pointing at a file that is not on this disk renders as a broken box on the website, which is
     * worse than the picture simply being absent. Matched on the stored file name, which already
     * carries a content hash, so re-importing the same bundle does not stack duplicates.
     */
    private void applyImages(JsonNode row, Activity owner, TransferContext context) {
        JsonNode images = row.path("images");
        if (!images.isArray() || images.isEmpty()) return;

        var outcome = context.getReport().forModule("activities-images");
        var existing = activityImages.findByActivityIdOrderByDisplayOrderAsc(owner.getId()).stream()
            .map(ActivityImage::getFileName).collect(java.util.stream.Collectors.toSet());

        for (JsonNode imageRow : images) {
            String fileName = imageFiles.place(context, "activities", imageRow);
            if (fileName == null) {
                outcome.unresolved(imageRow.path("fileName").asText("(unnamed)"),
                    "the bundle was exported without its files");
                continue;
            }
            if (existing.contains(fileName)) {
                outcome.skip(fileName, "already here");
                continue;
            }
            ActivityImage image = new ActivityImage();
            Scalars.apply(mapper, imageRow, image, "file");
            image.setActivity(owner);
            activityImages.save(image);
            outcome.created();
        }
    }

    /** Every rate this activity already has, by composite key, read once per activity. */
    private Map<String, ActivityTariffRate> ratesAlreadyHere(
            TransferContext context, Activity activity) {
        return context.cached("activity-rate-index", String.valueOf(activity.getId()), cacheKey -> {
            Map<String, ActivityTariffRate> index = new HashMap<>();
            for (ActivityTariffRate rate : rates.findByActivityId(activity.getId())) {
                index.put((rate.getPark() == null ? "-" : rate.getPark().getId())
                    + "/" + (rate.getSeason() == null ? null : rate.getSeason().getId())
                    + "/" + (rate.getNationCategory() == null ? null : rate.getNationCategory().getId())
                    + "/" + (rate.getAgeCategory() == null ? "-" : rate.getAgeCategory().getId()), rate);
            }
            return index;
        });
    }
}
