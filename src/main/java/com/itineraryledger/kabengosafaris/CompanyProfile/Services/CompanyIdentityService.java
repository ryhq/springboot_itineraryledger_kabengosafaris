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
    public Map<String, String> variables() {
        return snapshot().variables();
    }

    /** The `${company.*}` object for the Thymeleaf PDF templates. */
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

        return new Snapshot(
            profile.displayName(),
            orEmpty(profile.getLegalName()),
            orEmpty(profile.getTagline()),
            orEmpty(profile.getTin()),
            orEmpty(profile.getVrn()),
            orEmpty(profile.getRegistrationNumber()),
            orEmpty(profile.getLicenceNumber()),
            orEmpty(profile.getDefaultCurrency()),
            email, phone, address, website,
            allEmails, allPhones,
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_EMAIL, CompanyAsset.AssetKind.LOGO_LIGHT),
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_LIGHT),
            assetUrl(profile, CompanyAsset.AssetKind.LOGO_DARK),
            assetUrl(profile, CompanyAsset.AssetKind.FAVICON_LIGHT),
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
                return root + "/api/public/company/assets/" + kind.name().toLowerCase();
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
    public record Snapshot(
        String name, String legalName, String tagline,
        String tin, String vrn, String registrationNumber, String licenceNumber,
        String defaultCurrency,
        String email, String phone, String address, String website,
        List<String> emails, List<String> phones,
        String logoUrl, String logoLightUrl, String logoDarkUrl, String faviconUrl,
        BankSnapshot bank
    ) {
        /** What a document says before anybody has filled the profile in: the name, and blanks. */
        public static Snapshot empty(String fallbackName) {
            return new Snapshot(orEmptyStatic(fallbackName), "", "", "", "", "", "", "",
                "", "", "", "", List.of(), List.of(), "", "", "", "", BankSnapshot.empty());
        }

        private static String orEmptyStatic(String v) { return v == null ? "" : v; }

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
            map.put("companyAddress", address);
            map.put("companyWebsite", website);
            map.put("companyTin", tin);
            map.put("companyVrn", vrn);
            map.put("companyRegistrationNumber", registrationNumber);
            map.put("companyLicenceNumber", licenceNumber);
            map.put("companyCurrency", defaultCurrency);
            map.put("companyEmails", String.join(" · ", emails));
            map.put("companyPhones", String.join(" · ", phones));
            map.put("companyLogoUrl", logoUrl);
            map.put("companyLogoDarkUrl", logoDarkUrl);
            map.put("companyFaviconUrl", faviconUrl);
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
