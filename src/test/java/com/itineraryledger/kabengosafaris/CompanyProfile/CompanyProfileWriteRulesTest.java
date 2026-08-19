package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyCompletenessDTO;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.*;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.*;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

/**
 * The rules that decide what a company can and cannot save.
 *
 * Mocked repository, no Spring context: every rule here is a decision made in Java, and a decision
 * worth making is worth checking without a database.
 */
class CompanyProfileWriteRulesTest {

    // ------------------------------------------------------------------ completeness

    @Test
    @DisplayName("an empty profile reports every blocking gap, and is not ready for documents")
    void gapsOnAnEmptyProfile() {
        CompanyProfileGetService service = getService(bank(""));

        CompanyCompletenessDTO result = service.completeness(null);

        assertFalse(result.isReadyForDocuments(), "nothing is filled in — documents would print blanks");
        assertEquals(0, result.getFilled());
        List<String> keys = result.getGaps().stream().map(CompanyCompletenessDTO.GapDTO::getKey).toList();
        assertTrue(keys.containsAll(List.of("tradingName", "tin", "email", "phone", "address", "bank")),
            "the gaps that stop an invoice going out must all be listed: " + keys);
        assertTrue(result.getGaps().stream().anyMatch(g -> "BANK".equals(g.getSection())),
            "the bank gap points at the module that fixes it, which is not this page");
    }

    @Test
    @DisplayName("a filled profile is ready even without a VRN — not every company is VAT-registered")
    void vrnIsNotBlocking() {
        CompanyProfileGetService service = getService(bank("0123456789"));

        CompanyProfile profile = filledProfile();
        CompanyCompletenessDTO result = service.completeness(profile);

        assertTrue(result.isReadyForDocuments(),
            "still blocked by: " + result.getGaps().stream()
                .filter(g -> "BLOCKING".equals(g.getSeverity()))
                .map(CompanyCompletenessDTO.GapDTO::getKey).toList());
        assertTrue(result.getGaps().stream().anyMatch(g -> "vrn".equals(g.getKey()))
            || result.getGaps().stream().allMatch(g -> "RECOMMENDED".equals(g.getSeverity())),
            "whatever is left is advice, not a blocker");
    }

    @Test
    @DisplayName("an inactive email does not count — switching one off is how you retire it")
    void inactiveContactsDoNotCount() {
        CompanyProfileGetService service = getService(bank("0123456789"));

        CompanyProfile profile = filledProfile();
        profile.getEmails().forEach(e -> e.setIsActive(false));

        CompanyCompletenessDTO result = service.completeness(profile);

        assertTrue(result.getGaps().stream().anyMatch(g -> "email".equals(g.getKey())),
            "an address nobody is using is not an address documents can print");
        assertFalse(result.isReadyForDocuments());
    }

    // ------------------------------------------------------------------ assets

    @Test
    @DisplayName("an SVG carrying a script is refused, and says why")
    void svgWithScriptIsRefused(@TempDir Path dir) {
        CompanyAssetService service = assetService(dir);

        var response = service.upload("logo-light", new MockMultipartFile("file", "logo.svg",
            "image/svg+xml", "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>fetch('/api')</script></svg>".getBytes()));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(String.valueOf(response.getBody().getMessage()).contains("active content"),
            "the message has to tell somebody what to do about it: " + response.getBody().getMessage());
    }

    @Test
    @DisplayName("a plain SVG is stored, and the email slot refuses one because mail cannot render it")
    void plainSvgStoredButNotForEmail(@TempDir Path dir) throws Exception {
        CompanyAssetService service = assetService(dir);
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><path d=\"M0 0h10v10H0z\"/></svg>".getBytes();

        var ok = service.upload("logo-light", new MockMultipartFile("file", "logo.svg", "image/svg+xml", svg));
        assertEquals(200, ok.getStatusCode().value());
        assertEquals(1, Files.list(dir).count(), "the file landed in the asset directory");

        var refused = service.upload("logo-email", new MockMultipartFile("file", "logo.svg", "image/svg+xml", svg));
        assertEquals(400, refused.getStatusCode().value());
        assertTrue(String.valueOf(refused.getBody().getMessage()).contains("PNG"));
    }

    @Test
    @DisplayName("an unknown slot is a 400 that lists the real ones, not a silent no-op")
    void unknownSlot(@TempDir Path dir) {
        CompanyAssetService service = assetService(dir);

        var response = service.upload("banner", new MockMultipartFile("file", "x.png", "image/png", new byte[] {1}));

        assertEquals(400, response.getStatusCode().value());
        assertTrue(String.valueOf(response.getBody().getMessage()).contains("logo-light"));
    }

    // ------------------------------------------------------------------ fixtures

    private CompanyProfileGetService getService(CompanyIdentityService.BankSnapshot bank) {
        CompanyIdentityService identity = mock(CompanyIdentityService.class);
        CompanyIdentityService.Snapshot snapshot = new CompanyIdentityService.Snapshot(
            "Test Tours", "", "", "", "", "", "", "TZS", "", "", "", "", "",
            List.of(), List.of(), Map.of(), "", "", "", "", bank);
        when(identity.snapshot()).thenReturn(snapshot);

        CompanyProfileGetService service = new CompanyProfileGetService(
            mock(CompanyProfileRepository.class), identity, mock(IdObfuscator.class));
        ReflectionTestUtils.setField(service, "appBaseUrl", "https://api.example.com");
        ReflectionTestUtils.setField(service, "fallbackCompanyName", "Test Tours");
        return service;
    }

    private CompanyAssetService assetService(Path dir) {
        CompanyProfileRepository repository = mock(CompanyProfileRepository.class);
        when(repository.findSingleton()).thenReturn(Optional.empty());
        when(repository.save(any(CompanyProfile.class))).thenAnswer(i -> i.getArgument(0));

        CompanyProfileWriteService writeService = new CompanyProfileWriteService(
            repository, getService(bank("1")), mock(CompanyIdentityService.class), mock(IdObfuscator.class));
        ReflectionTestUtils.setField(writeService, "fallbackCompanyName", "Test Tours");

        CompanyAssetService service = new CompanyAssetService(
            repository, getService(bank("1")), writeService, mock(CompanyIdentityService.class));
        ReflectionTestUtils.setField(service, "storagePath", dir.toString() + "/");
        ReflectionTestUtils.setField(service, "maxBytes", 2097152L);
        return service;
    }

    private CompanyIdentityService.BankSnapshot bank(String accountNumber) {
        return new CompanyIdentityService.BankSnapshot("CRDB", "Test Tours", "Test Tours",
            accountNumber, "", "", "USD");
    }

    private CompanyProfile filledProfile() {
        CompanyProfile profile = CompanyProfile.builder()
            .tradingName("Test Tours").legalName("Test Tours Ltd")
            .tin("123-456-789").defaultCurrency("USD")
            .build();

        profile.getEmails().add(CompanyEmail.builder().email("hello@example.com").isActive(true).isPrimary(true).build());
        profile.getPhones().add(CompanyPhone.builder().phoneNumber("700 000 001").isActive(true).isPrimary(true).build());
        profile.getAddresses().add(CompanyAddress.builder().city("Arusha").isActive(true).isPrimary(true).build());
        profile.getLinks().add(CompanyLink.builder().url("https://example.com").isActive(true).isPrimary(true).build());

        for (CompanyAsset.AssetKind kind : CompanyAsset.AssetKind.values()) {
            profile.getAssets().add(CompanyAsset.builder()
                .assetKind(kind).fileName(kind.name().toLowerCase() + ".svg").isActive(true).build());
        }
        return profile;
    }
}
