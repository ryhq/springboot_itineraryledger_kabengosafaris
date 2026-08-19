package com.itineraryledger.kabengosafaris.CompanyProfile.Services;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
 * Writes the company profile and its four contact collections.
 *
 * Two rules run through everything here:
 *
 *  1. Every write ends with {@link CompanyIdentityService#invalidate()}. The identity is cached
 *     because every PDF and every email asks for it; a cache nobody invalidates means the new
 *     phone number appears on the website and not on the invoice.
 *  2. Every write returns the SAME payload as GET — the profile plus the completeness gaps — so the
 *     Settings page never has to re-fetch to find out that removing the last active email just
 *     broke the invoice footer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyProfileWriteService {

    private final CompanyProfileRepository profileRepository;
    private final CompanyProfileGetService getService;
    private final CompanyIdentityService identityService;
    private final IdObfuscator idObfuscator;

    @Value("${app.company.name:}")
    private String fallbackCompanyName;

    // ------------------------------------------------------------------ identity scalars

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateProfile(UpdateCompanyProfileDTO dto) {
        CompanyProfile profile = requireProfile();

        /*
         * Patch semantics. A null field is absent from the request and must not be touched; a blank
         * field was deliberately emptied and must be cleared. Getting this wrong is why clearing a
         * field used to be impossible in this API.
         */
        patch(dto.getTradingName(), v -> {
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("Trading name cannot be empty — every document prints it");
            }
            profile.setTradingName(v);
        });
        patch(dto.getLegalName(), profile::setLegalName);
        patch(dto.getTagline(), profile::setTagline);
        patch(dto.getTin(), profile::setTin);
        patch(dto.getVrn(), profile::setVrn);
        patch(dto.getRegistrationNumber(), profile::setRegistrationNumber);
        patch(dto.getLicenceNumber(), profile::setLicenceNumber);
        patch(dto.getDefaultCurrency(), v -> profile.setDefaultCurrency(v == null ? null : v.toUpperCase()));
        patch(dto.getTimezone(), profile::setTimezone);
        patch(dto.getLocale(), profile::setLocale);

        profileRepository.save(profile);
        identityService.invalidate();
        log.info("Company profile updated: {}", profile.displayName());

        return respond("Company profile updated successfully", profile);
    }

    // ------------------------------------------------------------------ emails

    @Transactional
    public ResponseEntity<ApiResponse<?>> addEmail(CompanyEmailRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return badRequest("An email address is required");
        }

        CompanyEmail entity = CompanyEmail.builder()
            .companyProfile(profile)
            .email(dto.getEmail().trim())
            .emailType(parseEnum(CompanyEmail.EmailType.class, dto.getEmailType(), CompanyEmail.EmailType.GENERAL))
            .label(blankToNull(dto.getLabel()))
            .isPrimary(Boolean.TRUE.equals(dto.getIsPrimary()) || profile.getEmails().isEmpty())
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .displayOrder(dto.getDisplayOrder() == null ? profile.getEmails().size() : dto.getDisplayOrder())
            .build();

        profile.getEmails().add(entity);
        if (Boolean.TRUE.equals(entity.getIsPrimary())) {
            profile.getEmails().forEach(e -> { if (e != entity) e.setIsPrimary(false); });
        }

        return saved("Email added successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateEmail(String idObfuscated, CompanyEmailRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        CompanyEmail entity = find(profile.getEmails(), idObfuscated, CompanyEmail::getId);
        if (entity == null) return notFound("email");

        patch(dto.getEmail(), v -> {
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("An email address cannot be blank — delete the row instead");
            }
            entity.setEmail(v.trim());
        });
        if (dto.getEmailType() != null) entity.setEmailType(parseEnum(CompanyEmail.EmailType.class, dto.getEmailType(), entity.getEmailType()));
        patch(dto.getLabel(), entity::setLabel);
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsPrimary() != null) {
            applyPrimary(profile.getEmails(), entity, dto.getIsPrimary(),
                CompanyEmail::setIsPrimary, CompanyEmail::getIsPrimary, CompanyEmail::getIsActive);
        }

        return saved("Email updated successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteEmail(String idObfuscated) {
        CompanyProfile profile = requireProfile();
        CompanyEmail entity = find(profile.getEmails(), idObfuscated, CompanyEmail::getId);
        if (entity == null) return notFound("email");

        boolean wasPrimary = Boolean.TRUE.equals(entity.getIsPrimary());
        profile.getEmails().remove(entity);
        if (wasPrimary) promoteFirstActive(profile.getEmails(), CompanyEmail::setIsPrimary, CompanyEmail::getIsActive);

        return saved("Email removed successfully", profile);
    }

    // ------------------------------------------------------------------ phones

    @Transactional
    public ResponseEntity<ApiResponse<?>> addPhone(CompanyPhoneRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        if (dto.getPhoneNumber() == null || dto.getPhoneNumber().isBlank()) {
            return badRequest("A phone number is required");
        }

        CompanyPhone entity = CompanyPhone.builder()
            .companyProfile(profile)
            .countryCode(blankToNull(dto.getCountryCode()))
            .phoneNumber(dto.getPhoneNumber().trim())
            .phoneType(parseEnum(CompanyPhone.PhoneType.class, dto.getPhoneType(), CompanyPhone.PhoneType.MOBILE))
            .label(blankToNull(dto.getLabel()))
            .isWhatsApp(Boolean.TRUE.equals(dto.getIsWhatsApp()))
            .operatingHours(blankToNull(dto.getOperatingHours()))
            .isPrimary(Boolean.TRUE.equals(dto.getIsPrimary()) || profile.getPhones().isEmpty())
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .displayOrder(dto.getDisplayOrder() == null ? profile.getPhones().size() : dto.getDisplayOrder())
            .build();

        profile.getPhones().add(entity);
        if (Boolean.TRUE.equals(entity.getIsPrimary())) {
            profile.getPhones().forEach(p -> { if (p != entity) p.setIsPrimary(false); });
        }

        return saved("Phone added successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updatePhone(String idObfuscated, CompanyPhoneRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        CompanyPhone entity = find(profile.getPhones(), idObfuscated, CompanyPhone::getId);
        if (entity == null) return notFound("phone");

        patch(dto.getCountryCode(), entity::setCountryCode);
        patch(dto.getPhoneNumber(), v -> {
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("A phone number cannot be blank — delete the row instead");
            }
            entity.setPhoneNumber(v.trim());
        });
        if (dto.getPhoneType() != null) entity.setPhoneType(parseEnum(CompanyPhone.PhoneType.class, dto.getPhoneType(), entity.getPhoneType()));
        patch(dto.getLabel(), entity::setLabel);
        patch(dto.getOperatingHours(), entity::setOperatingHours);
        if (dto.getIsWhatsApp() != null) entity.setIsWhatsApp(dto.getIsWhatsApp());
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsPrimary() != null) {
            applyPrimary(profile.getPhones(), entity, dto.getIsPrimary(),
                CompanyPhone::setIsPrimary, CompanyPhone::getIsPrimary, CompanyPhone::getIsActive);
        }

        return saved("Phone updated successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deletePhone(String idObfuscated) {
        CompanyProfile profile = requireProfile();
        CompanyPhone entity = find(profile.getPhones(), idObfuscated, CompanyPhone::getId);
        if (entity == null) return notFound("phone");

        boolean wasPrimary = Boolean.TRUE.equals(entity.getIsPrimary());
        profile.getPhones().remove(entity);
        if (wasPrimary) promoteFirstActive(profile.getPhones(), CompanyPhone::setIsPrimary, CompanyPhone::getIsActive);

        return saved("Phone removed successfully", profile);
    }

    // ------------------------------------------------------------------ addresses

    @Transactional
    public ResponseEntity<ApiResponse<?>> addAddress(CompanyAddressRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        if (isBlank(dto.getLineOne()) && isBlank(dto.getCity()) && isBlank(dto.getPostalCode())) {
            return badRequest("An address needs at least a street line, a city or a postal code");
        }

        CompanyAddress entity = CompanyAddress.builder()
            .companyProfile(profile)
            .addressType(parseEnum(CompanyAddress.AddressType.class, dto.getAddressType(), CompanyAddress.AddressType.OFFICE))
            .label(blankToNull(dto.getLabel()))
            .lineOne(blankToNull(dto.getLineOne()))
            .lineTwo(blankToNull(dto.getLineTwo()))
            .city(blankToNull(dto.getCity()))
            .region(blankToNull(dto.getRegion()))
            .postalCode(blankToNull(dto.getPostalCode()))
            .country(blankToNull(dto.getCountry()))
            .isPrimary(Boolean.TRUE.equals(dto.getIsPrimary()) || profile.getAddresses().isEmpty())
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .displayOrder(dto.getDisplayOrder() == null ? profile.getAddresses().size() : dto.getDisplayOrder())
            .build();

        profile.getAddresses().add(entity);
        if (Boolean.TRUE.equals(entity.getIsPrimary())) {
            profile.getAddresses().forEach(a -> { if (a != entity) a.setIsPrimary(false); });
        }

        return saved("Address added successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateAddress(String idObfuscated, CompanyAddressRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        CompanyAddress entity = find(profile.getAddresses(), idObfuscated, CompanyAddress::getId);
        if (entity == null) return notFound("address");

        if (dto.getAddressType() != null) entity.setAddressType(parseEnum(CompanyAddress.AddressType.class, dto.getAddressType(), entity.getAddressType()));
        patch(dto.getLabel(), entity::setLabel);
        patch(dto.getLineOne(), entity::setLineOne);
        patch(dto.getLineTwo(), entity::setLineTwo);
        patch(dto.getCity(), entity::setCity);
        patch(dto.getRegion(), entity::setRegion);
        patch(dto.getPostalCode(), entity::setPostalCode);
        patch(dto.getCountry(), entity::setCountry);
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsPrimary() != null) {
            applyPrimary(profile.getAddresses(), entity, dto.getIsPrimary(),
                CompanyAddress::setIsPrimary, CompanyAddress::getIsPrimary, CompanyAddress::getIsActive);
        }

        if (entity.formatted().isBlank()) {
            return badRequest("That would leave the address empty — delete the row instead");
        }

        return saved("Address updated successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteAddress(String idObfuscated) {
        CompanyProfile profile = requireProfile();
        CompanyAddress entity = find(profile.getAddresses(), idObfuscated, CompanyAddress::getId);
        if (entity == null) return notFound("address");

        boolean wasPrimary = Boolean.TRUE.equals(entity.getIsPrimary());
        profile.getAddresses().remove(entity);
        if (wasPrimary) promoteFirstActive(profile.getAddresses(), CompanyAddress::setIsPrimary, CompanyAddress::getIsActive);

        return saved("Address removed successfully", profile);
    }

    // ------------------------------------------------------------------ links

    @Transactional
    public ResponseEntity<ApiResponse<?>> addLink(CompanyLinkRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        if (dto.getUrl() == null || dto.getUrl().isBlank()) {
            return badRequest("A URL is required");
        }

        String url = normaliseUrl(dto.getUrl());
        CompanyLink.LinkType type = parseEnum(CompanyLink.LinkType.class, dto.getLinkType(), CompanyLink.LinkType.WEBSITE);

        CompanyLink entity = CompanyLink.builder()
            .companyProfile(profile)
            .url(url)
            .linkType(type)
            .label(blankToNull(dto.getLabel()))
            /* the first WEBSITE link is what ${company.website} prints, so it becomes primary */
            .isPrimary(Boolean.TRUE.equals(dto.getIsPrimary())
                || profile.getLinks().stream().noneMatch(l -> l.getLinkType() == type))
            .isActive(dto.getIsActive() == null || dto.getIsActive())
            .displayOrder(dto.getDisplayOrder() == null ? profile.getLinks().size() : dto.getDisplayOrder())
            .build();

        profile.getLinks().add(entity);
        if (Boolean.TRUE.equals(entity.getIsPrimary())) {
            /* primary is per link type: one primary website AND one primary Instagram is correct */
            profile.getLinks().forEach(l -> { if (l != entity && l.getLinkType() == type) l.setIsPrimary(false); });
        }

        return saved("Link added successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateLink(String idObfuscated, CompanyLinkRequestDTO dto) {
        CompanyProfile profile = requireProfile();
        CompanyLink entity = find(profile.getLinks(), idObfuscated, CompanyLink::getId);
        if (entity == null) return notFound("link");

        patch(dto.getUrl(), v -> {
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("A URL cannot be blank — delete the row instead");
            }
            entity.setUrl(normaliseUrl(v));
        });
        if (dto.getLinkType() != null) entity.setLinkType(parseEnum(CompanyLink.LinkType.class, dto.getLinkType(), entity.getLinkType()));
        patch(dto.getLabel(), entity::setLabel);
        if (dto.getIsActive() != null) entity.setIsActive(dto.getIsActive());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        if (dto.getIsPrimary() != null) {
            List<CompanyLink> sameType = profile.getLinks().stream()
                .filter(l -> l.getLinkType() == entity.getLinkType()).toList();
            applyPrimary(sameType, entity, dto.getIsPrimary(),
                CompanyLink::setIsPrimary, CompanyLink::getIsPrimary, CompanyLink::getIsActive);
        }

        return saved("Link updated successfully", profile);
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteLink(String idObfuscated) {
        CompanyProfile profile = requireProfile();
        CompanyLink entity = find(profile.getLinks(), idObfuscated, CompanyLink::getId);
        if (entity == null) return notFound("link");

        CompanyLink.LinkType type = entity.getLinkType();
        boolean wasPrimary = Boolean.TRUE.equals(entity.getIsPrimary());
        profile.getLinks().remove(entity);
        if (wasPrimary) {
            promoteFirstActive(
                profile.getLinks().stream().filter(l -> l.getLinkType() == type).toList(),
                CompanyLink::setIsPrimary, CompanyLink::getIsActive);
        }

        return saved("Link removed successfully", profile);
    }

    // ------------------------------------------------------------------ shared

    /**
     * The profile row, created on demand.
     *
     * The initializer seeds it at boot, but a company that cleared its database, or an install where
     * the initializer was skipped, must still be able to save from the Settings page.
     */
    @Transactional
    public CompanyProfile requireProfile() {
        return profileRepository.findSingleton().orElseGet(() -> {
            CompanyProfile created = CompanyProfile.builder()
                .tradingName(fallbackCompanyName == null || fallbackCompanyName.isBlank()
                    ? "Your company" : fallbackCompanyName)
                .build();
            log.info("No company profile existed — creating one named '{}'", created.getTradingName());
            return profileRepository.save(created);
        });
    }

    private ResponseEntity<ApiResponse<?>> saved(String message, CompanyProfile profile) {
        profileRepository.save(profile);
        identityService.invalidate();
        return respond(message, profile);
    }

    /** GET's payload, from every mutation, so the page sees the new gap list immediately. */
    private ResponseEntity<ApiResponse<?>> respond(String message, CompanyProfile profile) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("company", getService.toDTO(profile));
        payload.put("completeness", getService.completeness(profile));
        return ResponseEntity.ok(ApiResponse.success(200, message, payload));
    }

    private void patch(String value, Consumer<String> setter) {
        if (value == null) return;              // absent — leave it alone
        setter.accept(value.isBlank() ? null : value.trim());   // "" — clear it
    }

    private <T> T find(List<T> items, String idObfuscated, java.util.function.Function<T, Long> id) {
        Long decoded = idObfuscator.decodeId(idObfuscated);
        if (decoded == null) return null;
        return items.stream().filter(i -> decoded.equals(id.apply(i))).findFirst().orElse(null);
    }

    /**
     * Exactly one primary among the siblings.
     *
     * Turning a primary OFF hands the flag to the next active sibling rather than leaving none:
     * "primary" is what documents print, and no primary means a blank line.
     */
    private <T> void applyPrimary(List<T> siblings, T target, boolean primary,
                                  java.util.function.BiConsumer<T, Boolean> setPrimary,
                                  java.util.function.Function<T, Boolean> getPrimary,
                                  java.util.function.Function<T, Boolean> getActive) {
        if (primary) {
            siblings.forEach(s -> setPrimary.accept(s, s == target));
            return;
        }
        if (!Boolean.TRUE.equals(getPrimary.apply(target))) return;

        setPrimary.accept(target, false);
        siblings.stream()
            .filter(s -> s != target && Boolean.TRUE.equals(getActive.apply(s)))
            .findFirst()
            .ifPresent(s -> setPrimary.accept(s, true));
    }

    private <T> void promoteFirstActive(List<T> siblings,
                                        java.util.function.BiConsumer<T, Boolean> setPrimary,
                                        java.util.function.Function<T, Boolean> getActive) {
        siblings.stream()
            .filter(s -> Boolean.TRUE.equals(getActive.apply(s)))
            .findFirst()
            .ifPresent(s -> setPrimary.accept(s, true));
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, E fallback) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase().replace('-', '_').replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("'" + value + "' is not one of: "
                + Arrays.toString(type.getEnumConstants()));
        }
    }

    /** A url typed without a scheme still has to be clickable in a PDF. */
    private String normaliseUrl(String url) {
        String trimmed = url.trim();
        if (trimmed.matches("(?i)^https?://.*")) return trimmed;
        if (trimmed.startsWith("mailto:") || trimmed.startsWith("tel:")) return trimmed;
        return "https://" + trimmed;
    }

    private boolean isBlank(String v) { return v == null || v.isBlank(); }

    private String blankToNull(String v) { return v == null || v.isBlank() ? null : v.trim(); }

    private ResponseEntity<ApiResponse<?>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, message, "VALIDATION_ERROR"));
    }

    private ResponseEntity<ApiResponse<?>> notFound(String what) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, "That company " + what + " does not exist", "NOT_FOUND"));
    }
}
