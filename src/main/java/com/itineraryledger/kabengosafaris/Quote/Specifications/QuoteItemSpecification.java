package com.itineraryledger.kabengosafaris.Quote.Specifications;

import org.springframework.data.jpa.domain.Specification;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteItem;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;

/**
 * JPA Specifications for QuoteItem filtering.
 */
public class QuoteItemSpecification {

    // ========================
    // BASIC FILTERS
    // ========================

    public static Specification<QuoteItem> byQuoteId(Long quoteId) {
        return (root, query, cb) -> quoteId == null
            ? cb.conjunction()
            : cb.equal(root.get("quote").get("id"), quoteId);
    }

    public static Specification<QuoteItem> byItemType(QuoteItemType itemType) {
        return (root, query, cb) -> itemType == null
            ? cb.conjunction()
            : cb.equal(root.get("itemType"), itemType);
    }

    public static Specification<QuoteItem> byItemName(String itemName) {
        return (root, query, cb) -> {
            if (itemName == null || itemName.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("itemName")), "%" + itemName.toLowerCase().trim() + "%");
        };
    }

    public static Specification<QuoteItem> byItemNameContaining(String itemName) {
        return byItemName(itemName);
    }

    public static Specification<QuoteItem> byDescription(String description) {
        return (root, query, cb) -> {
            if (description == null || description.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase().trim() + "%");
        };
    }

    public static Specification<QuoteItem> byDescriptionContaining(String description) {
        return byDescription(description);
    }

    public static Specification<QuoteItem> byIsActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null
            ? cb.conjunction()
            : cb.equal(root.get("isActive"), isActive);
    }

    // ========================
    // ITEM TYPE GROUPS
    // ========================

    public static Specification<QuoteItem> byAccommodationItems() {
        return (root, query, cb) -> cb.equal(root.get("itemType"), QuoteItemType.ACCOMMODATION);
    }

    public static Specification<QuoteItem> byParkFeeItems() {
        return (root, query, cb) -> cb.equal(root.get("itemType"), QuoteItemType.PARK_FEE);
    }

    public static Specification<QuoteItem> byActivityItems() {
        return (root, query, cb) -> cb.equal(root.get("itemType"), QuoteItemType.ACTIVITY);
    }

    public static Specification<QuoteItem> byTransportItems() {
        return (root, query, cb) -> cb.equal(root.get("itemType"), QuoteItemType.TRANSPORT);
    }

    public static Specification<QuoteItem> byGuideItems() {
        return (root, query, cb) -> cb.equal(root.get("itemType"), QuoteItemType.GUIDE);
    }

    public static Specification<QuoteItem> byMealItems() {
        return (root, query, cb) -> cb.equal(root.get("itemType"), QuoteItemType.MEALS);
    }

    /**
     * Filter by item type group using string parameter
     *
     * @param itemTypeGroup The group name (accommodation, parkfee, activity, transport, guide, meal)
     * @return Specification for the specified item type group
     */
    public static Specification<QuoteItem> byItemTypeGroup(String itemTypeGroup) {
        if (itemTypeGroup == null || itemTypeGroup.trim().isEmpty()) {
            return (root, query, cb) -> cb.conjunction();
        }

        String group = itemTypeGroup.toLowerCase().trim();
        switch (group) {
            case "accommodation":
                return byAccommodationItems();
            case "parkfee":
            case "park_fee":
                return byParkFeeItems();
            case "activity":
                return byActivityItems();
            case "transport":
                return byTransportItems();
            case "guide":
                return byGuideItems();
            case "meal":
            case "meals":
                return byMealItems();
            default:
                return (root, query, cb) -> cb.conjunction();
        }
    }
}
