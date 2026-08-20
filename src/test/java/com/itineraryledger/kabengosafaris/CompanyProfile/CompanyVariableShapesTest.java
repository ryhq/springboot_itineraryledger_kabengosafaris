package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;

/**
 * That each variable holds the KIND of thing its name promises.
 *
 * Every field of the identity snapshot is a String, so putting a URL where markup belongs compiles
 * perfectly and fails in front of a client: a cost estimation printed
 * "https://api…/assets/logo-full-tagline" as text in the space reserved for the logo, because two
 * fields had been added to the middle of the record while the positional constructor kept its old
 * order.
 *
 * The constructor is a builder now, which prevents the mistake. This checks the promise anyway,
 * because "they are all Strings" is precisely why the compiler cannot.
 */
class CompanyVariableShapesTest {

    private Map<String, String> variables() {
        return CompanyIdentityService.Snapshot.builder()
            .name("Test Tours").legalName("Test Tours Ltd").tagline("Go well")
            .tin("103").vrn("").registrationNumber("").licenceNumber("").defaultCurrency("USD")
            .email("hello@example.com").phone("+255 700 000 001").phoneSecondary("")
            .address("Arusha, Tanzania").website("example.com")
            .emails(List.of("hello@example.com")).phones(List.of("+255 700 000 001")).socials(Map.of())
            .logoUrl("https://api.example.com/api/public/company/assets/logo-email")
            .logoLightUrl("https://api.example.com/api/public/company/assets/logo-light")
            .logoDarkUrl("https://api.example.com/api/public/company/assets/logo-dark")
            .faviconUrl("https://api.example.com/api/public/company/assets/favicon-light")
            .logoFullUrl("https://api.example.com/api/public/company/assets/logo-full")
            .logoFullTaglineUrl("https://api.example.com/api/public/company/assets/logo-full-tagline")
            .logoMarkup("<svg xmlns=\"http://www.w3.org/2000/svg\"><path d=\"M0 0h9v9H0z\"/></svg>")
            .logoFullMarkup("<img src=\"data:image/png;base64,AAAA\" alt=\"\" />")
            .accent("#014225").accentContrast("#ffffff").accentDark("#00291a").accentSoft("#e6f0ec")
            .radius("12px").font("Georgia, serif")
            .bank(CompanyIdentityService.BankSnapshot.empty())
            .build()
            .variables();
    }

    @Test
    @DisplayName("a URL variable holds a URL, and only a URL")
    void urlsAreUrls() {
        Map<String, String> vars = variables();
        for (String name : List.of("companyLogoUrl", "companyLogoLightUrl", "companyLogoDarkUrl",
            "companyLogoFullUrl", "companyLogoFullTaglineUrl", "companyFaviconUrl")) {
            String value = vars.get(name);
            assertNotNull(value, name + " is not supplied at all");
            assertTrue(value.startsWith("http"), name + " should be a URL but was: " + value);
            assertFalse(value.contains("<"), name + " should be a URL, not markup: " + value);
        }
    }

    @Test
    @DisplayName("a markup variable holds markup — the mix-up that printed a URL on an invoice")
    void markupIsMarkup() {
        Map<String, String> vars = variables();
        for (String name : List.of("companyLogoMarkup", "companyLogoFullMarkup")) {
            String value = vars.get(name);
            assertNotNull(value, name + " is not supplied at all");
            assertTrue(value.startsWith("<svg") || value.startsWith("<img"),
                name + " must be drawable markup, but was: " + value
                    + " — a URL here prints as text where the logo belongs");
        }
    }

    @Test
    @DisplayName("a colour is a colour and a length is a length")
    void brandValuesAreUsable() {
        Map<String, String> vars = variables();
        for (String name : List.of("companyAccent", "companyAccentContrast", "companyAccentDark",
            "companyAccentSoft")) {
            assertTrue(vars.get(name).matches("#[0-9a-fA-F]{6}"),
                name + " must be a hex colour usable in CSS, but was: " + vars.get(name));
        }
        assertTrue(vars.get("companyRadius").matches("\\d+(px|rem)"),
            "the radius must be a CSS length, but was: " + vars.get("companyRadius"));
    }
}
