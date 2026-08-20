package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.*;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Who we are, as the renderers need it.
 *
 * One read model serving three callers that used to have the answer typed into them: the email
 * templates ({@code {{companyName}}}), the PDF templates ({@code ${company.name}}) and the
 * "send me a test" path. Missing a caller is how a template gets fixed and the test email still
 * says the wrong company.
 *
 * Bank details are resolved from the Bank accounts module, not stored here — the invoice PDF already
 * reads that module, and a sort code with two homes is a sort code that will disagree with itself.
 *
 * Cached, because rendering one letter asks for nine fields and a PDF asks for more. Any write to
 * the profile clears it; nothing else can, so the cache cannot go stale behind an edit.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyIdentityService {

    private final CompanyProfileRepository profileRepository;
    private final BankAccountRepository bankAccountRepository;

    /** Last resort when no profile row exists yet — a fresh install, before anybody has filled it. */
    @Value("${app.company.name:}")
    private String fallbackCompanyName;

    @Value("${app.base.url:}")
    private String baseUrl;

    /** the deployment's accent, used until the profile carries one */
    @Value("${app.brand.accent:#1c7a58}")
    private String defaultAccent;

    @Value("${company.asset.storage.path:./data/company-assets/}")
    private String assetStoragePath;

    private final AtomicReference<Snapshot> cached = new AtomicReference<>();

    /** Anything that changes the profile calls this; nothing else needs to know the cache exists. */
    public void invalidate() {
        cached.set(null);
        log.debug("Company identity cache cleared");
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot() {
        Snapshot current = cached.get();
        if (current != null) return current;
        Snapshot built = build();
        cached.set(built);
        return built;
    }

    /** The `{{name}}` map for email templates and signatures. */
    @Transactional(readOnly = true)
    public Map<String, String> variables() {
        return snapshot().variables();
    }

    /**
     * The company as a Thymeleaf model: {@code ${company.name}}, {@code ${company.bank.swift}}.
     *
     * Same data as {@link #snapshot()}, shaped for SpEL — see CompanyTemplateModel for why a record
     * would not do.
     */
    @Transactional(readOnly = true)
    public com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyTemplateModel templateModel() {
        Snapshot s = snapshot();
        return com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyTemplateModel.builder()
            .name(s.name())
            .legalName(s.legalName())
            .tagline(s.tagline())
            .email(s.email())
            .phone(s.phone())
            .phoneSecondary(s.phoneSecondary())
            .address(s.address())
            .website(s.website())
            .tin(s.tin())
            .vrn(s.vrn())
            .registrationNumber(s.registrationNumber())
            .licenceNumber(s.licenceNumber())
            .currency(s.defaultCurrency())
            .emails(s.emails())
            .phones(s.phones())
            .socials(s.socials())
            .logoUrl(s.logoUrl())
            .logoLightUrl(s.logoLightUrl())
            .logoDarkUrl(s.logoDarkUrl())
            .faviconUrl(s.faviconUrl())
            .logoFullUrl(s.logoFullUrl())
            .logoFullTaglineUrl(s.logoFullTaglineUrl())
            .logoMarkup(s.logoMarkup())
            .logoFullMarkup(s.logoFullMarkup())
            .accent(s.accent())
            .accentContrast(s.accentContrast())
            .accentDark(s.accentDark())
            .accentSoft(s.accentSoft())
            .radius(s.radius())
            .font(s.font())
            /* not cached with the snapshot: a copyright line has to be right on the 2nd of January */
            .year(java.time.Year.now().getValue())
            .bank(com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyTemplateModel.Bank.builder()
                .bankName(s.bank().bankName())
                .accountName(s.bank().accountName())
                .accountHolder(s.bank().accountHolder())
                .accountNumber(s.bank().accountNumber())
                .swift(s.bank().swift())
                .iban(s.bank().iban())
                .currency(s.bank().currency())
                .build())
            .build();
    }

    @Transactional(readOnly = true)
    public Snapshot view() {
        return snapshot();
    }

    private Snapshot build() {
        CompanyProfile profile = profileRepository.findSingleton().orElse(null);

        if (profile == null) {
            log.warn("No company profile row yet — documents will carry the fallback name only");
            return Snapshot.empty(fallbackCompanyName);
        }

        String email = pick(profile.getEmails(), CompanyEmail::getIsPrimary, CompanyEmail::getIsActive,
            CompanyEmail::getDisplayOrder).map(CompanyEmail::getEmail).orElse("");
        String phone = pick(profile.getPhones(), CompanyPhone::getIsPrimary, CompanyPhone::getIsActive,
            CompanyPhone::getDisplayOrder).map(CompanyPhone::formatted).orElse("");
        String address = pick(profile.getAddresses(), CompanyAddress::getIsPrimary, CompanyAddress::getIsActive,
            CompanyAddress::getDisplayOrder).map(CompanyAddress::formatted).orElse("");
        String website = profile.getLinks().stream()
            .filter(l -> Boolean.TRUE.equals(l.getIsActive()) && l.getLinkType() == CompanyLink.LinkType.WEBSITE)
            .sorted(Comparator.comparing((CompanyLink l) -> !Boolean.TRUE.equals(l.getIsPrimary()))
                .thenComparing(l -> l.getDisplayOrder() == null ? 0 : l.getDisplayOrder()))
            .findFirst().map(CompanyLink::display).orElse("");

        /*
         * Every active address and number, for the footers that print all of them — a company with
         * one office and a WhatsApp line should not have to choose which one a voucher shows.
         */
        List<String> allEmails = profile.getEmails().stream()
            .filter(e -> Boolean.TRUE.equals(e.getIsActive())).map(CompanyEmail::getEmail).toList();
        List<String> allPhones = profile.getPhones().stream()
            .filter(p -> Boolean.TRUE.equals(p.getIsActive())).map(CompanyPhone::formatted).toList();

        /*
         * The second number, separately.
         *
         * Letterheads print two numbers side by side. Filling both from `phone` would print the same
         * number twice, which looks like a mistake because it is one; a company with a single line
         * gets an empty string here and the template's section for it disappears.
         */
        String phoneSecondary = allPhones.size() > 1 ? allPhones.get(1) : "";

        /* one url per platform: primary first among the active ones of that type */
        Map<String, String> socials = new LinkedHashMap<>();
        for (CompanyLink.LinkType type : CompanyLink.LinkType.values()) {
            if (type == CompanyLink.LinkType.WEBSITE) continue;
            profile.getLinks().stream()
                .filter(l -> Boolean.TRUE.equals(l.getIsActive()) && l.getLinkType() == type)
                .sorted(Comparator.comparing((CompanyLink l) -> !Boolean.TRUE.equals(l.getIsPrimary()))
                    .thenComparing(l -> l.getDisplayOrder() == null ? 0 : l.getDisplayOrder()))
                .findFirst()
                .ifPresent(l -> socials.put(type.name(), l.getUrl()));
        }

        String brandAccent = orEmpty(profile.getBrandAccent()).isBlank()
            ? orEmpty(defaultAccent) : profile.getBrandAccent().trim();

        /*
         * The BUILDER, not the positional constructor.
         *
         * This is the bug it just cost: two fields were added in the middle of the record and the
         * arguments here kept their old order, so logoMarkup received a URL and a PDF printed
         * "https://api…/assets/logo-full-tagline" as text where the logo belongs. Nothing failed to
         * compile, because they are all Strings — which is exactly why naming them matters.
         */
        return Snapshot.builder()
            .name(profile.displayName())
            .legalName(orEmpty(profile.getLegalName()))
            .tagline(orEmpty(profile.getTagline()))
            .tin(orEmpty(profile.getTin()))
            .vrn(orEmpty(profile.getVrn()))
            .registrationNumber(orEmpty(profile.getRegistrationNumber()))
            .licenceNumber(orEmpty(profile.getLicenceNumber()))
            .defaultCurrency(orEmpty(profile.getDefaultCurrency()))
            .email(email)
            .phone(phone)
            .phoneSecondary(phoneSecondary)
            .address(address)
            .website(website)
            .emails(allEmails)
            .phones(allPhones)
            .socials(socials)
            /* always the email slot: it answers with a raster even when the company uploaded vectors */
            .logoUrl(emailLogoUrl(profile))
            .logoLightUrl(assetUrl(profile, CompanyAsset.AssetKind.LOGO_LIGHT))
            .logoDarkUrl(assetUrl(profile, CompanyAsset.AssetKind.LOGO_DARK))
            .faviconUrl(assetUrl(profile, CompanyAsset.AssetKind.FAVICON_LIGHT))
            /* the tagline cut is a deliberate second choice: a header wants the plain lockup */
            .logoFullUrl(assetUrl(profile, CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_FULL_TAGLINE))
            .logoFullTaglineUrl(assetUrl(profile, CompanyAsset.AssetKind.LOGO_FULL_TAGLINE))
            /* embedded rather than fetched: a PDF is rendered here and read anywhere */
            .logoMarkup(assetMarkup(profile, CompanyAsset.AssetKind.LOGO_LIGHT, CompanyAsset.AssetKind.LOGO_FULL))
            .logoFullMarkup(assetMarkup(profile, CompanyAsset.AssetKind.LOGO_FULL,
                CompanyAsset.AssetKind.LOGO_FULL_TAGLINE, CompanyAsset.AssetKind.LOGO_LIGHT))
            .accent(brandAccent)
            /* black text on a pale accent, white on a dark one — a heading has to stay readable */
            .accentContrast(contrastFor(brandAccent))
            /* the darker shade a header band uses, and the tint a callout sits on */
            .accentDark(shade(brandAccent, 0.62f))
            .accentSoft(shade(brandAccent, 8.5f))
            .radius(orEmpty(profile.getBrandRadius()).isBlank() ? "8px" : radiusToPx(profile.getBrandRadius()))
            .font(orEmpty(profile.getBrandFont()))
            .bank(bank())
            .build();
    }

    /**
     * Roundness as the templates need it: pixels.
     *
     * The profile stores a percentage because that is how somebody thinks about roundness; CSS in an
     * email or a PDF needs a length. Same scale as the panel: 50% is 8px.
     */
    private String radiusToPx(String stored) {
        if (stored == null || stored.isBlank()) return "8px";
        String value = stored.trim();
        if (value.endsWith("px") || value.endsWith("rem")) return value;
        try {
            int percent = Integer.parseInt(value.replace("%", ""));
            return Math.round(Math.min(100, Math.max(0, percent)) / 100f * 16) + "px";
        } catch (NumberFormatException e) {
            return "8px";
        }
    }

    /**
     * The accent, darkened or tinted.
     *
     * A design has more than one brand colour in it — a header band, a rule, the wash behind a
     * callout — and asking a company to choose three is asking them to get two of them wrong. A
     * factor below 1 darkens; above 1 lightens towards white.
     */
    private String shade(String hex, float factor) {
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) return hex == null ? "" : hex;
        int[] rgb = {
            Integer.parseInt(hex.substring(1, 3), 16),
            Integer.parseInt(hex.substring(3, 5), 16),
            Integer.parseInt(hex.substring(5, 7), 16)
        };
        StringBuilder out = new StringBuilder("#");
        for (int channel : rgb) {
            int value = factor <= 1f
                ? Math.round(channel * factor)
                /* lighten towards white rather than overflowing the channel */
                : Math.round(channel + (255 - channel) * (1 - 1 / factor));
            out.append(String.format("%02x", Math.max(0, Math.min(255, value))));
        }
        return out.toString();
    }

    /**
     * Readable text on the accent.
     *
     * A dark green wants white on it and a pale sand wants black, and a template cannot work that out
     * for itself — so it is computed once here, by luminance.
     */
    private String contrastFor(String hex) {
        if (hex == null || !hex.matches("#[0-9a-fA-F]{6}")) return "#ffffff";
        int r = Integer.parseInt(hex.substring(1, 3), 16);
        int g = Integer.parseInt(hex.substring(3, 5), 16);
        int b = Integer.parseInt(hex.substring(5, 7), 16);
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
        return luminance > 0.6 ? "#16171a" : "#ffffff";
    }

    /** Primary first, then declared order — and only among the ones still in use. */
    private <T> Optional<T> pick(List<T> items,
                                 java.util.function.Function<T, Boolean> primary,
                                 java.util.function.Function<T, Boolean> active,
                                 java.util.function.Function<T, Integer> order) {
        return items.stream()
            .filter(i -> Boolean.TRUE.equals(active.apply(i)))
            .sorted(Comparator.comparing((T i) -> !Boolean.TRUE.equals(primary.apply(i)))
                .thenComparing(i -> order.apply(i) == null ? 0 : order.apply(i)))
            .findFirst();
    }

    /**
     * The logo as markup a document can draw without asking the network for anything.
     *
     * An uploaded SVG is inlined as-is, which is what the shipped templates always did (with one
     * company's art hardcoded); anything else becomes an <img> with a data URI. Width is left to the
     * template, because the space a logo sits in belongs to the design, not to the logo.
     *
     * Returns empty when nothing is uploaded — a template guards on it and leaves the space out
     * rather than drawing a broken image.
     */
    private String assetMarkup(CompanyProfile profile, CompanyAsset.AssetKind... kinds) {
        for (CompanyAsset.AssetKind kind : kinds) {
            CompanyAsset asset = profile.getAssets().stream()
                .filter(a -> a.getAssetKind() == kind && Boolean.TRUE.equals(a.getIsActive()))
                .findFirst().orElse(null);
            if (asset == null || asset.getFileName() == null) continue;

            try {
                java.nio.file.Path path = java.nio.file.Paths.get(assetStoragePath, asset.getFileName());
                if (!java.nio.file.Files.exists(path)) continue;
                byte[] bytes = java.nio.file.Files.readAllBytes(path);

                String mime = asset.getMimeType() == null ? "" : asset.getMimeType();
                if (mime.contains("svg")) {
                    /* inlined, so the vector stays a vector and prints sharp at any size */
                    String svg = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                    int start = svg.indexOf("<svg");
                    return start < 0 ? "" : svg.substring(start);
                }
                return "<img src=\"data:" + (mime.isBlank() ? "image/png" : mime) + ";base64,"
                    + java.util.Base64.getEncoder().encodeToString(bytes)
                    + "\" alt=\"\" style=\"max-width:100%;height:auto\" />";
            } catch (Exception e) {
                log.warn("Could not read the {} asset for embedding: {}", kind, e.getMessage());
            }
        }
        return "";
    }

    /** The first kind that exists, so mail falls back to the light logo when no raster is uploaded. */
    /**
     * The URL an email should point an {@code <img>} at.
     *
     * Always the email slot, whenever the company has uploaded ANY logo — because that endpoint
     * falls back through the other slots and converts a vector to PNG on the way out. Pointing at
     * logo-light instead, as this used to, sends a mail client after an SVG it will not render: a
     * broken-image box at the top of a welcome email, which is what a customer saw.
     */
    private String emailLogoUrl(CompanyProfile profile) {
        return assetUrl(profile, CompanyAsset.AssetKind.LOGO_EMAIL)
            .isEmpty()
            ? (hasAnyLogo(profile) ? assetPath("logo-email") : "")
            : assetUrl(profile, CompanyAsset.AssetKind.LOGO_EMAIL);
    }

    private boolean hasAnyLogo(CompanyProfile profile) {
        return profile.getAssets().stream()
            .anyMatch(a -> Boolean.TRUE.equals(a.getIsActive()) && a.getFileName() != null
                && a.getAssetKind() != CompanyAsset.AssetKind.FAVICON_LIGHT
                && a.getAssetKind() != CompanyAsset.AssetKind.FAVICON_DARK);
    }

    private String assetPath(String slug) {
        String root = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        return root + "/api/public/company/assets/" + slug;
    }

    private String assetUrl(CompanyProfile profile, CompanyAsset.AssetKind... kinds) {
        for (CompanyAsset.AssetKind kind : kinds) {
            boolean present = profile.getAssets().stream()
                .anyMatch(a -> a.getAssetKind() == kind && Boolean.TRUE.equals(a.getIsActive()));
            if (present) {
                String root = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
                return root + "/api/public/company/assets/" + kind.name().toLowerCase().replace('_', '-');
            }
        }
        return "";
    }

    /** The default active account, in the company's own currency where there is a choice. */
    private BankSnapshot bank() {
        try {
            List<BankAccount> active = bankAccountRepository.findByIsActiveOrderByAccountNameAsc(true);
            BankAccount chosen = active.stream().filter(a -> Boolean.TRUE.equals(a.getIsDefault())).findFirst()
                .orElse(active.isEmpty() ? null : active.get(0));
            if (chosen == null) return BankSnapshot.empty();
            return new BankSnapshot(
                orEmpty(chosen.getBankName()), orEmpty(chosen.getAccountName()),
                orEmpty(chosen.getAccountHolderName()), orEmpty(chosen.getAccountNumber()),
                orEmpty(chosen.getSwiftBicCode()), orEmpty(chosen.getIban()),
                orEmpty(chosen.getCurrency()));
        } catch (Exception e) {
            /* a document without bank details is worse than nothing, but not worth failing a send */
            log.warn("Could not resolve the default bank account for a document: {}", e.getMessage());
            return BankSnapshot.empty();
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /** What a document knows about the company. Immutable; rebuilt when the profile changes. */
    /*
     * A builder, because this record has grown three times and each time every positional caller —
     * including the tests — stopped compiling. A caller that names the two fields it cares about does
     * not break when a fourteenth is added.
     */
    @lombok.Builder
    public record Snapshot(
        String name, String legalName, String tagline,
        String tin, String vrn, String registrationNumber, String licenceNumber,
        String defaultCurrency,
        String email, String phone, String phoneSecondary, String address, String website,
        List<String> emails, List<String> phones,
        /** link type -> full url, for the social icons a signature or a footer carries */
        Map<String, String> socials,
        String logoUrl, String logoLightUrl, String logoDarkUrl, String faviconUrl,
        /** the brand: a template that hardcodes a colour is as wrong as one that hardcodes a name */
        String accent, String accentContrast, String accentDark, String accentSoft,
        String radius, String font,
        /**
         * The logo as MARKUP, for a document that must not depend on the network.
         *
         * A PDF is rendered on the server and a client may open it offline; fetching an https image
         * mid-render is slow at best and a blank box at worst. Measured against this renderer: inline
         * SVG draws, an SVG data URI draws, a raster data URI draws — so the asset is embedded, and
         * whichever form the company uploaded is the form used.
         */
        String logoMarkup, String logoFullMarkup,
        /** the whole lockup: a letterhead or a cover page, never a 28px topbar */
        String logoFullUrl, String logoFullTaglineUrl,
        BankSnapshot bank
    ) {
        /** What a document says before anybody has filled the profile in: the name, and blanks. */
        public static Snapshot empty(String fallbackName) {
            /*
             * The builder, not the positional constructor. This record has grown five times; every
             * time, this one method was the thing that stopped compiling.
             */
            return Snapshot.builder()
                .name(orEmptyStatic(fallbackName))
                .legalName("").tagline("").tin("").vrn("").registrationNumber("").licenceNumber("")
                .defaultCurrency("")
                .email("").phone("").phoneSecondary("").address("").website("")
                .emails(List.of()).phones(List.of()).socials(Map.of())
                .logoUrl("").logoLightUrl("").logoDarkUrl("").faviconUrl("")
                .accent("").accentContrast("").accentDark("").accentSoft("").radius("").font("")
                .logoFullUrl("").logoFullTaglineUrl("").logoMarkup("").logoFullMarkup("")
                .bank(BankSnapshot.empty())
                .build();
        }

        private static String orEmptyStatic(String v) { return v == null ? "" : v; }

        /** What goes in a {@code tel:} href: the leading + and the digits, nothing else. */
        private static String dialable(String number) {
            if (number == null) return "";
            String cleaned = number.replaceAll("[^0-9+]", "");
            return cleaned.startsWith("+") ? "+" + cleaned.substring(1).replace("+", "") : cleaned;
        }

        /**
         * The template variables. Names are the ones the templates will use, and every one is a
         * string — a template that prints "null" is a template nobody proofread.
         */
        public Map<String, String> variables() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("companyName", name);
            map.put("companyLegalName", legalName.isEmpty() ? name : legalName);
            map.put("companyTagline", tagline);
            map.put("companyEmail", email);
            map.put("companyPhone", phone);
            map.put("companyPhoneSecondary", phoneSecondary);
            /* tel: links need the digits only — "+255 746 598 330" is not a dialable href */
            map.put("companyPhoneTel", dialable(phone));
            map.put("companyPhoneSecondaryTel", dialable(phoneSecondary));
            map.put("companyAddress", address);
            map.put("companyWebsite", website);
            map.put("companyTin", tin);
            map.put("companyVrn", vrn);
            map.put("companyRegistrationNumber", registrationNumber);
            map.put("companyLicenceNumber", licenceNumber);
            map.put("companyCurrency", defaultCurrency);
            map.put("companyEmails", String.join(" · ", emails));
            map.put("companyPhones", String.join(" · ", phones));
            /*
             * One variable per platform, empty when the company has no such page. A template wraps
             * its icon in {{#companyFacebook}}…{{/companyFacebook}} so an absent page leaves no
             * dead link behind.
             */
            map.put("companyFacebook", socials.getOrDefault("FACEBOOK", ""));
            map.put("companyInstagram", socials.getOrDefault("INSTAGRAM", ""));
            map.put("companyX", socials.getOrDefault("X", ""));
            map.put("companyLinkedin", socials.getOrDefault("LINKEDIN", ""));
            map.put("companyYoutube", socials.getOrDefault("YOUTUBE", ""));
            map.put("companyTiktok", socials.getOrDefault("TIKTOK", ""));
            map.put("companyTripadvisor", socials.getOrDefault("TRIPADVISOR", ""));
            map.put("companyBookingUrl", socials.getOrDefault("BOOKING", ""));
            /* so a signature can drop the whole "Follow us" row rather than leave the label alone */
            map.put("companyHasSocials", socials.isEmpty() ? "" : "yes");
            map.put("companyLogoUrl", logoUrl);
            map.put("companyLogoDarkUrl", logoDarkUrl);
            map.put("companyFaviconUrl", faviconUrl);
            map.put("companyLogoLightUrl", logoLightUrl);
            map.put("companyLogoMarkup", logoMarkup);
            map.put("companyLogoFullMarkup", logoFullMarkup);
            map.put("companyLogoDarkUrl", logoDarkUrl);
            map.put("companyAccent", accent);
            map.put("companyAccentContrast", accentContrast);
            map.put("companyAccentDark", accentDark);
            map.put("companyAccentSoft", accentSoft);
            map.put("companyRadius", radius);
            map.put("companyFont", font);
            map.put("companyLogoFullUrl", logoFullUrl);
            map.put("companyLogoFullTaglineUrl", logoFullTaglineUrl);
            /* the same brand name older templates already ask for, so they keep working */
            map.put("brandName", name);
            map.put("bankName", bank.bankName());
            map.put("bankAccountName", bank.accountName());
            map.put("bankAccountHolder", bank.accountHolder());
            map.put("bankAccountNumber", bank.accountNumber());
            map.put("bankSwift", bank.swift());
            map.put("bankIban", bank.iban());
            map.put("bankCurrency", bank.currency());
            return map;
        }
    }

    public record BankSnapshot(String bankName, String accountName, String accountHolder,
                               String accountNumber, String swift, String iban, String currency) {
        public static BankSnapshot empty() {
            return new BankSnapshot("", "", "", "", "", "", "");
        }
    }
}
