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
import com.itineraryledger.kabengosafaris.PaxAgeCategory.PaxAgeCategory;
import com.itineraryledger.kabengosafaris.PaxAgeCategory.Repositories.PaxAgeCategoryRepository;
import com.itineraryledger.kabengosafaris.PaxNationCategory.PaxNationCategory;
import com.itineraryledger.kabengosafaris.PaxNationCategory.Repositories.PaxNationCategoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Who a rate is FOR: residency and age bands.
 *
 * Both are what park and activity rates are priced by — a Serengeti entry fee is one number for a
 * non-resident adult and another for an East African child — so they travel with the rates whether
 * or not anybody thinks of them as data worth moving.
 *
 * Two small tables in one module because they are never wanted separately, and because a rate needs
 * both to resolve before it can be placed.
 */
@Component
@RequiredArgsConstructor
public class PaxCategoryTransfer implements ModuleTransfer {

    private final PaxNationCategoryRepository nations;
    private final PaxAgeCategoryRepository ages;
    private final ObjectMapper mapper;

    @Override public String name() { return "pax-categories"; }
    @Override public String label() { return "Guest categories (residency and age)"; }
    @Override public int order() { return 20; }
    @Override public boolean isSupporting() { return true; }
    @Override public long count() { return nations.count() + ages.count(); }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ObjectNode payload = mapper.createObjectNode();

        ArrayNode nationRows = payload.putArray("nations");
        for (PaxNationCategory nation : nations.findAll()) nationRows.add(Scalars.of(mapper, nation));

        ArrayNode ageRows = payload.putArray("ages");
        for (PaxAgeCategory age : ages.findAll()) ageRows.add(Scalars.of(mapper, age));

        return payload;
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data.path("nations")) {
            String name = row.path("name").asText(null);
            if (name == null || name.isBlank()) {
                outcome.unresolved("(unnamed residency)", "a category with no name cannot be matched");
                continue;
            }
            nations.findByNameIgnoreCase(name).ifPresentOrElse(existing -> {
                if (!context.mayOverwrite() || Boolean.TRUE.equals(existing.getIsSystem())) {
                    outcome.skip(name, Boolean.TRUE.equals(existing.getIsSystem())
                        ? "a built-in category" : "already here");
                    return;
                }
                Scalars.apply(mapper, row, existing, "isSystem");
                nations.save(existing);
                outcome.updated();
            }, () -> {
                PaxNationCategory created = new PaxNationCategory();
                Scalars.apply(mapper, row, created, "isSystem");
                created.setIsSystem(false);
                nations.save(created);
                outcome.created();
            });
        }

        for (JsonNode row : data.path("ages")) {
            String name = row.path("name").asText(null);
            if (name == null || name.isBlank()) {
                outcome.unresolved("(unnamed age band)", "a category with no name cannot be matched");
                continue;
            }
            ages.findByNameIgnoreCase(name).ifPresentOrElse(existing -> {
                if (!context.mayOverwrite() || Boolean.TRUE.equals(existing.getIsSystem())) {
                    outcome.skip(name, Boolean.TRUE.equals(existing.getIsSystem())
                        ? "a built-in category" : "already here");
                    return;
                }
                Scalars.apply(mapper, row, existing, "isSystem");
                ages.save(existing);
                outcome.updated();
            }, () -> {
                PaxAgeCategory created = new PaxAgeCategory();
                Scalars.apply(mapper, row, created, "isSystem");
                created.setIsSystem(false);
                ages.save(created);
                outcome.created();
            });
        }
    }
}
