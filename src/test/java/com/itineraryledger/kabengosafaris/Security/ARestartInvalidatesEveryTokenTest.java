package com.itineraryledger.kabengosafaris.Security;

import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A restart invalidates every token, and that is the design.
 *
 * <p>The signing key is generated at startup and never leaves memory, so there is nothing in
 * configuration or in a backup to leak and no rotation to remember. The price is paid at every
 * deploy: everybody is signed out, and anything holding a token authenticates again. That trade
 * has been made deliberately, so a token that outlived a restart would be the bug.
 *
 * <p>What must NOT follow from it is a thrown exception. Because keys change this often, a token
 * signed with a key this instance never had is the ordinary case rather than an attack, and every
 * caller asks {@code validateToken} as a yes-or-no question.
 */
class ARestartInvalidatesEveryTokenTest {

    private SecuritySettingsGetterServices settings() {
        SecuritySettingsGetterServices s = mock(SecuritySettingsGetterServices.class);
        when(s.getJwtExpirationMinutes()).thenReturn(300L);
        when(s.getMFAJwtExpirationMinutes()).thenReturn(180L);
        when(s.getRegistrationJwtExpirationMinutes()).thenReturn(60L);
        when(s.getPasswordResetJwtExpirationMinutes()).thenReturn(30L);
        return s;
    }

    @Test
    @DisplayName("a token is good to the instance that minted it")
    void aFreshTokenVerifies() {
        JwtTokenProvider provider = new JwtTokenProvider(settings());

        String token = provider.generateTokenFromUsername("ricksy_faby");

        assertTrue(provider.validateToken(token));
        assertEquals("ricksy_faby", provider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("and worthless to the next one, which is the point")
    void aRestartSignsEverybodyOut() {
        JwtTokenProvider before = new JwtTokenProvider(settings());
        String token = before.generateTokenFromUsername("ricksy_faby");

        // the same deployment, started again: a new key, so nothing minted earlier verifies
        JwtTokenProvider after = new JwtTokenProvider(settings());

        assertFalse(after.validateToken(token),
            "a session outliving a restart would mean the key had been persisted, which this "
            + "deployment has deliberately chosen not to do");
    }

    @Test
    @DisplayName("a token from another key is refused with a false, never a thrown exception")
    void aForeignSignatureAnswersNo() {
        JwtTokenProvider mine = new JwtTokenProvider(settings());
        JwtTokenProvider theirs = new JwtTokenProvider(settings());

        String foreign = theirs.generateTokenFromUsername("somebody_else");

        /*
         * This is the case that was mishandled. validateToken caught java.lang.SecurityException,
         * which a JWT library has no reason to raise; jjwt throws its own SignatureException, so
         * a mismatched signature came out of here as an exception rather than a false. With the
         * key changing at every restart, that is not a rare adversarial case -- it is what every
         * still-open browser tab sends the moment a deploy finishes.
         */
        assertFalse(mine.validateToken(foreign),
            "five callers treat this as a yes-or-no question and none of them expect a throw");
    }

    @Test
    @DisplayName("nonsense in the header is refused the same quiet way")
    void rubbishAnswersNo() {
        JwtTokenProvider provider = new JwtTokenProvider(settings());

        assertFalse(provider.validateToken("not-a-token"));
        assertFalse(provider.validateToken("a.b.c"));
        assertFalse(provider.validateToken(""));
    }

    @Test
    @DisplayName("two instances really do get different keys")
    void everyStartupIsANewKey() {
        JwtTokenProvider one = new JwtTokenProvider(settings());
        JwtTokenProvider two = new JwtTokenProvider(settings());

        assertNotEquals(one.generateTokenFromUsername("ricksy_faby"),
                        two.generateTokenFromUsername("ricksy_faby"),
            "identical tokens would mean the key was not being regenerated at all");
    }
}
