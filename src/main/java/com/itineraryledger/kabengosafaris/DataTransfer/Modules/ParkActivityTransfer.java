package com.itineraryledger.kabengosafaris.DataTransfer.Modules;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itineraryledger.kabengosafaris.Activity.Activity;
import com.itineraryledger.kabengosafaris.DataTransfer.ModuleTransfer;
import com.itineraryledger.kabengosafaris.DataTransfer.Scalars;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferContext;
import com.itineraryledger.kabengosafaris.DataTransfer.TransferFile;
import com.itineraryledger.kabengosafaris.DataTransfer.Services.ReferenceResolver;
import com.itineraryledger.kabengosafaris.Park.Park;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivity;
import com.itineraryledger.kabengosafaris.ParkActivity.ParkActivityRepository;

import lombok.RequiredArgsConstructor;

/**
 * Which activities each park offers.
 *
 * A small table with a large consequence. An activity can only be put on a park visit if the park
 * is recorded as offering it, and the code that adds one answers 200 and stores NOTHING when the
 * link is absent, logging a warning nobody reads. That is how an optional Maasai boma visit
 * disappeared from a freshly built itinerary while the run log said it had been added.
 *
 * So the links have to travel. Without them a company can receive every park, every activity and
 * every rate, and still be unable to put a crater descent on a day.
 *
 * Its own module rather than a passenger of the parks module: a park's tariffs belong to the park,
 * but which activities it offers is a decision about the product, and one an office may reasonably
 * want to move on its own.
 */
@Component
@RequiredArgsConstructor
public class ParkActivityTransfer implements ModuleTransfer {

    private final ParkActivityRepository links;
    private final ReferenceResolver resolver;
    private final ObjectMapper mapper;

    @Override public String name() { return "park-activities"; }
    @Override public String label() { return "Which activities each park offers"; }
    @Override public int order() { return 55; }
    @Override public long count() { return links.count(); }
    @Override public List<String> requires() { return List.of("parks", "activities"); }

    @Override
    public String detail() {
        long n = links.count();
        return n + (n == 1 ? " link" : " links");
    }

    @Override
    public JsonNode export(boolean includeImages, List<TransferFile> files) {
        ArrayNode rows = mapper.createArrayNode();
        for (ParkActivity link : links.findAll()) {
            ObjectNode row = Scalars.of(mapper, link);
            /*
             * Both sides by SLUG. An id belongs to the database it came from, and a name is retyped
             * and re-cased; a slug is the one form that survives the journey.
             */
            row.put("park", link.getPark() == null ? null : link.getPark().getSlug());
            row.put("activity", link.getActivity() == null ? null : link.getActivity().getSlug());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public void importInto(JsonNode data, TransferContext context) {
        var outcome = context.getReport().forModule(name());

        for (JsonNode row : data) {
            String parkSlug = row.path("park").asText(null);
            String activitySlug = row.path("activity").asText(null);
            String key = parkSlug + " / " + activitySlug;

            Park park = resolver.park(context, parkSlug);
            if (park == null) {
                outcome.unresolved(key, "no park '" + parkSlug + "' here");
                continue;
            }
            Activity activity = resolver.activity(context, activitySlug);
            if (activity == null) {
                outcome.unresolved(key, "no activity '" + activitySlug + "' here");
                continue;
            }

            ParkActivity existing = links
                .findByParkIdAndActivityId(park.getId(), activity.getId()).orElse(null);
            if (existing != null) {
                if (!context.mayOverwrite()) {
                    outcome.skip(key, "already here");
                    continue;
                }
                Scalars.apply(mapper, row, existing, "park", "activity");
                links.save(existing);
                outcome.updated();
                continue;
            }

            ParkActivity created = new ParkActivity();
            Scalars.apply(mapper, row, created, "park", "activity");
            created.setPark(park);
            created.setActivity(activity);
            links.save(created);
            outcome.created();
        }
    }
}
