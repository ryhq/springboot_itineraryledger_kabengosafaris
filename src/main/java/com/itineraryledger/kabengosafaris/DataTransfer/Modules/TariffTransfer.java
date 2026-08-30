package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

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
import com.itineraryledger.kabengosafaris.Tariff.Tariff;
import com.itineraryledger.kabengosafaris.Tariff.Repositories.TariffRepository;

import lombok.RequiredArgsConstructor;

/**
 * The tariff catalogue: what KINDS of charge exist at all.
 *
 * Not an export anybody asks for — it travels because park rates are made of it. A park rate is
 * identified by (park, tariff, season, nationality, age), so a bundle of Serengeti rates that did
 * not carry "Conservation Fee" as a concept would arrive with nowhere to put a single number.
 *
 * Matched on `slug`, which is unique and generated from the name, so "Concession Fee" lands on the
 * existing `concession-fee` rather than making a second one.
 */
@Component
@RequiredArgsConstructor
public class TariffTransfer implements ModuleTransfer {

    private final TariffRepository tariffs;
    private final ObjectMapper mapper;

    @Override public String name() { return "tariffs"; }
    @Override public String label() { return "Tariff catalogue"; }
    @Override public int order() { return 10; }
    @Override public boolean isSupporting() { return true; }
    @Override public long count() { return tariffs.count(); }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();
        for (Tariff tariff : tariffs.findAll()) {
            rows.add(Scalars.of(mapper, tariff));
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
                outcome.unresolved(label, "the row carries no slug, so nothing can be matched on");
                continue;
            }

            Tariff existing = tariffs.findBySlug(slug).orElse(null);
            if (existing != null) {
                if (!context.mayOverwrite()) {
                    outcome.skip(label, "already here");
                    continue;
                }
                /*
                 * A system tariff is seeded by this application on every start, so overwriting one
                 * lasts until the next restart and then silently reverts. Better to say so.
                 */
                if (Boolean.TRUE.equals(existing.getIsSystem())) {
                    outcome.skip(label, "a built-in tariff — the initializer restores it on every restart");
                    continue;
                }
                Scalars.apply(mapper, row, existing, "slug", "isSystem");
                tariffs.save(existing);
                outcome.updated();
                continue;
            }

            Tariff created = new Tariff();
            Scalars.apply(mapper, row, created, "isSystem");
            /* imported rows are ordinary data: only this application's own seeding makes a built-in */
            created.setIsSystem(false);
            tariffs.save(created);
            outcome.created();
        }
    }
}
