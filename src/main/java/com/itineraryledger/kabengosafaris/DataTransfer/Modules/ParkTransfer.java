package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ImageFiles;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ReferenceResolver;
import com.itineraryledger.kabengosafaris.Park.Entities.ParkImage;
import com.itineraryledger.kabengosafaris.Park.Repositories.ParkImageRepository;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.Park.ParkRepository;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariff;
import com.itineraryledger.kabengosafaris.ParkTariff.ParkTariffRepository;
import com.itineraryledger.kabengosafaris.ParkTariffRate.ParkTariffRate;
import com.itineraryledger.kabengosafaris.ParkTariffRate.Repositories.ParkTariffRateRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Tariff.Tariff;

import lombok.RequiredArgsConstructor;

/**
 * Parks, the tariffs each one charges, and the rates for them.
 *
 * The awkward shape of the three, and the reason this one was built first. A park rate is identified
 * by five things — park, tariff, season, residency, age band — and only the first is inside this
 * document. The other four are looked up by name in the company being imported into, which is the
 * whole trick: an id from the source installation is meaningless here (they are per-installation and
 * rotate on restart), so a name is the only thing that survives the journey.
 *
 * Rates are written one at a time rather than in a batch on purpose. A batch that fails tells you
 * nothing about which of 2,600 rows was wrong; a rate that cannot find its season is reported by
 * name, and the other 2,599 still land.
 */
@Component
@RequiredArgsConstructor
public class ParkTransfer implements ModuleTransfer {

    private final ParkRepository parks;
    private final ParkTariffRepository parkTariffs;
    private final ParkTariffRateRepository rates;
    private final ParkImageRepository parkImages;
    private final ImageFiles imageFiles;
    private final ReferenceResolver resolver;
    private final ObjectMapper mapper;

    @Override public String name() { return "parks"; }
    @Override public String label() { return "Parks, their tariffs and rates"; }
    @Override public int order() { return 40; }
    @Override public long count() { return parks.count(); }

    @Override
    public String detail() {
        long total = rates.count();
        return total == 0 ? "no rates yet" : String.format("%,d rate%s", total, total == 1 ? "" : "s");
    }

    @Override
    public List<String> requires() {
        /* a rate is made of these; a park without them arrives with nowhere to put its numbers */
        return List.of("tariffs", "pax-categories", "seasons");
    }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();

