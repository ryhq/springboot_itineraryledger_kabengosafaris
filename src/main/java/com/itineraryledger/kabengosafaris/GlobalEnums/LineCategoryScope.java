package com.itineraryledger.kabengosafaris.GlobalEnums;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * "Which kinds of line does this apply to?", stored as text and read the same way everywhere.
 *
 * <p>A tax or a provision over the WHOLE of a safari is wrong in both directions. Park, crater and
 * conservation fees are government charges: there is no VAT of ours inside them to pass on, so
 * taxing them at the same rate as a bed invents a liability nobody owes. And a provision against a
 * supplier invoicing beyond contract is an accommodation risk, so spreading it over park fees and
 * activities charges the client for a risk those lines do not carry.
 *
 * <p>Empty or null means EVERY category — what every record written before this said by saying
 * nothing, and it must keep meaning the same thing.
 *
 * <p>Enum-agnostic on purpose. {@code QuoteItemType} and {@code InvoiceItemType} are the same ten
 * constants declared twice, and a quote's scope has to survive being invoiced: a quote taxed on
 * the beds alone that becomes an invoice taxed on everything is the exact overcharge this was
 * written to stop, arriving at the moment the money is actually collected.
 */
public final class LineCategoryScope {

    private LineCategoryScope() {}

    /** Canonical text for storage: "ACCOMMODATION,ACTIVITY", or null for everything. */
    public static String canonical(Set<String> names, Class<? extends Enum<?>> universe) {
        List<String> known = namesOf(universe);
        if (names == null || names.isEmpty()) return null;

        List<String> kept = new ArrayList<>();
        for (String name : known) {
            if (names.stream().anyMatch(n -> n != null && n.trim().equalsIgnoreCase(name))) {
                kept.add(name);
            }
        }
        if (kept.isEmpty() || kept.size() == known.size()) return null;
        // the enum's own order, so one selection is always written one way
        return String.join(",", kept);
    }

    /**
     * Canonical text from text: what arrives on a DTO, tidied for storage.
     *
     * <p>Null stays null (meaning every category), unknown names are dropped, order comes from the
     * enum. Without this the same selection is stored two ways depending on how it was typed, and
     * two quotes with identical scopes compare unequal.
     */
    public static String canonicalOf(String stored, Class<? extends Enum<?>> universe) {
        if (stored == null || stored.isBlank()) return null;
        return canonical(parse(stored, universe), universe);
    }

    /**
     * The category names in scope, or every one of them when nothing is named.
     *
     * <p>Unknown names are ignored rather than fatal, and a scope of ONLY unknown names widens to
     * everything: a category renamed one day must not silently tax nothing. A figure that is too
     * high gets questioned by whoever reads it; one that is too low gets invoiced and lost.
     */
    public static Set<String> parse(String stored, Class<? extends Enum<?>> universe) {
        List<String> known = namesOf(universe);
        if (stored == null || stored.isBlank()) return new LinkedHashSet<>(known);

        Set<String> asked = new LinkedHashSet<>();
        for (String part : stored.split(",")) {
            String name = part.trim().toUpperCase();
            if (!name.isEmpty()) asked.add(name);
        }
        /*
         * Walked in the ENUM's order, not the string's, so "PARK_FEE,ACCOMMODATION" and
         * "ACCOMMODATION,PARK_FEE" are one scope. Otherwise the same selection describes itself two
         * different ways on two quotes, and the sentence under a total looks like a difference.
         */
        Set<String> found = new LinkedHashSet<>();
        for (String name : known) {
            if (asked.contains(name)) found.add(name);
        }
        return found.isEmpty() ? new LinkedHashSet<>(known) : found;
    }

    /** Whether this line is inside the scope. A null type is outside a NAMED scope. */
    public static boolean covers(String stored, Enum<?> type) {
        if (stored == null || stored.isBlank()) return true;
        if (type == null) return false;
        return parse(stored, type.getDeclaringClass()).contains(type.name());
    }

    /** For a note on the document: "accommodation only", or "every line". */
    public static String describe(String stored, Class<? extends Enum<?>> universe) {
        if (stored == null || stored.isBlank()) return "every line";
        Set<String> names = parse(stored, universe);
        if (names.size() == namesOf(universe).size()) return "every line";
        return String.join(" and ", names.stream()
            .map(name -> name.toLowerCase().replace('_', ' '))
            .toList()) + " only";
    }

    private static List<String> namesOf(Class<? extends Enum<?>> universe) {
        List<String> names = new ArrayList<>();
        for (Enum<?> constant : universe.getEnumConstants()) names.add(constant.name());
        return names;
    }
}
