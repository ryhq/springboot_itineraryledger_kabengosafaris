package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The whole company profile as the Settings page needs it: the identity scalars, the four
 * contact collections in display order, and the asset slots.
 *
 * There is exactly one of these rows, so there is no list endpoint and no obfuscated id for the
 * profile itself — only its children are addressable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfileDTO {

    private String tradingName;
    private String legalName;
    private String tagline;
    private String tin;
    private String vrn;
    private String registrationNumber;
    private String licenceNumber;
    private String defaultCurrency;
    private String timezone;
    private String locale;

    private String brandMark;
    private String brandAccent;
    private String brandRadius;
    private String brandFont;

    private List<CompanyEmailDTO> emails;
    private List<CompanyPhoneDTO> phones;
    private List<CompanyAddressDTO> addresses;
    private List<CompanyLinkDTO> links;
    private List<CompanyAssetDTO> assets;

    /** What every document would print today — the resolved primaries, not the collections. */
    private ResolvedDTO resolved;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ResolvedDTO {
        private String name;
        private String legalName;
        private String email;
        private String phone;
        private String address;
        private String website;
        private String logoUrl;
        private String logoDarkUrl;
        private String faviconUrl;
    }
}
