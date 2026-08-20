package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.*;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.*;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reads the one company profile, and says what is still missing from it.
 *
 * There is no list endpoint: this is a singleton row. If it does not exist yet (a brand-new
 * database whose initializer has not run) the response is an empty profile rather than a 404 —
 * the Settings page must still render, with every gap showing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyProfileGetService {

    private final CompanyProfileRepository profileRepository;
    private final CompanyIdentityService identityService;
    private final IdObfuscator idObfuscator;

    @Value("${app.base.url:http://localhost:4450}")
    private String appBaseUrl;

    @Value("${app.company.name:}")
    private String fallbackCompanyName;

    @Value("${app.brand.accent:}")
    private String defaultAccent;

    /** Slot labels; the page shows all five whether or not a file has been uploaded. */
    private static final Map<CompanyAsset.AssetKind, String[]> SLOTS = new LinkedHashMap<>();
    static {
        SLOTS.put(CompanyAsset.AssetKind.LOGO_LIGHT,
            new String[] { "Icon logo — light mode",
                "The mark on its own, for light backgrounds: the topbar, the sign-in screen, footers, the website." });
        SLOTS.put(CompanyAsset.AssetKind.LOGO_DARK,
            new String[] { "Icon logo — dark mode",
                "The same mark for dark backgrounds — the panel in dark mode, and a dark website footer." });
        SLOTS.put(CompanyAsset.AssetKind.LOGO_FULL,
            new String[] { "Full logo",
                "Mark plus wordmark, for places with room: letterheads, PDF covers, a website header." });
        SLOTS.put(CompanyAsset.AssetKind.LOGO_FULL_TAGLINE,
            new String[] { "Full logo with tagline",
                "The same lockup carrying the tagline. Right on a cover page; noise in a header." });
        SLOTS.put(CompanyAsset.AssetKind.FAVICON_LIGHT,
            new String[] { "Favicon — light mode", "The browser tab mark when the viewer's system is light." });
        SLOTS.put(CompanyAsset.AssetKind.FAVICON_DARK,
            new String[] { "Favicon — dark mode", "The browser tab mark when the viewer's system is dark." });
        SLOTS.put(CompanyAsset.AssetKind.LOGO_EMAIL,
            new String[] { "Logo — email", "A PNG. Mail clients do not render SVG, so email needs its own raster copy." });
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getCompany() {
        CompanyProfile profile = profileRepository.findSingleton().orElse(null);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("company", toDTO(profile));
        payload.put("completeness", completeness(profile));

        return ResponseEntity.ok(ApiResponse.success(200, "Company profile retrieved successfully", payload));
    }

    /**
     * Every company variable with the value it currently resolves to.
     *
     * The panel needs this to preview a template honestly. A preview that shows `{{companyName}}`
     * where the name goes, and no colour where the accent goes, cannot answer the only question
     * somebody previews a letterhead to ask: does this look right. The business variables stay as
     * placeholders — a customer's name genuinely is not known yet — but who we are is known, so
     * showing a placeholder there is a worse lie than showing the value.
     *
     * Each entry carries both spellings, because the two engines differ: `{{companyName}}` in an
     * email or a signature, `${company.name}` in a Thymeleaf PDF. `path` is null where the model has
     * no equivalent.
     *
     * Any signed-in user may read it. This is the text their templates already print on invoices and
     * letters; withholding it from the person writing the layout protects nothing.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getVariables() {
        Map<String, String> resolved = identityService.variables();
        List<Map<String, Object>> rows = new ArrayList<>();

        for (Map<String, Object> declared : CompanyVariableCatalogue.asEmailVariables()) {
            String name = (String) declared.get("name");
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", name);
            row.put("path", CompanyVariableCatalogue.pdfPath(name));
            row.put("description", declared.get("description"));
            row.put("group", declared.get("group"));
            /*
             * currentYear is not part of the cached snapshot — a copyright line that still says last
             * year on the 2nd of January is exactly the kind of thing nobody reports.
             */
            row.put("value", "currentYear".equals(name)
                ? String.valueOf(java.time.Year.now().getValue())
                : resolved.getOrDefault(name, ""));
            rows.add(row);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("variables", rows);
        return ResponseEntity.ok(ApiResponse.success(200, "Company variables retrieved successfully", payload));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getCompleteness() {
        CompanyProfile profile = profileRepository.findSingleton().orElse(null);
        return ResponseEntity.ok(ApiResponse.success(200, "Company completeness retrieved successfully",
            completeness(profile)));
    }

    /** The public face: brand and contact only. Tax numbers are for documents, not for the web. */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getPublicCompany() {
        CompanyIdentityService.Snapshot snapshot = identityService.snapshot();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", snapshot.name());
        payload.put("legalName", snapshot.legalName());
        payload.put("tagline", snapshot.tagline());
        payload.put("email", snapshot.email());
        payload.put("phone", snapshot.phone());
        payload.put("address", snapshot.address());
        payload.put("website", snapshot.website());
        payload.put("emails", snapshot.emails());
        payload.put("phones", snapshot.phones());
        payload.put("logoUrl", snapshot.logoLightUrl());
        payload.put("logoFullUrl", snapshot.logoFullUrl());
        payload.put("logoFullTaglineUrl", snapshot.logoFullTaglineUrl());
        payload.put("logoDarkUrl", snapshot.logoDarkUrl());
        payload.put("faviconUrl", snapshot.faviconUrl());
        payload.put("defaultCurrency", snapshot.defaultCurrency());

        return ResponseEntity.ok(ApiResponse.success(200, "Company retrieved successfully", payload));
    }

    /**
     * What the app needs to look like this company, before anybody has logged in.
     *
     * Public and deliberately small: the login screen renders before there is a token, and it has to
     * render in the right colour with the right mark — a build pointed at another company's API must
     * not flash this company's brand. Nothing sensitive is here; a name and a logo are published by
     * definition.
     *
     * Every field can be empty. Empty means "use whatever the app ships with", so an installation
     * that never opens the Brand tab is unaffected by this endpoint existing.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getBrand() {
        CompanyProfile profile = profileRepository.findSingleton().orElse(null);
        CompanyIdentityService.Snapshot snapshot = identityService.snapshot();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", snapshot.name());
        payload.put("tagline", snapshot.tagline());
        /*
         * The letter mark, drawn when there is no logo file. The company's own choice if it made one —
         * an initial is a poor mark for "Jatelo African Travels" — otherwise the first letter.
         */
        String chosenMark = profile == null ? null : profile.getBrandMark();
        payload.put("mark", chosenMark != null && !chosenMark.isBlank()
            ? chosenMark.trim()
            : (snapshot.name().isBlank() ? "" : snapshot.name().substring(0, 1).toUpperCase()));

        /*
         * Contact details, because the sign-in screen offers "trouble signing in?" and it has to
         * offer a real address. Already public through /api/public/company — repeated here so the
         * login screen needs one request, not two.
         */
        payload.put("email", snapshot.email());
        payload.put("website", snapshot.website());

        payload.put("accent", orDefault(profile == null ? null : profile.getBrandAccent(), defaultAccent));
        payload.put("radius", orDefault(profile == null ? null : profile.getBrandRadius(), ""));
        payload.put("font", orDefault(profile == null ? null : profile.getBrandFont(), ""));

        payload.put("logoLightUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.LOGO_LIGHT));
        /* the whole lockup, for a header or a cover page rather than a 28px topbar */
        payload.put("logoFullUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.LOGO_FULL));
        payload.put("logoFullTaglineUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.LOGO_FULL_TAGLINE));
        payload.put("logoDarkUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.LOGO_DARK));
        payload.put("faviconLightUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.FAVICON_LIGHT));
        payload.put("faviconDarkUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.FAVICON_DARK));
        payload.put("logoEmailUrl", assetUrlIfPresent(profile, CompanyAsset.AssetKind.LOGO_EMAIL));

        return ResponseEntity.ok(ApiResponse.success(200, "Brand retrieved successfully", payload));
    }

    private String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? (fallback == null ? "" : fallback) : value.trim();
    }

    /** Empty rather than a URL that 404s: the client falls back to its own asset. */
    private String assetUrlIfPresent(CompanyProfile profile, CompanyAsset.AssetKind kind) {
        if (profile == null) return "";
        boolean present = profile.getAssets().stream()
            .anyMatch(a -> a.getAssetKind() == kind && Boolean.TRUE.equals(a.getIsActive()));
        return present ? publicAssetUrl(kind) : "";
    }

    // ------------------------------------------------------------------ mapping

    public CompanyProfileDTO toDTO(CompanyProfile profile) {
        if (profile == null) {
            return CompanyProfileDTO.builder()
                .tradingName(fallbackCompanyName == null ? "" : fallbackCompanyName)
                .emails(List.of()).phones(List.of()).addresses(List.of()).links(List.of())
                .assets(assetSlots(null))
                .resolved(CompanyProfileDTO.ResolvedDTO.builder().name(fallbackCompanyName).build())
                .build();
        }

        CompanyIdentityService.Snapshot snapshot = identityService.snapshot();

        return CompanyProfileDTO.builder()
            .tradingName(profile.getTradingName())
            .legalName(profile.getLegalName())
            .tagline(profile.getTagline())
            .tin(profile.getTin())
            .vrn(profile.getVrn())
            .registrationNumber(profile.getRegistrationNumber())
            .licenceNumber(profile.getLicenceNumber())
            .defaultCurrency(profile.getDefaultCurrency())
            .timezone(profile.getTimezone())
            .locale(profile.getLocale())
            .brandMark(profile.getBrandMark())
            .brandAccent(profile.getBrandAccent())
            .brandRadius(profile.getBrandRadius())
            .brandFont(profile.getBrandFont())
            .emails(profile.getEmails().stream().sorted(byOrder(CompanyEmail::getIsPrimary, CompanyEmail::getDisplayOrder, CompanyEmail::getId)).map(this::toDTO).toList())
            .phones(profile.getPhones().stream().sorted(byOrder(CompanyPhone::getIsPrimary, CompanyPhone::getDisplayOrder, CompanyPhone::getId)).map(this::toDTO).toList())
            .addresses(profile.getAddresses().stream().sorted(byOrder(CompanyAddress::getIsPrimary, CompanyAddress::getDisplayOrder, CompanyAddress::getId)).map(this::toDTO).toList())
            .links(profile.getLinks().stream().sorted(byOrder(CompanyLink::getIsPrimary, CompanyLink::getDisplayOrder, CompanyLink::getId)).map(this::toDTO).toList())
            .assets(assetSlots(profile))
            .resolved(CompanyProfileDTO.ResolvedDTO.builder()
                .name(snapshot.name())
                .legalName(snapshot.legalName())
                .email(snapshot.email())
                .phone(snapshot.phone())
                .address(snapshot.address())
                .website(snapshot.website())
                .logoUrl(snapshot.logoLightUrl())
                .logoDarkUrl(snapshot.logoDarkUrl())
                .faviconUrl(snapshot.faviconUrl())
                .build())
            .createdAt(profile.getCreatedAt())
            .updatedAt(profile.getUpdatedAt())
            .build();
    }

    /**
     * Primary first, then display order, then id — the same order {@link CompanyIdentityService}
     * picks a primary in, so the top row of the panel is always the one documents print.
     */
    private <T> Comparator<T> byOrder(java.util.function.Function<T, Boolean> primary,
                                      java.util.function.Function<T, Integer> order,
                                      java.util.function.Function<T, Long> id) {
        return Comparator
            .comparing((T t) -> Boolean.TRUE.equals(primary.apply(t)) ? 0 : 1)
            .thenComparing(t -> order.apply(t) == null ? 0 : order.apply(t))
            .thenComparing(t -> id.apply(t) == null ? 0L : id.apply(t));
    }

    public CompanyEmailDTO toDTO(CompanyEmail e) {
        return CompanyEmailDTO.builder()
            .id(idObfuscator.encodeId(e.getId()))
            .email(e.getEmail())
            .emailType(e.getEmailType() == null ? null : e.getEmailType().name())
            .label(e.getLabel())
            .isPrimary(e.getIsPrimary())
            .isActive(e.getIsActive())
            .displayOrder(e.getDisplayOrder())
            .build();
    }

    public CompanyPhoneDTO toDTO(CompanyPhone p) {
        return CompanyPhoneDTO.builder()
            .id(idObfuscator.encodeId(p.getId()))
            .countryCode(p.getCountryCode())
            .phoneNumber(p.getPhoneNumber())
            .formatted(p.formatted())
            .phoneType(p.getPhoneType() == null ? null : p.getPhoneType().name())
            .label(p.getLabel())
            .isWhatsApp(p.getIsWhatsApp())
            .operatingHours(p.getOperatingHours())
            .isPrimary(p.getIsPrimary())
            .isActive(p.getIsActive())
            .displayOrder(p.getDisplayOrder())
            .build();
    }

    public CompanyAddressDTO toDTO(CompanyAddress a) {
        return CompanyAddressDTO.builder()
            .id(idObfuscator.encodeId(a.getId()))
            .addressType(a.getAddressType() == null ? null : a.getAddressType().name())
            .label(a.getLabel())
            .lineOne(a.getLineOne())
            .lineTwo(a.getLineTwo())
            .city(a.getCity())
            .region(a.getRegion())
            .postalCode(a.getPostalCode())
            .country(a.getCountry())
            .formatted(a.formatted())
            .isPrimary(a.getIsPrimary())
            .isActive(a.getIsActive())
            .displayOrder(a.getDisplayOrder())
            .build();
    }

    public CompanyLinkDTO toDTO(CompanyLink l) {
        return CompanyLinkDTO.builder()
            .id(idObfuscator.encodeId(l.getId()))
            .url(l.getUrl())
            .display(l.display())
            .linkType(l.getLinkType() == null ? null : l.getLinkType().name())
            .label(l.getLabel())
            .isPrimary(l.getIsPrimary())
            .isActive(l.getIsActive())
            .displayOrder(l.getDisplayOrder())
            .build();
    }

    /** All five slots, every time — an empty slot is the point of the panel. */
    public List<CompanyAssetDTO> assetSlots(CompanyProfile profile) {
        List<CompanyAssetDTO> out = new ArrayList<>();

        for (Map.Entry<CompanyAsset.AssetKind, String[]> slot : SLOTS.entrySet()) {
            CompanyAsset.AssetKind kind = slot.getKey();
            CompanyAsset asset = profile == null ? null : profile.getAssets().stream()
                .filter(a -> a.getAssetKind() == kind)
                .findFirst().orElse(null);

            out.add(CompanyAssetDTO.builder()
                .assetKind(kind.name())
                .label(slot.getValue()[0])
                .hint(slot.getValue()[1])
                .present(asset != null)
                .originalFileName(asset == null ? null : asset.getOriginalFileName())
                .mimeType(asset == null ? null : asset.getMimeType())
                .fileSize(asset == null ? null : asset.getFileSize())
                .fileSizeFormatted(asset == null ? null : formatFileSize(asset.getFileSize()))
                .url(asset == null ? null : publicAssetUrl(kind))
                .safeForEmail(asset == null ? null : asset.safeForEmail())
                .isActive(asset == null ? null : asset.getIsActive())
                .updatedAt(asset == null ? null : asset.getUpdatedAt())
                .build());
        }
        return out;
    }

    private String publicAssetUrl(CompanyAsset.AssetKind kind) {
        String base = appBaseUrl == null ? "" : appBaseUrl.replaceAll("/+$", "");
        return base + "/api/public/company/assets/" + kind.name().toLowerCase().replace('_', '-');
    }

    // ------------------------------------------------------------------ completeness

    /**
     * The gap list.
     *
     * BLOCKING means a document or an email will print a blank where this belongs. RECOMMENDED
     * means it merely looks unfinished. Nothing here is enforced on save — the profile of a company
     * that has not registered for VAT is complete without a VRN, and the page should say so rather
     * than nag.
     */
    public CompanyCompletenessDTO completeness(CompanyProfile profile) {
        List<CompanyCompletenessDTO.GapDTO> gaps = new ArrayList<>();
        int total = 0, filled = 0;

        // --- identity
        total++; if (has(profile == null ? null : profile.getTradingName())) filled++; else
            gaps.add(gap("tradingName", "Trading name", "BLOCKING",
                "Every document, email and browser tab prints the company name.", "IDENTITY"));

        total++; if (has(profile == null ? null : profile.getLegalName())) filled++; else
            gaps.add(gap("legalName", "Legal name", "RECOMMENDED",
                "Invoices and contracts fall back to the trading name, which may not be the registered entity.", "IDENTITY"));

        total++; if (has(profile == null ? null : profile.getTin())) filled++; else
            gaps.add(gap("tin", "TIN", "BLOCKING",
                "Tanzanian invoices must carry the taxpayer identification number.", "IDENTITY"));

        total++; if (has(profile == null ? null : profile.getDefaultCurrency())) filled++; else
            gaps.add(gap("defaultCurrency", "Default currency", "RECOMMENDED",
                "New quotes and invoices have to guess a currency without it.", "IDENTITY"));

        // --- collections: at least one ACTIVE entry each
        total++; if (anyActive(profile == null ? null : profile.getEmails(), CompanyEmail::getIsActive)) filled++; else
            gaps.add(gap("email", "An active email address", "BLOCKING",
                "Documents and email footers print no reply address, and clients cannot answer.", "EMAILS"));

        total++; if (anyActive(profile == null ? null : profile.getPhones(), CompanyPhone::getIsActive)) filled++; else
            gaps.add(gap("phone", "An active phone number", "BLOCKING",
                "Vouchers and confirmations print no number for a traveller who needs help.", "PHONES"));

        total++; if (anyActive(profile == null ? null : profile.getAddresses(), CompanyAddress::getIsActive)) filled++; else
            gaps.add(gap("address", "An active address", "BLOCKING",
                "Invoice headers print a blank address block.", "ADDRESSES"));

        total++; if (anyActive(profile == null ? null : profile.getLinks(), CompanyLink::getIsActive)) filled++; else
            gaps.add(gap("website", "A website link", "RECOMMENDED",
                "Footers and signatures omit the website.", "LINKS"));

        // --- assets
        for (Map.Entry<CompanyAsset.AssetKind, String[]> slot : SLOTS.entrySet()) {
            CompanyAsset.AssetKind kind = slot.getKey();
            boolean present = profile != null && profile.getAssets().stream()
                .anyMatch(a -> a.getAssetKind() == kind && Boolean.TRUE.equals(a.getIsActive()));
            total++;
            if (present) { filled++; continue; }

            boolean blocking = kind == CompanyAsset.AssetKind.LOGO_LIGHT;
            gaps.add(gap("asset." + kind.name(), slot.getValue()[0], blocking ? "BLOCKING" : "RECOMMENDED",
                kind == CompanyAsset.AssetKind.LOGO_EMAIL
                    ? "Outgoing email shows a broken image, or falls back to the SVG that mail clients cannot render."
                    : slot.getValue()[1],
                "ASSETS"));
        }

        // --- the one thing that lives in another module
        CompanyIdentityService.BankSnapshot bank = identityService.snapshot().bank();
        total++;
        if (has(bank.accountNumber())) filled++; else
            gaps.add(gap("bank", "A default bank account", "BLOCKING",
                "Proforma invoices print no payment details, so nobody can pay. Set one in Finance → Bank accounts.", "BANK"));

        boolean ready = gaps.stream().noneMatch(g -> "BLOCKING".equals(g.getSeverity()));

        return CompanyCompletenessDTO.builder()
            .filled(filled)
            .total(total)
            .percent(total == 0 ? 100 : (int) Math.round(filled * 100.0 / total))
            .readyForDocuments(ready)
            .gaps(gaps)
            .build();
    }

    private CompanyCompletenessDTO.GapDTO gap(String key, String label, String severity,
                                              String consequence, String section) {
        return CompanyCompletenessDTO.GapDTO.builder()
            .key(key).label(label).severity(severity).consequence(consequence).section(section).build();
    }

    private boolean has(String value) {
        return value != null && !value.isBlank();
    }

    private <T> boolean anyActive(List<T> items, java.util.function.Function<T, Boolean> active) {
        return items != null && items.stream().anyMatch(i -> Boolean.TRUE.equals(active.apply(i)));
    }

    static String formatFileSize(Long bytes) {
        if (bytes == null) return null;
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
