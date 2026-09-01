package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.Accommodation;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationBoardType;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRate;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomStandard;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationRoomType;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationBoardTypeRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRateRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomStandardRepository;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationRoomTypeRepository;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ImageFiles;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ReferenceResolver;
import com.itineraryledger.kabengosafaris.Accommodation.Entities.AccommodationImage;
import com.itineraryledger.kabengosafaris.Accommodation.Repositories.AccommodationImageRepository;
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;

import lombok.RequiredArgsConstructor;

/**
 * Lodges, with everything a rate is made of inside them.
 *
 * The easy shape, once parks have forced the resolver to be honest. A rate here is identified by
 * (lodge, season, room type, room standard, board type) — and unlike a park rate, four of those five
 * are CHILDREN of the lodge rather than company-wide lookups. "Deluxe" at one lodge has nothing to do
 * with "Deluxe" at another, so it travels inside the document and is matched within its own parent.
 *
 * A lodge may also carry its own seasons, which likewise mean nothing outside it. They are written
 * before the rates for the obvious reason and reported separately, because a lodge whose seasons
 * failed to land will produce a page of unplaceable rates and it should be clear which came first.
 */
@Component
@RequiredArgsConstructor
public class AccommodationTransfer implements ModuleTransfer {

    private final AccommodationRepository accommodations;
    private final AccommodationRoomTypeRepository roomTypes;
    private final AccommodationRoomStandardRepository roomStandards;
    private final AccommodationBoardTypeRepository boardTypes;
    private final AccommodationRateRepository rates;
    private final SeasonRepository seasons;
    private final SeasonPeriodRepository periods;
    private final AccommodationImageRepository accommodationImages;
    private final ImageFiles imageFiles;
    private final ReferenceResolver resolver;
    private final ObjectMapper mapper;

    @Override public String name() { return "accommodations"; }
    @Override public String label() { return "Accommodations, their room setup and rates"; }
    @Override public int order() { return 60; }
    @Override public long count() { return accommodations.count(); }

    @Override
    public String detail() {
        long total = rates.count();
        return total == 0 ? "no rates yet" : String.format("%,d rate%s", total, total == 1 ? "" : "s");
    }

    @Override
    public List<String> requires() {
        /* no parks: a lodge's rate names nothing outside itself except a company-wide season */
        return List.of("seasons");
    }

    private void name(ObjectNode node, String field, String value) {
        if (value == null) node.putNull(field); else node.put(field, value);
    }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();

        for (Accommodation lodge : accommodations.findAll()) {
            ObjectNode node = Scalars.of(mapper, lodge);

            ArrayNode types = node.putArray("roomTypes");
            for (AccommodationRoomType type : roomTypes.findByAccommodationId(lodge.getId())) {
                types.add(Scalars.of(mapper, type));
            }
            ArrayNode standards = node.putArray("roomStandards");
            for (AccommodationRoomStandard standard : roomStandards.findByAccommodationId(lodge.getId())) {
                standards.add(Scalars.of(mapper, standard));
            }
            ArrayNode boards = node.putArray("boardTypes");
            for (AccommodationBoardType board : boardTypes.findByAccommodationId(lodge.getId())) {
                boards.add(Scalars.of(mapper, board));
            }

            /* the lodge's own seasons, which have no meaning anywhere else */
            ArrayNode ownSeasons = node.putArray("seasons");
            for (Season season : seasons.findByAccommodationId(lodge.getId())) {
                ownSeasons.add(SeasonTransfer.seasonNode(mapper, season, periods.findBySeasonId(season.getId())));
            }

            ArrayNode rateRows = node.putArray("rates");
            for (AccommodationRate rate : rates.findByAccommodationId(lodge.getId())) {
                ObjectNode row = Scalars.of(mapper, rate);
                name(row, "season", rate.getSeason() == null ? null : rate.getSeason().getName());
                name(row, "roomType", rate.getRoomType() == null ? null : rate.getRoomType().getName());
                name(row, "roomStandard",
                    rate.getRoomStandard() == null ? null : rate.getRoomStandard().getName());
                name(row, "boardType", rate.getBoardType() == null ? null : rate.getBoardType().getName());
                rateRows.add(row);
            }

            /* the gallery, only when asked for — it dwarfs the rates otherwise */
            imageFiles.attach(mapper, node, "accommodations", lodge.getSlug(),
                accommodationImages.findByAccommodationIdOrderByDisplayOrderAsc(lodge.getId()), includeImages, files);

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
                outcome.unresolved(label, "the accommodation has no slug, so nothing can be matched on");
                continue;
            }

