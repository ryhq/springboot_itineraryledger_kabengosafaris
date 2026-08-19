package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyAddress;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyLink;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyPhone;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;

/**
 * What a document prints about the company.
 *
 * No Spring and no database on purpose: this is the part that decides how a phone number, an address
 * and a website LOOK on an invoice, and it should be checkable in a second on any machine — including
 * a CI runner with no MySQL, which is why the rest of this project's tests do not run.
 */
class CompanyIdentityFormattingTest {

    @Test
    @DisplayName("a number prints with its country code, and survives not having one")
    void phoneFormatting() {
        assertEquals("+255 746 598 330", phone("+255", "746 598 330").formatted());
        assertEquals("+255 746 598 330", phone("255", "746 598 330").formatted(), "a missing + is added");
        assertEquals("746 598 330", phone(null, "746 598 330").formatted(), "no code, no stray space");
        assertEquals("", phone(null, null).formatted(), "nothing to print is empty, never 'null'");
    }

    @Test
    @DisplayName("an address skips the parts that are not filled in")
    void addressFormatting() {
        CompanyAddress full = CompanyAddress.builder()
            .lineOne("P.O. Box 11271").city("Arusha").country("Tanzania").build();
        assertEquals("P.O. Box 11271, Arusha, Tanzania", full.formatted());

        CompanyAddress sparse = CompanyAddress.builder().city("Arusha").country("Tanzania").build();
        assertEquals("Arusha, Tanzania", sparse.formatted(), "no empty commas where a line is missing");

        assertEquals("", CompanyAddress.builder().lineOne("  ").build().formatted(), "blank is not a line");
    }

    @Test
    @DisplayName("a website prints as an address, not as a URL")
    void linkDisplay() {
        assertEquals("www.kabengosafaris.com",
            CompanyLink.builder().url("https://www.kabengosafaris.com").build().display());
        assertEquals("www.example.com",
            CompanyLink.builder().url("http://www.example.com/").build().display(), "trailing slash goes");
        assertEquals("", CompanyLink.builder().url(null).build().display());
    }

    @Test
    @DisplayName("every documented variable exists, and none of them is null")
    void variablesAreComplete() {
        CompanyIdentityService.Snapshot snapshot = new CompanyIdentityService.Snapshot(
            "Kabengo Safaris", "", "Tailor-made safaris",
            "103-035-856", "", "", "", "TZS",
            "info@kabengosafaris.com", "+255 746 598 330", "+255 786 345 408", "Arusha, Tanzania", "www.kabengosafaris.com",
            List.of("info@kabengosafaris.com"), List.of("+255 746 598 330"),
            Map.of("INSTAGRAM", "https://instagram.com/example"),
            "https://api.example.com/api/public/company/assets/logo-email", "", "", "",
            new CompanyIdentityService.BankSnapshot("CRDB", "Kabengo USD", "Kabengo Safaris Ltd",
                "42810007750", "NMIBTZTZ", "", "USD"));

        Map<String, String> vars = snapshot.variables();
        for (String key : List.of("companyName", "companyLegalName", "companyEmail", "companyPhone",
                "companyAddress", "companyWebsite", "companyTin", "companyVrn", "companyLogoUrl",
                "brandName", "bankName", "bankAccountNumber", "bankSwift")) {
            assertTrue(vars.containsKey(key), key + " must be offered to every template");
            assertNotNull(vars.get(key), key + " must never be null — a template would print 'null'");
        }
        assertEquals("Kabengo Safaris", vars.get("companyLegalName"),
            "with no legal name recorded, the trading name stands in rather than leaving a blank");
        assertEquals("Kabengo Safaris", vars.get("brandName"), "older templates ask for brandName");
    }

    @Test
    @DisplayName("an unfilled profile renders empty strings, not the word null")
    void emptyProfileIsSafe() {
        Map<String, String> vars = CompanyIdentityService.Snapshot
            .empty("Kabengo Safaris").variables();
        assertEquals("Kabengo Safaris", vars.get("companyName"), "the configured name is the last resort");
        assertEquals("", vars.get("companyTin"), "a gap prints as nothing and shows in the UI as missing");
        assertEquals("", vars.get("bankSwift"));
        assertFalse(vars.containsValue(null));
    }

    private static CompanyPhone phone(String code, String number) {
        return CompanyPhone.builder().countryCode(code).phoneNumber(number).build();
    }
}
