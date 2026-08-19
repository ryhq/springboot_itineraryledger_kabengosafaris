package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * The company, as a PDF template sees it: {@code ${company.name}}, {@code ${company.address}},
 * {@code ${company.bank.accountNumber}}.
 *
 * A plain class with getters rather than the identity snapshot record, on purpose. SpEL resolves a
 * property through its getter; a record exposes {@code name()}, not {@code getName()}, and whether
 * that resolves depends on reflective field access working — which is not something a company's
 * invoice should depend on. Getters make it certain.
 */
@Getter
@Builder
@AllArgsConstructor
public class CompanyTemplateModel {

    private final String name;
    private final String legalName;
    private final String tagline;
    private final String email;
    private final String phone;
    /** the second active number, so a letterhead with two lines does not print one twice */
    private final String phoneSecondary;
    private final String address;
    private final String website;
    private final String tin;
    private final String vrn;
    private final String registrationNumber;
    private final String licenceNumber;
    private final String currency;

    /** every active address / number, for footers that print them all */
    private final List<String> emails;
    private final List<String> phones;

    /** link type -> url, e.g. {@code ${company.socials['INSTAGRAM']}} */
    private final java.util.Map<String, String> socials;

    private final String logoUrl;
    private final String logoLightUrl;
    private final String logoDarkUrl;
    private final String faviconUrl;
    /** the whole lockup — mark plus wordmark — for a letterhead or a cover page */
    private final String logoFullUrl;
    private final String logoFullTaglineUrl;

    /** the year, for a copyright line that has to be right every January */
    private final int year;

    private final Bank bank;

    /** True when there is a logo to print, so a template can leave the space out rather than draw a broken image. */
    public boolean hasLogo() {
        return logoLightUrl != null && !logoLightUrl.isBlank();
    }

    /** Same question for the wide lockup, which a letterhead prefers when it exists. */
    public boolean hasFullLogo() {
        return logoFullUrl != null && !logoFullUrl.isBlank();
    }

    /**
     * What a document should print at the top: the full lockup where one was uploaded, the icon
     * otherwise. A template asks for this rather than choosing, so every document agrees.
     */
    public String getDocumentLogoUrl() {
        return hasFullLogo() ? logoFullUrl : logoLightUrl;
    }

    /** The name to print on a legal document: the registered entity where there is one. */
    public String getFormalName() {
        return legalName == null || legalName.isBlank() ? name : legalName;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class Bank {
        private final String bankName;
        private final String accountName;
        private final String accountHolder;
        private final String accountNumber;
        private final String swift;
        private final String iban;
        private final String currency;

        public boolean isPresent() {
            return accountNumber != null && !accountNumber.isBlank();
        }
    }
}
