package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The company's variables, declared once for every renderer in the product.
 *
 * They were always substituted — an email footer, a PDF letterhead and a signature all resolve them —
 * but nothing DECLARED them, so the "variables you can use" panel showed only the record's own fields.
 * A variable nobody can discover may as well not exist: template authors typed the company's details
 * by hand instead, which is the drift this whole exercise was meant to end.
 *
 * One list, three consumers:
 *  · email events and signatures merge it into their declared variables as {{name}}
 *  · PDF documents merge it as ${company.path}, because Thymeleaf reads a path into a model
 *  · the renderers substitute it whether the template declares it or not
 *
 * Adding one here makes it visible in all three panels and usable in every template, with no
 * per-event or per-document editing.
 */
public final class CompanyVariableCatalogue {

    private CompanyVariableCatalogue() {}

    /** flat name → what it is, in the order somebody filling a letterhead would want them */
    private static final Map<String, String> VARIABLES = new LinkedHashMap<>();
    static {
        VARIABLES.put("companyName", "Trading name — what customers call the company");
        VARIABLES.put("companyLegalName", "Registered entity. Falls back to the trading name");
        VARIABLES.put("companyTagline", "One line under the logo, where there is one");

        VARIABLES.put("companyEmail", "Primary email address");
        VARIABLES.put("companyEmails", "Every active email address, separated by ·");
        VARIABLES.put("companyPhone", "Primary phone number, formatted");
        VARIABLES.put("companyPhoneSecondary", "Second number, for a letterhead with two");
        VARIABLES.put("companyPhoneTel", "Primary number as digits, for a tel: link");
        VARIABLES.put("companyPhoneSecondaryTel", "Second number as digits, for a tel: link");
        VARIABLES.put("companyPhones", "Every active number, separated by ·");
        VARIABLES.put("companyAddress", "Primary address on one line");
        VARIABLES.put("companyWebsite", "Website without the scheme, as a footer prints it");

        VARIABLES.put("companyTin", "Taxpayer identification number — invoices must carry it");
        VARIABLES.put("companyVrn", "VAT registration number, where the company has one");
        VARIABLES.put("companyRegistrationNumber", "Company registration / incorporation number");
        VARIABLES.put("companyLicenceNumber", "Tour operator licence, where one is printed");
        VARIABLES.put("companyCurrency", "Default currency code");

        VARIABLES.put("companyFacebook", "Facebook URL, empty when there is none");
        VARIABLES.put("companyInstagram", "Instagram URL, empty when there is none");
        VARIABLES.put("companyX", "X / Twitter URL, empty when there is none");
        VARIABLES.put("companyLinkedin", "LinkedIn URL, empty when there is none");
        VARIABLES.put("companyYoutube", "YouTube URL, empty when there is none");
        VARIABLES.put("companyTiktok", "TikTok URL, empty when there is none");
        VARIABLES.put("companyTripadvisor", "Tripadvisor URL, empty when there is none");
        VARIABLES.put("companyBookingUrl", "Booking page URL, empty when there is none");
        VARIABLES.put("companyHasSocials", "Non-empty when the company has any social page — wrap the whole row in {{#companyHasSocials}}…{{/companyHasSocials}}");

        /*
         * The brand. A template that hardcodes a green is a template that looks wrong for the second
         * company, exactly like a hardcoded name — so the accent, the corner radius and the font are
         * variables too, and the shipped templates use them.
         */
        VARIABLES.put("companyAccent", "The brand accent colour, e.g. #1c7a58. In a PDF use {{companyAccent}} even inside CSS — a ${} expression is escaped there");
        VARIABLES.put("companyAccentBare", "The accent with no '#', for a colour inside a data: URI — write it as %23{{companyAccentBare}}");
        VARIABLES.put("companyAccentContrast", "Text colour that stays legible on the accent");
        VARIABLES.put("companyAccentDark", "A darker shade of the accent — header bands, footers, rules");
        VARIABLES.put("companyAccentSoft", "A pale tint of the accent — the wash behind a callout");
        VARIABLES.put("companyRadius", "Corner radius in pixels, e.g. 8px");
        VARIABLES.put("companyFont", "The brand font stack");

        /*
         * Assets. Every installation serves them at the SAME path — /api/public/company/assets/<kind> —
         * so a template refers to the variable and never to a host, and the same template renders for
         * every company without editing.
         */
        VARIABLES.put("companyLogoUrl", "Logo for a message BODY — a raster, on a white background. Upload a vector and it is converted for you");
        VARIABLES.put("companyLogoEmailDarkUrl", "Logo for a message's coloured HEADER band — the light-ink mark, since the band is dark");
        VARIABLES.put("companyLogoLightUrl", "Icon logo for light backgrounds");
        VARIABLES.put("companyLogoDarkUrl", "Icon logo for dark backgrounds");
        VARIABLES.put("companyLogoFullUrl", "Full lockup — mark plus wordmark — for a letterhead");
        VARIABLES.put("companyLogoFullTaglineUrl", "Full lockup carrying the tagline, for a cover page");
        VARIABLES.put("companyFaviconUrl", "Favicon, for anywhere a small square mark is wanted");
        VARIABLES.put("companyLogoMarkup", "The icon logo as drawable markup, embedded — use this in a PDF, where fetching a URL mid-render is a blank box at worst. It FILLS its container, so set the size on the wrapper: <span style=\"display:inline-block;width:120px\">");
        VARIABLES.put("companyLogoFullMarkup", "The full lockup as drawable markup, embedded — for a letterhead or a cover page. It FILLS its container, so the wrapper's width is the size: <span style=\"display:inline-block;width:180px\">");

        VARIABLES.put("bankName", "Default bank account: the bank");
        VARIABLES.put("bankAccountName", "Default bank account: the account name");
        VARIABLES.put("bankAccountHolder", "Default bank account: the holder");
        VARIABLES.put("bankAccountNumber", "Default bank account: the number");
        VARIABLES.put("bankSwift", "Default bank account: SWIFT / BIC");
        VARIABLES.put("bankIban", "Default bank account: IBAN");
        VARIABLES.put("bankCurrency", "Default bank account: currency");

        VARIABLES.put("currentYear", "This year, for a copyright line that is right every January");
    }

