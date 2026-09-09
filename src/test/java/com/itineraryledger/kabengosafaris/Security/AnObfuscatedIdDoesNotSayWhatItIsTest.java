package com.itineraryledger.kabengosafaris.Security;

import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * An obfuscated id names a NUMBER, not a record.
 *
 * <p>There is one Hashids instance for the whole application, with no per-table discriminator, so
 * the forty-first stay and the forty-first park visit encode to the same string. Anything that
 * decides identity from an id alone is therefore comparing two different records and calling them
 * one.
 *
 * <p>That is not theoretical. A bill for a lodge reported the park fees of unrelated days as
 * already invoiced, because the coverage list was keyed on the id and not on the kind: the day tree
 * had always compared {@code subjectType} as well, and one drawer had not. The only way past the
 * warning was to raise a duplicate bill.
 *
 * <p>This test exists to make that property visible where somebody will meet it, rather than in a
 * support call about a supplier being paid twice. If it ever fails because ids became
 * type-distinct, the pairing rule can be relaxed on purpose -- never by accident.
 */
class AnObfuscatedIdDoesNotSayWhatItIsTest {

    private IdObfuscator obfuscator() {
        SecuritySettingsGetterServices settings = mock(SecuritySettingsGetterServices.class);
        when(settings.getIdObfuscationLength()).thenReturn(70);
        when(settings.getIdObfuscationSaltLength()).thenReturn(21);
        return new IdObfuscator(settings, "a-fixed-salt-so-this-test-is-not-about-the-salt");
    }

    @Test
    @DisplayName("the same number encodes the same way whatever table it came from")
    void theSameNumberIsTheSameString() {
        IdObfuscator ids = obfuscator();

        String stayFortyOne = ids.encodeId(41L);
        String parkVisitFortyOne = ids.encodeId(41L);

        assertEquals(stayFortyOne, parkVisitFortyOne,
            "one salt, no table in the input: an id carries no kind, so a caller must supply it");
    }

    @Test
    @DisplayName("it round-trips to the number and tells you nothing more")
    void itRoundTripsAndNothingElse() {
        IdObfuscator ids = obfuscator();

        assertEquals(41L, ids.decodeId(ids.encodeId(41L)));
        assertEquals(42L, ids.decodeId(ids.encodeId(42L)),
            "decoding gives back a number; which of the ten tables holding a row 42 is not in it");
    }
}
