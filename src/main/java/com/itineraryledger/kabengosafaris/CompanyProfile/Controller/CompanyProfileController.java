package com.itineraryledger.kabengosafaris.CompanyProfile.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.*;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyAssetService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyProfileGetService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyProfileWriteService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Settings → Company.
 *
 * A singleton resource, so there is no list and no create: GET returns the one profile with its
 * completeness gaps, and every write returns that same payload back. The four contact collections
 * and the five asset slots are addressed as sub-paths.
 *
 * Permissions are matched by NAME against the catalogue seeded from permissions/entities.json — the
 * COMPANY_PROFILE entity is registered there, so PERM_READ_COMPANY_PROFILE and friends exist. A name
 * the catalogue does not know 403s everybody, superadmin included.
 */
@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyProfileController {

    private final CompanyProfileGetService getService;
    private final CompanyProfileWriteService writeService;
    private final CompanyAssetService assetService;

    // ------------------------------------------------------------------ profile

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> getCompany() {
        log.info("GET /api/company - Retrieving company profile");
        return getService.getCompany();
    }

    @GetMapping("/completeness")
    @PreAuthorize("hasAuthority('PERM_READ_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> getCompleteness() {
        log.info("GET /api/company/completeness - Retrieving company completeness");
        return getService.getCompleteness();
    }

    /**
     * What the company variables currently resolve to, for previewing a template.
     *
     * This was `isAuthenticated()` on the reasoning that nothing here is secret — it is the text the
     * templates already send out. That was wrong twice over. The payload carries the TIN and the
     * default bank account's number, IBAN and SWIFT, which are not "the text on a letterhead" in any
     * useful sense. And authentication is SELF-SERVICE here: anyone may create an account from the
     * sign-in screen and activate it from their own inbox, so `isAuthenticated()` means "anyone who
     * can receive email", not "somebody who works here".
     *
     * So: whoever edits a template may read it, because they genuinely need to see the name and the
     * colours their layout will print — and the money and tax numbers inside it are redacted unless
     * the caller may read the company profile outright.
     */
    @GetMapping("/variables")
    @PreAuthorize("hasAnyAuthority('PERM_READ_COMPANY_PROFILE', 'PERM_UPDATE_COMPANY_PROFILE',"
        + " 'PERM_READ_EMAIL_TEMPLATE', 'PERM_UPDATE_EMAIL_TEMPLATE',"
        + " 'PERM_READ_PDF_TEMPLATE', 'PERM_UPDATE_PDF_TEMPLATE',"
        + " 'PERM_READ_EMAIL_ACCOUNT_SIGNATURE', 'PERM_UPDATE_EMAIL_ACCOUNT_SIGNATURE')")
    public ResponseEntity<ApiResponse<?>> getVariables() {
        log.info("GET /api/company/variables - Retrieving resolved company variables");
        return getService.getVariables();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> updateCompany(@Valid @RequestBody UpdateCompanyProfileDTO dto) {
        log.info("PUT /api/company - Updating company identity");
        return writeService.updateProfile(dto);
    }

    // ------------------------------------------------------------------ emails

    @PostMapping("/emails")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> addEmail(@Valid @RequestBody CompanyEmailRequestDTO dto) {
        log.info("POST /api/company/emails - Adding company email");
        return writeService.addEmail(dto);
    }

    @PutMapping("/emails/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> updateEmail(@PathVariable String idObfuscated,
                                                      @Valid @RequestBody CompanyEmailRequestDTO dto) {
        log.info("PUT /api/company/emails/{} - Updating company email", idObfuscated);
        return writeService.updateEmail(idObfuscated, dto);
    }

    @DeleteMapping("/emails/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> deleteEmail(@PathVariable String idObfuscated) {
        log.info("DELETE /api/company/emails/{} - Removing company email", idObfuscated);
        return writeService.deleteEmail(idObfuscated);
    }

    // ------------------------------------------------------------------ phones

    @PostMapping("/phones")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> addPhone(@Valid @RequestBody CompanyPhoneRequestDTO dto) {
        log.info("POST /api/company/phones - Adding company phone");
        return writeService.addPhone(dto);
    }

    @PutMapping("/phones/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> updatePhone(@PathVariable String idObfuscated,
                                                      @Valid @RequestBody CompanyPhoneRequestDTO dto) {
        log.info("PUT /api/company/phones/{} - Updating company phone", idObfuscated);
        return writeService.updatePhone(idObfuscated, dto);
    }

    @DeleteMapping("/phones/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> deletePhone(@PathVariable String idObfuscated) {
        log.info("DELETE /api/company/phones/{} - Removing company phone", idObfuscated);
        return writeService.deletePhone(idObfuscated);
    }

    // ------------------------------------------------------------------ addresses

    @PostMapping("/addresses")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> addAddress(@Valid @RequestBody CompanyAddressRequestDTO dto) {
        log.info("POST /api/company/addresses - Adding company address");
        return writeService.addAddress(dto);
    }

    @PutMapping("/addresses/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> updateAddress(@PathVariable String idObfuscated,
                                                        @Valid @RequestBody CompanyAddressRequestDTO dto) {
        log.info("PUT /api/company/addresses/{} - Updating company address", idObfuscated);
        return writeService.updateAddress(idObfuscated, dto);
    }

    @DeleteMapping("/addresses/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> deleteAddress(@PathVariable String idObfuscated) {
        log.info("DELETE /api/company/addresses/{} - Removing company address", idObfuscated);
        return writeService.deleteAddress(idObfuscated);
    }

    // ------------------------------------------------------------------ links

    @PostMapping("/links")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> addLink(@Valid @RequestBody CompanyLinkRequestDTO dto) {
        log.info("POST /api/company/links - Adding company link");
        return writeService.addLink(dto);
    }

    @PutMapping("/links/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> updateLink(@PathVariable String idObfuscated,
                                                     @Valid @RequestBody CompanyLinkRequestDTO dto) {
        log.info("PUT /api/company/links/{} - Updating company link", idObfuscated);
        return writeService.updateLink(idObfuscated, dto);
    }

    @DeleteMapping("/links/{idObfuscated}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> deleteLink(@PathVariable String idObfuscated) {
        log.info("DELETE /api/company/links/{} - Removing company link", idObfuscated);
        return writeService.deleteLink(idObfuscated);
    }

    // ------------------------------------------------------------------ assets

    /**
     * One file per slot: {@code logo-light}, {@code logo-dark}, {@code favicon-light},
     * {@code favicon-dark}, {@code logo-email}. Uploading replaces what is there.
     */
    @PostMapping(value = "/assets/{kind}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> uploadAsset(@PathVariable String kind,
                                                      @RequestParam("file") MultipartFile file) {
        log.info("POST /api/company/assets/{} - Uploading company asset", kind);
        return assetService.upload(kind, file);
    }

    @DeleteMapping("/assets/{kind}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_COMPANY_PROFILE')")
    public ResponseEntity<ApiResponse<?>> deleteAsset(@PathVariable String kind) {
        log.info("DELETE /api/company/assets/{} - Removing company asset", kind);
        return assetService.remove(kind);
    }
}
