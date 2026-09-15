package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * The lines a trip's promise is assembled from.
 *
 * <p>Supporting, and it travels because an itinerary needs it. Until the catalogue existed these
 * two sentences were free text on the itinerary row, so {@code Scalars.of} carried them by accident
 * — the moment they became rows they would have stopped travelling, and an itinerary imported into
 * another company would have arrived saying nothing about what its price covers.
 *
 * <p><strong>Matched on the wording</strong>, not on a code. A code is derived from a row id, so
 * the same sentence has a different code in each company; the sentence itself is the only stable
 * name it has across an export. That is also why the importer refuses to rewrite an existing line:
 * two companies that both say "Private 4×4 safari vehicle with pop-up roof" mean the same thing,
 * and neither needs the other's punctuation.
 */
@Component
@RequiredArgsConstructor
public class InclusionTransfer implements ModuleTransfer {

    private final InclusionItemRepository items;
    private final ObjectMapper mapper;

    @Override public String name() { return "inclusions"; }
    @Override public String label() { return "What a price covers"; }
    /* Before itineraries (order 60 and up), because an itinerary names these. */
    @Override public int order() { return 25; }
    @Override public boolean isSupporting() { return true; }
    @Override public long count() { return items.count(); }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();
        for (InclusionItem item : items.findAll()) {
            /*
             * The code does not travel: it is derived from a row id, so carrying it would either
             * collide with a local row or be ignored. The receiving company generates its own.
             */
            rows.add(Scalars.of(mapper, item, "code", "isSystem"));
        }
        return rows;
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data) {
            String label = row.path("label").asText(null);
            if (label == null || label.isBlank()) {
                outcome.unresolved("(unnamed line)", "a line with no wording cannot be matched");
                continue;
            }

            var existing = items.findByLabelIgnoringCaseAndSpace(label);
            if (existing.isPresent()) {
                /*
                 * Left alone on purpose, even when overwriting is allowed. The same sentence in two
                 * companies is the same promise, and this company's own wording, ordering and
                 * claim scope are decisions somebody here made. Importing an itinerary should not
                 * quietly restyle the catalogue it lands in.
                 */
                outcome.skip(label, "this company already says it");
                continue;
            }

            InclusionItem created = new InclusionItem();
            Scalars.apply(mapper, row, created, "code", "isSystem");
            created.setIsSystem(false);
            /*
             * Never standard on arrival. A line another company puts on every trip is not
             * necessarily one this company wants on every trip, and a standard line would start
             * appearing on itineraries created here that have nothing to do with the import.
             */
            created.setIsStandard(false);
            created.setDisplayOrder(nextOrder());

            created = items.saveAndFlush(created);
            created.setCode(created.generateCode());
            items.save(created);
            outcome.created();
        }
    }

    private int nextOrder() {
        Integer max = items.findMaxDisplayOrder();
        return (max != null ? max : 0) + 1;
    }
}
