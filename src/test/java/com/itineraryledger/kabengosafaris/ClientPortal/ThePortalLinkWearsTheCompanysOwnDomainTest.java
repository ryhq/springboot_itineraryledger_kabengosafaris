package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalOriginService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Entity.CompanyProfile;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;

/**
 * What a client is told to open.
 *
 * <p>A link is the first thing a customer sees of a company, and
 * {@code api.something.ryhqtech.com/portal/…} tells them they are looking at our plumbing. The
 * address is a setting; when nobody has set it, it is built from the company's own domain rather
 * than from the API's, because the company's domain is the thing a customer already recognises.
 *
 * <p>The website field is not a tidy URL in either live company — one holds a {@code www.} prefix
 * and the other a locale path — so the normalising is the part worth pinning. Each case here is a
 * value that actually exists or plausibly could, not an invented one.
 */
class ThePortalLinkWearsTheCompanysOwnDomainTest {

    private PortalOriginService serviceFor(String portalUrl, String websiteUrl) {
        CompanyProfileRepository repository = mock(CompanyProfileRepository.class);
        CompanyProfile profile = CompanyProfile.builder()
            .portalUrl(portalUrl)
            .websiteUrl(websiteUrl)
            .build();
        when(repository.findAll()).thenReturn(List.of(profile));
        return new PortalOriginService(repository);
    }

    @Test
    @DisplayName("a configured address is used exactly as given, minus any trailing slash")
    void configuredWins() {
        assertEquals("https://mytrip.example.com",
            serviceFor("https://mytrip.example.com/", "https://www.example.com").get(),
            "an explicit setting is an instruction, not a hint");
    }

    @Test
    @DisplayName("a bare host with www. becomes mytrip. on the same domain")
    void derivesFromBareHost() {
        assertEquals("https://mytrip.kabengosafaris.com",
            serviceFor(null, "www.kabengosafaris.com").get(),
            "no scheme and a www. prefix is what this field actually holds today");
    }

    @Test
    @DisplayName("a locale path on the website address does not end up in the portal address")
    void derivesFromHostWithPath() {
        assertEquals("https://mytrip.jateloafricantravels.com",
            serviceFor("", "jateloafricantravels.com/en").get(),
            "a path belongs to the website, not to the host the portal answers on");
    }

    @Test
    @DisplayName("nothing recognisable yields nothing, rather than a hostname nobody can reach")
    void refusesRubbish() {
        assertNull(serviceFor(null, "localhost").get(), "no dot, so it is not a domain");
        assertNull(serviceFor(null, "   ").get(), "blank is not an address");
        assertNull(serviceFor(null, null).get(), "the caller falls back to the API's own address");
    }
}
