package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.ArrayList;
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
import com.itineraryledger.kabengosafaris.Season.Season;
import com.itineraryledger.kabengosafaris.Season.SeasonPeriod;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonPeriodRepository;
import com.itineraryledger.kabengosafaris.Season.Repositories.SeasonRepository;

import lombok.RequiredArgsConstructor;

/**
 * The company-wide seasons, with the dates that make them mean something.
 *
 * A season is only a label until its periods say when it runs, and every rate in the product hangs
 * off one — so a bundle of rates without these is a price list with no answer to "when".
 *
 * The periods travel as they are, which is safe more often than it sounds: a period is a MonthDay
 * pair, so "1 June to 31 October" repeats every year and lands correctly in any company. Only a
 * period that pins a `year` is tied to a particular one, and the preview counts those separately so
 * nobody discovers it from a quote priced against a window that closed last year.
 *
 * Only GLOBAL seasons are here. A season belonging to one lodge travels inside that lodge, because
 * that is where it means something.
 */
@Component
@RequiredArgsConstructor
public class SeasonTransfer implements ModuleTransfer {

    private final SeasonRepository seasons;
    private final SeasonPeriodRepository periods;
    private final ObjectMapper mapper;

    @Override public String name() { return "seasons"; }
    @Override public String label() { return "Seasons and their dates"; }
    @Override public int order() { return 30; }
    @Override public boolean isSupporting() { return true; }

    @Override
    public long count() {
        return seasons.findAll().stream().filter(s -> s.getAccommodation() == null).count();
    }

    /** A season plus its periods, used here and by the accommodation module for its own seasons. */
    static ObjectNode seasonNode(ObjectMapper mapper, Season season, List<SeasonPeriod> its) {
        ObjectNode node = Scalars.of(mapper, season);
        ArrayNode rows = node.putArray("periods");
        for (SeasonPeriod period : its) rows.add(Scalars.of(mapper, period));
        return node;
    }

    /**
     * Write a season's periods, replacing whatever was there.
     *
     * Replaced rather than merged because a period has no natural key — two periods of the same
     * season differ only by their dates, which are the thing being changed. Merging would leave last
     * year's window sitting alongside this year's with nothing to say which is current.
     */
    static void applyPeriods(ObjectMapper mapper, JsonNode node, Season season,
                             SeasonPeriodRepository periods) {
        JsonNode rows = node.path("periods");
        if (!rows.isArray() || rows.isEmpty()) return;

        List<SeasonPeriod> existing = periods.findBySeasonId(season.getId());
        if (!existing.isEmpty()) periods.deleteAll(existing);

        List<SeasonPeriod> fresh = new ArrayList<>();
        for (JsonNode row : rows) {
            SeasonPeriod period = new SeasonPeriod();
            Scalars.apply(mapper, row, period, "isSystem");
            period.setSeason(season);
            period.setIsSystem(false);
            fresh.add(period);
        }
        periods.saveAll(fresh);
    }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();
        for (Season season : seasons.findAll()) {
            /* a lodge's own season belongs to the lodge, and travels inside it */
            if (season.getAccommodation() != null) continue;
            rows.add(seasonNode(mapper, season, periods.findBySeasonId(season.getId())));
        }
        return rows;
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data) {
            String name = row.path("name").asText(null);
            if (name == null || name.isBlank()) {
                outcome.unresolved("(unnamed season)", "a season with no name cannot be matched");
                continue;
            }

            Season existing = seasons.findByIsGlobalTrueAndNameIgnoreCase(name).orElse(null);
            if (existing != null) {
                if (!context.mayOverwrite()) {
                    outcome.skip(name, "already here");
                    continue;
                }
                Scalars.apply(mapper, row, existing, "isSystem");
                existing.setIsGlobal(true);
                seasons.save(existing);
                applyPeriods(mapper, row, existing, periods);
                outcome.updated();
                continue;
            }

            Season created = new Season();
            Scalars.apply(mapper, row, created, "isSystem");
            created.setIsGlobal(true);
            created.setIsSystem(false);
            created.setAccommodation(null);
            Season saved = seasons.save(created);
            applyPeriods(mapper, row, saved, periods);
            outcome.created();
        }
    }
}
