package com.itineraryledger.kabengosafaris.Quote.Enums;

import com.itineraryledger.kabengosafaris.GlobalEnums.LineCategoryScope;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A quote's category scope: the typed face of {@link LineCategoryScope}.
 *
 * <p>The rules live there, once, because an invoice needs exactly the same ones and a quote's
 * scope has to survive being invoiced. This keeps the {@code QuoteItemType} signatures the quote
 * services read best.
 */
public final class QuoteItemTypeScope {

    private QuoteItemTypeScope() {}

    /** Canonical text for storage: "ACCOMMODATION,ACTIVITY", or null for everything. */
    public static String canonical(Set<QuoteItemType> types) {
        if (types == null) return null;
        return LineCategoryScope.canonical(
            types.stream().map(Enum::name).collect(Collectors.toSet()), QuoteItemType.class);
    }

    /** The categories named, or every category when nothing is. */
    public static Set<QuoteItemType> parse(String stored) {
        Set<QuoteItemType> found = EnumSet.noneOf(QuoteItemType.class);
        for (String name : LineCategoryScope.parse(stored, QuoteItemType.class)) {
            found.add(QuoteItemType.valueOf(name));
        }
        return found;
    }

    /** Whether this line is inside the scope. A null type is treated as outside a NAMED scope. */
    public static boolean covers(String stored, QuoteItemType type) {
        return LineCategoryScope.covers(stored, type);
    }

    /** For a note on the quote: "accommodation only", or "every line". */
    public static String describe(String stored) {
        return LineCategoryScope.describe(stored, QuoteItemType.class);
    }
}
