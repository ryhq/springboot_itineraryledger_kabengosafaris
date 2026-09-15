package com.itineraryledger.kabengosafaris.Inclusion.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Inclusion.Entity.InclusionItem;
import com.itineraryledger.kabengosafaris.Itinerary.ItineraryInclusion.Entity.ItineraryInclusion;

import jakarta.persistence.criteria.Subquery;

/** Predicates for the inclusion catalogue. */
public class InclusionItemSpecification {

    private InclusionItemSpecification() {}

    public static Specification<InclusionItem> isActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    public static Specification<InclusionItem> isStandard(Boolean isStandard) {
        return (root, query, cb) -> isStandard == null
            ? cb.conjunction()
            : cb.equal(root.get("isStandard"), isStandard);
    }

    public static Specification<InclusionItem> defaultIncluded(Boolean included) {
        return (root, query, cb) -> included == null
            ? cb.conjunction()
            : cb.equal(root.get("defaultIncluded"), included);
    }

    public static Specification<InclusionItem> isSystem(Boolean isSystem) {
        return (root, query, cb) -> isSystem == null
            ? cb.conjunction()
            : cb.equal(root.get("isSystem"), isSystem);
    }

    public static Specification<InclusionItem> byCategory(String category) {
        return (root, query, cb) -> category == null || category.isBlank()
            ? cb.conjunction()
            : cb.equal(cb.lower(root.get("category")), category.toLowerCase().trim());
    }

    public static Specification<InclusionItem> byCategories(java.util.List<String> categories) {
        return (root, query, cb) -> {
            if (categories == null || categories.isEmpty()) return cb.conjunction();
            return cb.lower(root.get("category")).in(
                categories.stream().filter(c -> c != null).map(c -> c.toLowerCase().trim()).toList());
        };
    }

    /** The label and the internal note both — staff search for the words they typed, either place. */
    public static Specification<InclusionItem> searchKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String needle = "%" + keyword.toLowerCase().trim() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("label")), needle),
                cb.like(cb.lower(root.get("category")), needle),
                cb.like(cb.lower(root.get("code")), needle),
                cb.like(cb.lower(root.get("internalNotes")), needle)
            );
        };
    }

    /**
     * Lines that say something a priced document can be checked against.
     *
     * <p>A blank scope is not "covers everything" here — it is "claims nothing". See the field
     * comment on {@code InclusionItem.claimAppliesTo}: the shared LineCategoryScope helper reads
     * null the other way round, and this is the boundary where the two meanings meet.
     */
    public static Specification<InclusionItem> makesAClaim() {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("claimAppliesTo")),
            cb.notEqual(cb.trim(root.get("claimAppliesTo")), "")
        );
    }

    public static Specification<InclusionItem> makesNoClaim() {
        return (root, query, cb) -> cb.or(
            cb.isNull(root.get("claimAppliesTo")),
            cb.equal(cb.trim(root.get("claimAppliesTo")), "")
        );
    }

    /**
     * Whether any itinerary has a row for this.
     *
     * <p>Itineraries only. A quote, safari or invoice holds the wording as its own text and has no
     * foreign key back here, so a sent document neither keeps an item alive nor blocks its delete —
     * which is exactly the property that lets a catalogue be tidied without touching paperwork
     * already with a customer.
     */
    public static Specification<InclusionItem> inUse(boolean used) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            var link = sub.from(ItineraryInclusion.class);
            sub.select(cb.literal(1L));
            sub.where(cb.equal(link.get("inclusionItem").get("id"), root.get("id")));
            return used ? cb.exists(sub) : cb.not(cb.exists(sub));
        };
    }

    public static Specification<InclusionItem> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
