package com.itineraryledger.kabengosafaris.Translation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.Translation.Cache.TranslationCache;

/**
 * A translation somebody fixed by hand must not quietly revert to the machine's version.
 *
 * <p>The cache is read before the provider, so a wrong entry is served to every visitor until a
 * person changes it. Entries also carry an expiry, and the provider write path updates whatever
 * row it finds. Put those together without a guard and a correction lasts exactly as long as its
 * TTL: the row lapses, the next request re-translates it, the machine's wording is written back,
 * and nothing anywhere reports that the fix was undone. The person who made it would have no
 * reason to look again.
 *
 * <p>Worth being concrete about what gets corrected, because none of it looks wrong to a reader
 * who does not know the trade: "lodge" came back as "loge" in French and "ложа" in Russian, both
 * a theatre box; "game drive" became "Игровой привод", a mechanical drive for playing games; and
 * "single supplement" became "supplément unique", a one-off charge rather than a single-occupancy
 * one -- a price term a client could dispute in good faith.
 */
@DisplayName("A correction outlives the provider")
class ACorrectionOutlivesTheProviderTest {

    private TranslationCache entry(boolean verified, boolean corrected, LocalDateTime expiresAt) {
        return TranslationCache.builder()
            .name("TC-TEST")
            .contentHash("hash")
            .sourceLanguage("en")
            .targetLanguage("ru")
            .originalContent("The lodge is confirmed for two nights.")
            .translatedContent("Лодж подтверждён на две ночи.")
            .characterCount(38)
            .hitCount(0L)
            .expiresAt(expiresAt)
            .verified(verified)
            .humanCorrected(corrected)
            .build();
    }

    @Test
    @DisplayName("a verified entry never expires, however long ago its TTL lapsed")
    void verifiedNeverExpires() {
        assertFalse(entry(true, true, LocalDateTime.now().minusYears(5)).isExpired());
        assertFalse(entry(true, false, LocalDateTime.now().minusDays(1)).isExpired());
    }

    @Test
    @DisplayName("an ordinary entry still expires on its TTL")
    void unverifiedStillExpires() {
        assertTrue(entry(false, false, LocalDateTime.now().minusHours(1)).isExpired());
        assertFalse(entry(false, false, LocalDateTime.now().plusHours(1)).isExpired());
    }

    @Test
    @DisplayName("a provider may not overwrite anything a person has touched")
    void providerCannotOverwriteHumanWork() {
        assertFalse(entry(true, true, LocalDateTime.now().plusDays(1)).isProviderWritable());
        assertFalse(entry(true, false, LocalDateTime.now().plusDays(1)).isProviderWritable(),
            "verified on its own is 'I read it and it was right' and must still be protected");
        assertFalse(entry(false, true, LocalDateTime.now().plusDays(1)).isProviderWritable(),
            "corrected text must be protected even if verification was later withdrawn");
    }

    @Test
    @DisplayName("an untouched entry is still the provider's to refresh")
    void providerMayRefreshMachineOutput() {
        assertTrue(entry(false, false, LocalDateTime.now().plusDays(1)).isProviderWritable());
    }

    @Test
    @DisplayName("the flags default to false, so existing rows keep their old behaviour")
    void defaultsAreUnprotected() {
        TranslationCache plain = TranslationCache.builder()
            .name("TC-PLAIN").contentHash("h").sourceLanguage("en").targetLanguage("fr")
            .originalContent("x").translatedContent("y").characterCount(1).hitCount(0L)
            .expiresAt(LocalDateTime.now().minusMinutes(1))
            .build();
        assertTrue(plain.isProviderWritable(), "a row nobody has touched must stay refreshable");
        assertTrue(plain.isExpired(), "and must still expire");
    }
}
