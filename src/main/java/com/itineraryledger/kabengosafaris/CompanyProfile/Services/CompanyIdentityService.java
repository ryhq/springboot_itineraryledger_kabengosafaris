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

        return new Snapshot(
            profile.displayName(),
            orEmpty(profile.getLegalName()),
            orEmpty(profile.getTagline()),
            orEmpty(profile.getTin()),
            orEmpty(profile.getVrn()),
            orEmpty(profile.getRegistrationNumber()),
            orEmpty(profile.getLicenceNumber()),
            orEmpty(profile.getDefaultCurrency()),
            email, phone, phoneSecondary, address, website,
            allEmails, allPhones, socials,
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_EMAIL, CompanyAsset.AssetKind.LOGO_LIGHT),
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_LIGHT),
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_DARK),
            assetUrl(profile, CompanyAsset.AssetKind.FAVICON_LIGHT),
            /* the tagline cut is a deliberate second choice: a header wants the plain lockup */
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_FULL, CompanyAsset.AssetKind.LOGO_FULL_TAGLINE),
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_FULL_TAGLINE),
            bank());
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

    /** The first kind that exists, so mail falls back to the light logo when no raster is uploaded. */
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
        /** the whole lockup: a letterhead or a cover page, never a 28px topbar */
        String logoFullUrl, String logoFullTaglineUrl,
        BankSnapshot bank
    ) {
        /** What a document says before anybody has filled the profile in: the name, and blanks. */
        public static Snapshot empty(String fallbackName) {
            return new Snapshot(orEmptyStatic(fallbackName), "", "", "", "", "", "", "",
                "", "", "", "", "", List.of(), List.of(), Map.of(), "", "", "", "", "", "",
                BankSnapshot.empty());
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
