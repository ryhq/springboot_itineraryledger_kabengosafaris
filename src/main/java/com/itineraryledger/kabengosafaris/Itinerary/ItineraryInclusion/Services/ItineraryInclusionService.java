package com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope;
import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Inclusion.Repository.InclusionItemRepository;
import com.itineraryledger.kabengosafaris.Inclusion.Services.ItineraryInclusionReader;
import com.itineraryledger.kabengosafaris.Itinerary.Entity.Itinerary;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.DTOs.ItineraryInclusionDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.DTOs.SetItineraryInclusionsDTO;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;
import com.itineraryledger.kabengosafaris.Itinerary.Repository.ItineraryRepository;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * An itinerary's own list of what its price covers.
 *
 * <p>Read and written as a whole set, never a row at a time. The screen is a list of switches over
 * the catalogue, and the honest shape of "save what I switched" is one request that replaces the
 * lot — a half-applied change is then not a state this can end up in.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ItineraryInclusionService {

    private final ItineraryRepository itineraryRepository;
    private final InclusionItemRepository inclusionItems;
    private final ItineraryInclusionReader reader;
    private final IdObfuscator idObfuscator;

    /**
     * This itinerary's rows AND the catalogue it can pick from, in one answer.
     *
     * <p>Two requests would mean the screen reconciling two lists client-side, and the moment those
     * disagree — an item disabled between the two calls — it would render a switch for something
     * that cannot be switched.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> get(String obfuscatedItineraryId) {
        Itinerary itinerary = find(obfuscatedItineraryId);
        if (itinerary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
        }

        List<ItineraryInclusionDTO> rows = itinerary.getInclusionList().stream()
            .map(this::toDTO)
            .toList();

        /* The pickable catalogue: enabled items only, because a disabled one cannot be added. */
        List<InclusionItemDTOLite> catalogue = inclusionItems
            .findByIsActiveTrueOrderByDisplayOrderAscIdAsc().stream()
            .map(item -> new InclusionItemDTOLite(
                idObfuscator.encodeId(item.getId()),
                item.getCode(),
                item.getLabel(),
                item.getCategory(),
                item.getDefaultIncluded(),
                item.makesACheckableClaim()
                    ? LineCategoryScope.describe(item.getClaimAppliesTo(), QuoteItemType.class)
                    : null))
            .toList();

        Map<String, Object> response = new HashMap<>();
        response.put("inclusions", rows);
        response.put("catalogue", catalogue);
        response.put("includedCount", rows.stream().filter(r -> Boolean.TRUE.equals(r.getIsIncluded())).count());
        response.put("excludedCount", rows.stream().filter(r -> Boolean.FALSE.equals(r.getIsIncluded())).count());

        /*
         * What this itinerary said before the catalogue existed, surfaced only while it is still
         * answering from those columns. The screen shows it so that a line the backfill could not
         * match is visible to whoever can retype it, rather than quietly lost.
         */
        boolean legacy = reader.isLegacy(itinerary);
        response.put("usingLegacyText", legacy);
        if (legacy) {
            response.put("legacyIncluded", ItineraryInclusionReader.splitLines(itinerary.getInclusions()));
            response.put("legacyExcluded", ItineraryInclusionReader.splitLines(itinerary.getExclusions()));
        }

        return ResponseEntity.ok(
            ApiResponse.success(200, "Itinerary inclusions retrieved successfully", response));
    }

    /** The lite catalogue row the picker renders. A record, so the field order cannot drift. */
    public record InclusionItemDTOLite(
        String id,
        String code,
        String label,
        String category,
        Boolean defaultIncluded,
        String claimAppliesToLabel
    ) {}

    /**
     * Replace the whole set.
     *
     * <p>A disabled catalogue item is refused rather than silently dropped: the caller asked for
     * something specific, and answering 200 to a request that was partly ignored is how a line
     * somebody thought they had added turns out to be missing from a customer's quote.
     */
    @AuditLogAnnotation(action = "SET_ITINERARY_INCLUSIONS",
        description = "Setting what an itinerary's price covers", entityType = "Itinerary")
    public ResponseEntity<ApiResponse<?>> set(String obfuscatedItineraryId, SetItineraryInclusionsDTO dto) {
        Itinerary itinerary = find(obfuscatedItineraryId);
        if (itinerary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(404, "Itinerary not found", "ITINERARY_NOT_FOUND"));
        }
        if (dto == null || dto.getItems() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400,
                "Items list is required — send an empty list to clear it", "ITEMS_REQUIRED"));
        }

        /* Resolve everything first, so a bad row cannot leave the set half-written. */
        List<InclusionItem> resolved = new ArrayList<>();
        List<Boolean> stances = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();

        for (SetItineraryInclusionsDTO.Row row : dto.getItems()) {
            if (row == null || row.getInclusionItemId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "Every row must name an inclusion item", "INCLUSION_ITEM_REQUIRED"));
            }
            Long itemId;
            try {
                itemId = idObfuscator.decodeId(row.getInclusionItemId());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "Unreadable inclusion item id: " + row.getInclusionItemId(),
                    "INVALID_INCLUSION_ITEM_ID"));
            }
            InclusionItem item = inclusionItems.findById(itemId).orElse(null);
            if (item == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "No such inclusion item: " + row.getInclusionItemId(), "INCLUSION_ITEM_NOT_FOUND"));
            }
            if (Boolean.FALSE.equals(item.getIsActive())) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "\"" + item.getLabel() + "\" is disabled, so it cannot be put on a trip. "
                    + "Enable it first, or leave it off.", "INCLUSION_ITEM_DISABLED"));
            }
            /* The same line twice would print twice; the unique constraint would refuse it anyway. */
            if (!seen.add(itemId)) {
                return ResponseEntity.badRequest().body(ApiResponse.error(400,
                    "\"" + item.getLabel() + "\" is listed twice", "INCLUSION_ITEM_DUPLICATE"));
            }
            resolved.add(item);
            stances.add(!Boolean.FALSE.equals(row.getIsIncluded()));
        }

        /* orphanRemoval deletes what is cleared; the new rows are written in the order given. */
        itinerary.getInclusionList().clear();
        for (int i = 0; i < resolved.size(); i++) {
            itinerary.addInclusion(ItineraryInclusion.builder()
                .inclusionItem(resolved.get(i))
                .isIncluded(stances.get(i))
                .sortOrder(i + 1)
                .build());
        }

        /*
         * The old text columns are cleared the moment this itinerary has rows of its own. Leaving
         * them would mean two answers to the same question, and the reader's fallback would never
         * fire again anyway — but the next person to read the column would not know that.
         */
        itinerary.setInclusions(null);
        itinerary.setExclusions(null);

        itineraryRepository.save(itinerary);

        Map<String, Object> report = new HashMap<>();
        report.put("savedCount", resolved.size());
        report.put("includedCount", stances.stream().filter(Boolean::booleanValue).count());
        report.put("excludedCount", stances.stream().filter(s -> !s).count());
        return ResponseEntity.ok(
            ApiResponse.success(200, "Itinerary inclusions saved successfully", report));
    }

    private ItineraryInclusionDTO toDTO(ItineraryInclusion row) {
        InclusionItem item = row.getInclusionItem();
        return ItineraryInclusionDTO.builder()
            .id(idObfuscator.encodeId(row.getId()))
            .inclusionItemId(item != null ? idObfuscator.encodeId(item.getId()) : null)
            .code(item != null ? item.getCode() : null)
            .label(item != null ? item.getLabel() : null)
            .category(item != null ? item.getCategory() : null)
            .claimAppliesTo(item != null ? item.getClaimAppliesTo() : null)
            .claimAppliesToLabel(item != null && item.makesACheckableClaim()
                ? LineCategoryScope.describe(item.getClaimAppliesTo(), QuoteItemType.class)
                : null)
            .isIncluded(row.getIsIncluded())
            .sortOrder(row.getSortOrder())
            .itemIsActive(item != null ? item.getIsActive() : null)
            .build();
    }

    private Itinerary find(String obfuscatedId) {
        try {
            return itineraryRepository.findById(idObfuscator.decodeId(obfuscatedId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