        for (Park park : parks.findAll()) {
            ObjectNode node = Scalars.of(mapper, park, "primaryImage");

            /* which tariffs this park charges, by the tariff's slug */
            ArrayNode charges = node.putArray("tariffs");
            for (ParkTariff link : parkTariffs.findByParkId(park.getId())) {
                ObjectNode charge = mapper.createObjectNode();
                charge.put("tariff", link.getTariff().getSlug());
                if (link.getNotes() != null) charge.put("notes", link.getNotes());
                charges.add(charge);
            }

            ArrayNode rateRows = node.putArray("rates");
            for (ParkTariffRate rate : rates.findByParkId(park.getId())) {
                ObjectNode row = Scalars.of(mapper, rate);
                /*
                 * The four references, as names — the only form that means anything elsewhere. The
                 * park and the tariff come through the join entity the rate actually hangs off.
                 */
                ParkTariff link = rate.getParkTariff();
                name(row, "tariff", link == null || link.getTariff() == null
                    ? null : link.getTariff().getSlug());
                name(row, "season", rate.getSeason() == null ? null : rate.getSeason().getName());
                name(row, "nationCategory",
                    rate.getNationCategory() == null ? null : rate.getNationCategory().getName());
                name(row, "ageCategory",
                    rate.getAgeCategory() == null ? null : rate.getAgeCategory().getName());
                rateRows.add(row);
            }

            /* the gallery, only when asked for — it dwarfs the rates otherwise */
            imageFiles.attach(mapper, node, "parks", park.getSlug(),
                parkImages.findByParkIdOrderByDisplayOrderAsc(park.getId()), includeImages, files);

            rows.add(node);
        }
        return rows;
    }

    /**
     * A reference written as a name, or an explicit null.
     *
     * A helper rather than ObjectNode.put because put(String, null) is ambiguous between the Boolean
     * and byte[] overloads, and because an absent field and a null one should not mean different
     * things to the reader.
     */
    private void name(ObjectNode node, String field, String value) {
        if (value == null) node.putNull(field); else node.put(field, value);
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data) {
            String slug = row.path("slug").asText(null);
            String label = row.path("name").asText(slug);
            if (slug == null || slug.isBlank()) {
                outcome.unresolved(label, "the park has no slug, so nothing can be matched on");
                continue;
            }

            Park park = parks.findBySlug(slug).orElse(null);
            boolean fresh = park == null;

            if (fresh) {
                park = new Park();
                Scalars.apply(mapper, row, park);
                park.setSlug(slug);
                park = parks.save(park);
                outcome.created();
            } else if (context.mayOverwrite()) {
                Scalars.apply(mapper, row, park, "slug");
                park = parks.save(park);
                outcome.updated();
            } else {
                outcome.skip(label, "already here");
                /*
                 * The park is left alone, but its RATES are still offered — which is usually the
                 * point. Somebody moving a corrected rate sheet into a company that already has the
                 * parks wants the numbers, not a second Serengeti.
                 */
            }

            applyImages(row, park, context);
            applyTariffLinks(row, park, context);
            applyRates(row, park, context);
        }
    }

    /** Which tariffs the park charges. The link carries nothing but a note, so it is create-or-leave. */
    private void applyTariffLinks(JsonNode row, Park park, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode charge : row.path("tariffs")) {
            String slug = charge.path("tariff").asText(null);
            if (slug == null) continue;

            Tariff tariff = resolver.tariff(context, slug);
            if (tariff == null) {
                outcome.unresolved(park.getName() + " / " + slug, "no tariff with this slug");
                continue;
            }
            if (parkTariffs.existsByParkIdAndTariffId(park.getId(), tariff.getId())) continue;

            ParkTariff link = new ParkTariff();
            link.setPark(park);
            link.setTariff(tariff);
            if (charge.hasNonNull("notes")) link.setNotes(charge.get("notes").asText());
            parkTariffs.save(link);
        }
    }

    private void applyRates(JsonNode row, Park park, TransferContext context) {
        var outcome = context.getReport().forModule("park-rates");

        for (JsonNode rateRow : row.path("rates")) {
            String tariffSlug = rateRow.path("tariff").asText(null);
            String seasonName = rateRow.path("season").asText(null);
            String nationName = rateRow.path("nationCategory").asText(null);
            String ageName = rateRow.path("ageCategory").asText(null);

            String key = park.getName() + " · " + tariffSlug + " · " + seasonName
                + " · " + nationName + (ageName == null ? "" : " · " + ageName);

            Tariff tariff = resolver.tariff(context, tariffSlug);
            if (tariff == null) {
                outcome.unresolved(key, "no tariff '" + tariffSlug + "'");
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
            /* the age band is optional on the entity, so only a NAMED one that is missing is a fault */
            var age = ageName == null ? null : resolver.age(context, ageName);
            if (ageName != null && age == null) {
                outcome.unresolved(key, "no age band '" + ageName + "'");
                continue;
            }

            /*
             * The composite the table already declares unique. Using it as the import identity means
             * the database and this code cannot disagree about what "the same rate" is.
             */
            /*
             * Looked up in a map built once per park, not with a query per rate.
             *
             * A park carries a few hundred rates and each one asked "is this already here?" on its
             * own, then asked for the park-tariff link on its own, then inserted. Three round trips
             * a row across three modules is sixteen thousand for one bundle, and a check of 5,394
             * rates took 274 seconds — longer than the gateway holds the request open, so the
             * report could never reach the person who asked for it. The rows were never the cost.
             */
            String rateKey = tariff.getId() + "/" + season.getId() + "/" + nation.getId()
                + "/" + (age == null ? "-" : age.getId());
            ParkTariffRate existing = ratesAlreadyHere(context, park).get(rateKey);

            if (existing != null && !context.mayOverwrite()) {
                outcome.skip(key, "already here");
                continue;
            }

            /*
             * The rate hangs off the park-tariff LINK, so the link has to exist first. A bundle may
             * carry a rate for a tariff the park's own `tariffs` list forgot to mention — the rate
             * is the stronger evidence that the park charges it, so the link is created rather than
             * the rate refused.
             */
            ParkTariff link = context.cached("park-tariff:" + park.getId(),
                String.valueOf(tariff.getId()),
                cacheKey -> parkTariffs.findByParkIdAndTariffId(park.getId(), tariff.getId())
                    .orElseGet(() -> {
                        ParkTariff fresh = new ParkTariff();
                        fresh.setPark(park);
                        fresh.setTariff(tariff);
                        return parkTariffs.save(fresh);
                    }));

            ParkTariffRate rate = existing == null ? new ParkTariffRate() : existing;
            Scalars.apply(mapper, rateRow, rate);
            rate.setParkTariff(link);
            rate.setSeason(season);
            rate.setNationCategory(nation);
            rate.setAgeCategory(age);
            rates.save(rate);
            ratesAlreadyHere(context, park).put(rateKey, rate);

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
    private void applyImages(JsonNode row, Park owner, TransferContext context) {
        JsonNode images = row.path("images");
        if (!images.isArray() || images.isEmpty()) return;

        var outcome = context.getReport().forModule("parks-images");
        var existing = parkImages.findByParkIdOrderByDisplayOrderAsc(owner.getId()).stream()
            .map(ParkImage::getFileName).collect(java.util.stream.Collectors.toSet());

        for (JsonNode imageRow : images) {
            String fileName = imageFiles.place(context, "parks", imageRow);
            if (fileName == null) {
                outcome.unresolved(imageRow.path("fileName").asText("(unnamed)"),
                    "the bundle was exported without its files");
                continue;
            }
            if (existing.contains(fileName)) {
                outcome.skip(fileName, "already here");
                continue;
            }
            ParkImage image = new ParkImage();
            Scalars.apply(mapper, imageRow, image, "file");
            image.setPark(owner);
            parkImages.save(image);
            outcome.created();
        }
    }

    /**
     * Every rate this park already has, by its composite key, read once.
     *
     * Cached on the context so the second rate in a park does not re-read them, and updated as rows
     * are written so a bundle naming the same rate twice still sees the first one.
     */
    @SuppressWarnings("unchecked")
    private Map<String, ParkTariffRate> ratesAlreadyHere(TransferContext context, Park park) {
        return context.cached("park-rate-index", String.valueOf(park.getId()), cacheKey -> {
            Map<String, ParkTariffRate> index = new HashMap<>();
            for (ParkTariffRate rate : rates.findByParkId(park.getId())) {
                index.put(compositeKeyOf(rate), rate);
            }
            return index;
        });
    }

    private static String compositeKeyOf(ParkTariffRate rate) {
        ParkTariff link = rate.getParkTariff();
        Long tariffId = link == null || link.getTariff() == null ? null : link.getTariff().getId();
        return tariffId + "/" + (rate.getSeason() == null ? null : rate.getSeason().getId())
            + "/" + (rate.getNationCategory() == null ? null : rate.getNationCategory().getId())
            + "/" + (rate.getAgeCategory() == null ? "-" : rate.getAgeCategory().getId());
    }
}