            Accommodation lodge = accommodations.findBySlug(slug).orElse(null);
            if (lodge == null) {
                lodge = new Accommodation();
                Scalars.apply(mapper, row, lodge);
                lodge.setSlug(slug);
                /* the parent-lodge and vendor links are per-installation, and are not carried */
                lodge.setParentAccommodation(null);
                lodge.setVendor(null);
                lodge = accommodations.save(lodge);
                outcome.created();
            } else if (context.mayOverwrite()) {
                Scalars.apply(mapper, row, lodge, "slug");
                lodge = accommodations.save(lodge);
                outcome.updated();
            } else {
                outcome.skip(label, "already here");
            }

            /* the rate's parents first, in the order a rate needs them */
            applyImages(row, lodge, context);
            applyChildren(row, lodge, context);
            applyOwnSeasons(row, lodge, context);
            applyRates(row, lodge, context);
        }
    }

    /**
     * Room types, standards and board types.
     *
     * Created when absent even in SKIP mode, and never overwritten. They are not the point of an
     * import — they are the vocabulary its rates are written in — so a missing one has to be made or
     * every rate naming it is unplaceable, and an existing one must be left alone or somebody's
     * renamed room category quietly changes under their quotes.
     */
    private void applyChildren(JsonNode row, Accommodation lodge, TransferContext context) {
        for (JsonNode child : row.path("roomTypes")) {
            ensure(child, () -> {
                AccommodationRoomType made = new AccommodationRoomType();
                made.setAccommodation(lodge);
                return made;
            }, n -> roomTypes.findByAccommodationIdAndName(lodge.getId(), n).isPresent(),
                made -> roomTypes.save((AccommodationRoomType) made));
        }
        for (JsonNode child : row.path("roomStandards")) {
            ensure(child, () -> {
                AccommodationRoomStandard made = new AccommodationRoomStandard();
                made.setAccommodation(lodge);
                return made;
            }, n -> roomStandards.findByAccommodationIdAndName(lodge.getId(), n).isPresent(),
                made -> roomStandards.save((AccommodationRoomStandard) made));
        }
        for (JsonNode child : row.path("boardTypes")) {
            ensure(child, () -> {
                AccommodationBoardType made = new AccommodationBoardType();
                made.setAccommodation(lodge);
                return made;
            }, n -> boardTypes.findByAccommodationIdAndName(lodge.getId(), n).isPresent(),
                made -> boardTypes.save((AccommodationBoardType) made));
        }
    }

    private void ensure(JsonNode child, Supplier<Object> make,
                        java.util.function.Predicate<String> exists,
                        java.util.function.Consumer<Object> save) {
        String childName = child.path("name").asText(null);
        if (childName == null || childName.isBlank() || exists.test(childName)) return;
        Object made = make.get();
        Scalars.apply(mapper, child, made);
        save.accept(made);
    }

    private void applyOwnSeasons(JsonNode row, Accommodation lodge, TransferContext context) {
        var outcome = context.getReport().forModule("accommodation-seasons");

        for (JsonNode seasonRow : row.path("seasons")) {
            String seasonName = seasonRow.path("name").asText(null);
            if (seasonName == null || seasonName.isBlank()) continue;

            Season existing = seasons
                .findByAccommodationIdAndNameIgnoreCase(lodge.getId(), seasonName).orElse(null);
            if (existing != null) {
                if (!context.mayOverwrite()) {
                    outcome.skip(lodge.getName() + " · " + seasonName, "already here");
                    continue;
                }
                Scalars.apply(mapper, seasonRow, existing, "isSystem");
                existing.setAccommodation(lodge);
                existing.setIsGlobal(false);
                seasons.save(existing);
                SeasonTransfer.applyPeriods(mapper, seasonRow, existing, periods);
                outcome.updated();
                continue;
            }

            Season created = new Season();
            Scalars.apply(mapper, seasonRow, created, "isSystem");
            created.setAccommodation(lodge);
            created.setIsGlobal(false);
            created.setIsSystem(false);
            Season saved = seasons.save(created);
            SeasonTransfer.applyPeriods(mapper, seasonRow, saved, periods);
            outcome.created();
        }
    }

    private void applyRates(JsonNode row, Accommodation lodge, TransferContext context) {
        var outcome = context.getReport().forModule("accommodation-rates");

        for (JsonNode rateRow : row.path("rates")) {
            String seasonName = rateRow.path("season").asText(null);
            String typeName = rateRow.path("roomType").asText(null);
            String standardName = rateRow.path("roomStandard").asText(null);
            String boardName = rateRow.path("boardType").asText(null);

            String key = lodge.getName() + " · " + seasonName + " · " + typeName
                + " · " + standardName + " · " + boardName;

            /* the lodge's own season first, then a company-wide one of that name */
            Season season = resolver.seasonFor(context, lodge.getId(), seasonName);
            if (season == null) {
                outcome.unresolved(key, "no season '" + seasonName + "' at this lodge or company-wide");
                continue;
            }
            AccommodationRoomType type = typeName == null ? null
                : roomTypes.findByAccommodationIdAndName(lodge.getId(), typeName).orElse(null);
            if (type == null) {
                outcome.unresolved(key, "no room type '" + typeName + "' at this lodge");
                continue;
            }
            AccommodationRoomStandard standard = standardName == null ? null
                : roomStandards.findByAccommodationIdAndName(lodge.getId(), standardName).orElse(null);
            if (standard == null) {
                outcome.unresolved(key, "no room standard '" + standardName + "' at this lodge");
                continue;
            }
            AccommodationBoardType board = boardName == null ? null
                : boardTypes.findByAccommodationIdAndName(lodge.getId(), boardName).orElse(null);
            if (board == null) {
                outcome.unresolved(key, "no board type '" + boardName + "' at this lodge");
                continue;
            }

            /* One read per lodge, not one per rate — see ParkTransfer.ratesAlreadyHere. */
            String rateKey = season.getId() + "/" + type.getId() + "/" + standard.getId()
                + "/" + board.getId();
            AccommodationRate existing = ratesAlreadyHere(context, lodge).get(rateKey);

            if (existing != null && !context.mayOverwrite()) {
                outcome.skip(key, "already here");
                continue;
            }

            AccommodationRate rate = existing == null ? new AccommodationRate() : existing;
            Scalars.apply(mapper, rateRow, rate);
            rate.setAccommodation(lodge);
            rate.setSeason(season);
            rate.setRoomType(type);
            rate.setRoomStandard(standard);
            rate.setBoardType(board);
            rates.save(rate);
            ratesAlreadyHere(context, lodge).put(rateKey, rate);

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
    private void applyImages(JsonNode row, Accommodation owner, TransferContext context) {
        JsonNode images = row.path("images");
        if (!images.isArray() || images.isEmpty()) return;

        var outcome = context.getReport().forModule("accommodations-images");
        var existing = accommodationImages.findByAccommodationIdOrderByDisplayOrderAsc(owner.getId()).stream()
            .map(AccommodationImage::getFileName).collect(java.util.stream.Collectors.toSet());

        for (JsonNode imageRow : images) {
            String fileName = imageFiles.place(context, "accommodations", imageRow);
            if (fileName == null) {
                outcome.unresolved(imageRow.path("fileName").asText("(unnamed)"),
                    "the bundle was exported without its files");
                continue;
            }
            if (existing.contains(fileName)) {
                outcome.skip(fileName, "already here");
                continue;
            }
            AccommodationImage image = new AccommodationImage();
            Scalars.apply(mapper, imageRow, image, "file");
            image.setAccommodation(owner);
            accommodationImages.save(image);
            outcome.created();
        }
    }

    /** Every rate this lodge already has, by composite key, read once per lodge. */
    private Map<String, AccommodationRate> ratesAlreadyHere(
            TransferContext context, Accommodation lodge) {
        return context.cached("lodge-rate-index", String.valueOf(lodge.getId()), cacheKey -> {
            Map<String, AccommodationRate> index = new HashMap<>();
            for (AccommodationRate rate : rates.findByAccommodationId(lodge.getId())) {
                index.put((rate.getSeason() == null ? null : rate.getSeason().getId())
                    + "/" + (rate.getRoomType() == null ? null : rate.getRoomType().getId())
                    + "/" + (rate.getRoomStandard() == null ? null : rate.getRoomStandard().getId())
                    + "/" + (rate.getBoardType() == null ? null : rate.getBoardType().getId()), rate);
            }
            return index;
        });
    }
}
