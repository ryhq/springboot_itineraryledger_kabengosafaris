package com.itineraryledger.kabengosafaris.ClientPortal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.EditingLevel;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.JourneyStatus;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalCodeRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalDeviceRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalJourneyRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalParticipantRepository;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalActivityService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalJourneyService;
import com.itineraryledger.kabengosafaris.ClientPortal.Services.PortalTokens;
import com.itineraryledger.kabengosafaris.CompanyProfile.Repository.CompanyProfileRepository;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;

/**
 * A setting that does nothing must not be offered.
 *
 * <p>CHOOSE_CATALOGUE was designed, named, documented, given a display string and wired into
 * three dropdowns, and nothing anywhere consulted it. {@code PortalEditingService} gates on
 * {@code allowsChanges()} alone, so choosing it did precisely what CHOOSE_OFFERED did. An
 * operator could set it on a company, believe they had opened the whole catalogue to every
 * client, and be wrong, with nothing on any screen to say so.
 *
 * <p>That is the facet rule from this project's own conventions applied to a setting: one that
 * does nothing is worse than one that is absent, because the absent one is at least honest.
 *
 * <p>The scan at the bottom is the part that matters past today. It ties availability to
 * whether anything actually enforces the level, so phase three cannot build catalogue editing
 * and forget to switch the setting back on, and cannot switch the setting on before building
 * it. Either mistake fails here with a sentence saying which way round it is.
 */
class TheCatalogueLevelIsNotOfferedUntilItWorksTest {

    private static final Path MAIN = Path.of("src/main/java/com/itineraryledger/kabengosafaris");

    private PortalJourneyService journeyService() {
        return new PortalJourneyService(
            mock(PortalJourneyRepository.class), mock(PortalParticipantRepository.class),
            mock(PortalDeviceRepository.class), mock(PortalCodeRepository.class),
            mock(PortalAccessEventRepository.class), mock(PortalTokens.class),
            mock(PortalActivityService.class), mock(CompanyProfileRepository.class),
            mock(QuoteRevisionService.class));
    }

    private PortalJourney journey() {
        PortalJourney journey = PortalJourney.builder()
            .status(JourneyStatus.OPEN).locked(false).build();
        journey.setId(7L);
        return journey;
    }

    @Test
    @DisplayName("the catalogue level is not available, and the other two are")
    void onlyTheBuiltLevelsAreAvailable() {
        assertFalse(EditingLevel.CHOOSE_CATALOGUE.isAvailable(),
            "it behaves exactly like CHOOSE_OFFERED, which is the worst state for a setting");
        assertTrue(EditingLevel.RESPOND.isAvailable());
        assertTrue(EditingLevel.CHOOSE_OFFERED.isAvailable());
    }

    @Test
    @DisplayName("setting it on one booking leaves the booking alone")
    void aJourneyCannotBeSetToIt() {
        PortalJourney journey = journey();
        journeyService().setEditingLevel(journey, EditingLevel.CHOOSE_CATALOGUE);

        assertNull(journey.getEditingLevel(),
            "silently accepting it is how somebody comes to believe it is in force");
    }

    @Test
    @DisplayName("narrowing a booking still works, because that is the control that matters")
    void narrowingStillWorks() {
        PortalJourney journey = journey();
        journeyService().setEditingLevel(journey, EditingLevel.RESPOND);

        assertEquals(EditingLevel.RESPOND, journey.getEditingLevel());
    }

    // ------------------------------------------------------------------ the scan

    /**
     * Availability and enforcement move together, in both directions.
     *
     * <p>Phase three will write the portal-safe accommodation search and call
     * {@code allowsCatalogue()} from it. At that moment this test starts failing and says to
     * flip {@code isAvailable()}. If somebody instead flips availability first, it fails the
     * other way. Neither half can ship without the other.
     */
    @Test
    @DisplayName("a level is offered exactly when something enforces it")
    void availabilityFollowsEnforcement() throws IOException {
        boolean enforcedSomewhere = false;

        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().equals("EditingLevel.java")) continue;
                if (Files.readString(file).contains("allowsCatalogue()")) {
                    enforcedSomewhere = true;
                    break;
                }
            }
        }

        assertEquals(enforcedSomewhere, EditingLevel.CHOOSE_CATALOGUE.isAvailable(),
            enforcedSomewhere
                ? "catalogue editing is now enforced somewhere, so the level should be "
                    + "selectable: flip EditingLevel.isAvailable() and put it back in the "
                    + "panel's EDITING_LEVELS"
                : "nothing calls allowsCatalogue(), so the level enforces nothing and must not "
                    + "be selectable");
    }
}