    public static Map<String, String> variables() {
        return VARIABLES;
    }

    /**
     * The catalogue as an email event or signature declares its variables: `name`, `description`, and
     * never required — a template that leaves the company out is unusual, not broken.
     */
    public static List<Map<String, Object>> asEmailVariables() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : VARIABLES.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", entry.getKey());
            row.put("description", entry.getValue());
            row.put("isRequired", false);
            row.put("group", "Company");
            out.add(row);
        }
        return out;
    }

    /**
     * The same catalogue as a PDF document declares its variables: a `path` into the model, since a
     * Thymeleaf expression reads a path rather than a flat placeholder.
     *
     * `companyName` becomes `company.name`, `bankSwift` becomes `company.bank.swift` — the shapes the
     * PDF model actually exposes.
     */
    public static List<Map<String, Object>> asPdfVariables() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, String> entry : VARIABLES.entrySet()) {
            String path = pdfPathFor(entry.getKey());
            if (path == null) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", path);
            row.put("description", entry.getValue());
            row.put("type", "string");
            row.put("isRequired", false);
            row.put("group", "Company");
            out.add(row);
        }
        /* two the model offers that a flat email placeholder cannot */
        out.add(pdfRow("company.year", "This year, as a number", "number"));
        out.add(pdfRow("company.hasLogo()", "True when a logo exists — guard an <img> with it", "boolean"));
        out.add(pdfRow("company.documentLogoUrl",
            "What this document should print at the top: the full lockup where one exists, the icon otherwise", "string"));
        out.add(pdfRow("company.formalName", "The registered entity where there is one, else the trading name", "string"));
        return out;
    }

    private static Map<String, Object> pdfRow(String path, String description, String type) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("path", path);
        row.put("description", description);
        row.put("type", type);
        row.put("isRequired", false);
        row.put("group", "Company");
        return row;
    }

    /** The Thymeleaf path for a flat name, or null where the model has no equivalent. */
    public static String pdfPath(String name) {
        return pdfPathFor(name);
    }

    /** The Thymeleaf path for a flat name, or null where the model has no equivalent. */
    private static String pdfPathFor(String name) {
        return switch (name) {
            case "companyName" -> "company.name";
            case "companyLegalName" -> "company.legalName";
            case "companyTagline" -> "company.tagline";
            case "companyEmail" -> "company.email";
            case "companyEmails" -> "company.emails";
            case "companyPhone" -> "company.phone";
            case "companyPhoneSecondary" -> "company.phoneSecondary";
            case "companyPhones" -> "company.phones";
            case "companyAddress" -> "company.address";
            case "companyWebsite" -> "company.website";
            case "companyTin" -> "company.tin";
            case "companyVrn" -> "company.vrn";
            case "companyRegistrationNumber" -> "company.registrationNumber";
            case "companyLicenceNumber" -> "company.licenceNumber";
            case "companyCurrency" -> "company.currency";
            case "companyAccent" -> "company.accent";
            case "companyAccentContrast" -> "company.accentContrast";
            case "companyAccentDark" -> "company.accentDark";
            case "companyAccentSoft" -> "company.accentSoft";
            case "companyRadius" -> "company.radius";
            case "companyFont" -> "company.font";
            case "companyLogoUrl" -> "company.logoUrl";
            case "companyLogoEmailDarkUrl" -> "company.logoEmailDarkUrl";
            case "companyLogoLightUrl" -> "company.logoLightUrl";
            case "companyLogoDarkUrl" -> "company.logoDarkUrl";
            case "companyLogoFullUrl" -> "company.logoFullUrl";
            case "companyLogoFullTaglineUrl" -> "company.logoFullTaglineUrl";
            case "companyFaviconUrl" -> "company.faviconUrl";
            case "companyLogoMarkup" -> "company.logoMarkup";
            case "companyLogoFullMarkup" -> "company.logoFullMarkup";
            case "bankName" -> "company.bank.bankName";
            case "bankAccountName" -> "company.bank.accountName";
            case "bankAccountHolder" -> "company.bank.accountHolder";
            case "bankAccountNumber" -> "company.bank.accountNumber";
            case "bankSwift" -> "company.bank.swift";
            case "bankIban" -> "company.bank.iban";
            case "bankCurrency" -> "company.bank.currency";
            /* social links and the mustache-only helpers have no path form */
            default -> null;
        };
    }
}
