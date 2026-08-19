package com.itineraryledger.kabengosafaris.CompanyProfile.DTOs;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * The identity scalars.
 *
 * Patch semantics, the same as everywhere else in this API: a field left out (null) is not touched,
 * and a field sent as "" is CLEARED. Both matter here — the page saves one field at a time from a
 * click-to-edit grid, and a VRN that was entered by mistake has to be removable.
 */
@Data
public class UpdateCompanyProfileDTO {

    @Size(max = 200, message = "Trading name cannot exceed 200 characters")
    private String tradingName;

    @Size(max = 200, message = "Legal name cannot exceed 200 characters")
    private String legalName;

    @Size(max = 300, message = "Tagline cannot exceed 300 characters")
    private String tagline;

    @Size(max = 50, message = "TIN cannot exceed 50 characters")
    private String tin;

    @Size(max = 50, message = "VRN cannot exceed 50 characters")
    private String vrn;

    @Size(max = 100, message = "Registration number cannot exceed 100 characters")
    private String registrationNumber;

    @Size(max = 100, message = "Licence number cannot exceed 100 characters")
    private String licenceNumber;

    @Size(max = 3, message = "Currency must be a 3-letter code")
    private String defaultCurrency;

    @Size(max = 64, message = "Timezone cannot exceed 64 characters")
    private String timezone;

    @Size(max = 16, message = "Locale cannot exceed 16 characters")
    private String locale;

    @Size(max = 8, message = "The mark can be at most 8 characters — one or two is usual")
    private String brandMark;

    @jakarta.validation.constraints.Pattern(
        regexp = "^$|^#[0-9a-fA-F]{6}$",
        message = "The accent must be a 6-digit hex colour, e.g. #1c7a58")
    private String brandAccent;

    /*
     * Roundness as a percentage, 0 (square) to 100 (as round as this design goes).
     *
     * NOT a CSS percentage: border-radius:50% turns a button into a lens, because a CSS percentage is
     * measured against the element's own width and height. The client maps this to a pixel radius.
     * px/rem values are still accepted so anything stored before this keeps working.
     */
    @jakarta.validation.constraints.Pattern(
        regexp = "^$|^(100|[1-9]?[0-9])%$|^\\d{1,2}(px|rem)$",
        message = "Roundness must be a percentage from 0% to 100%")
    private String brandRadius;

    @Size(max = 160, message = "The font stack cannot exceed 160 characters")
    private String brandFont;
}
