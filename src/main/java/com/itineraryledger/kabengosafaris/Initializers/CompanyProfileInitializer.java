package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.*;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * The company profile row, created once and never overwritten.
 *
 * "Never overwritten" is the important half: this runs on every restart, and an operator who
 * corrects their own address must not have it reverted by the next deploy — the same rule the other
 * settings initializers follow.
 *
 * For an install that already has documents in the wild (Kabengo), the values seeded here are the
 * ones those documents ALREADY print, read out of the shipped templates, so moving the identity into
 * a record changes nothing anybody receives.
 *
 * Deliberately left empty, because the templates disagree with each other and a guess would be
 * printed on a tax document:
 *   - the postal box: the signature says 11271, the proforma letterhead says 11721
 *   - the legal name: one template says "Ltd", another "Limited"
 *   - TIN and VRN: not in any shipped template
 * They surface as gaps in Settings → Company for somebody who knows the answer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CompanyProfileInitializer implements ApplicationRunner, Ordered {

    private final CompanyProfileRepository repository;
    private final CompanyIdentityService identityService;

    @Value("${app.company.name:}")
    private String configuredName;

    @Value("${app.company.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public int getOrder() {
        /* after the settings initializers, before anything that renders a document */
        return 25;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) return;
        if (repository.count() > 0) {
            log.info("Company profile already exists — left exactly as it is");
            return;
        }

        String name = (configuredName == null || configuredName.isBlank()) ? "" : configuredName.trim();
        CompanyProfile profile = CompanyProfile.builder()
            .tradingName(name.isEmpty() ? "Set your company name" : name)
            .defaultCurrency("TZS")
            .timezone("Africa/Dar_es_Salaam")
            .locale("en-TZ")
            .build();

        /*
         * Only seed contact details for the install these values came from. A different company
         * getting Kabengo's phone number in its profile is the exact bug this whole phase exists to
         * prevent, so the name has to match before any of it is written.
         */
        if (name.equalsIgnoreCase("Kabengo Safaris")) {
            profile.getEmails().add(CompanyEmail.builder()
                .companyProfile(profile).email("info@kabengosafaris.com")
                .emailType(CompanyEmail.EmailType.GENERAL).label("General")
                .isPrimary(true).isActive(true).displayOrder(0).build());

            profile.getPhones().add(CompanyPhone.builder()
                .companyProfile(profile).countryCode("+255").phoneNumber("746 598 330")
                .phoneType(CompanyPhone.PhoneType.MOBILE).label("Office")
                .isWhatsApp(true).isPrimary(true).isActive(true).displayOrder(0).build());
            profile.getPhones().add(CompanyPhone.builder()
                .companyProfile(profile).countryCode("+255").phoneNumber("786 345 408")
                .phoneType(CompanyPhone.PhoneType.MOBILE).label("Alternate")
                .isPrimary(false).isActive(true).displayOrder(1).build());

            /* city and country only: the box number is the one the templates disagree about */
            profile.getAddresses().add(CompanyAddress.builder()
                .companyProfile(profile).addressType(CompanyAddress.AddressType.POSTAL)
                .label("Head office").city("Arusha").country("Tanzania")
                .isPrimary(true).isActive(true).displayOrder(0).build());

            profile.getLinks().add(CompanyLink.builder()
                .companyProfile(profile).url("https://www.kabengosafaris.com")
                .linkType(CompanyLink.LinkType.WEBSITE).label("Website")
                .isPrimary(true).isActive(true).displayOrder(0).build());
        }

        repository.save(profile);
        identityService.invalidate();
        log.info("Company profile created: {} ({} emails, {} phones, {} addresses, {} links)",
            profile.displayName(), profile.getEmails().size(), profile.getPhones().size(),
            profile.getAddresses().size(), profile.getLinks().size());
    }
}
