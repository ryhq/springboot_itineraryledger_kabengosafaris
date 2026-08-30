package com.itineraryledger.kabengosafaris.DataTransfer;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One exportable module.
 *
 * Both directions live in the same class on purpose: the shape written and the shape read have to
 * agree, and keeping them apart is how a field gets added to one and forgotten in the other.
 *
 * `order` is a dependency order, not a preference. An activity rate may point at a park, a park rate
 * points at the global tariff catalogue and the pax categories, and every rate points at a season —
 * so those have to exist before the rates that name them, or the whole import reports "unresolved"
 * and writes nothing useful.
 */
public interface ModuleTransfer {

    /** the key in the bundle: `parks`, `accommodations` */
    String name();

    /** what a person calls it */
    String label();

    /** lower runs first, on both sides */
    int order();

    /** how many records are here now, for the picker and the manifest */
    long count();

    /**
     * True when this module travels only because something else needs it.
     *
     * The tariff catalogue and the pax categories are nobody's idea of an export; they are what park
     * rates are made of. They are pulled in automatically rather than being offered as a choice, and
     * a picker that listed them would be asking a question with only one sensible answer.
     */
    default boolean isSupporting() {
        return false;
    }

    /** modules that must travel with this one, by name */
    default java.util.List<String> requires() {
        return java.util.List.of();
    }

    /**
     * @param files where to add any picture files this module wants carried; the caller zips them.
     *              Left empty when images were not asked for, which is the default.
     */
    JsonNode export(boolean includeImages, java.util.List<com.itineraryledger.kabengosafaris.DataTransfer.TransferFile> files);

    void importInto(JsonNode data, TransferContext context);
}
