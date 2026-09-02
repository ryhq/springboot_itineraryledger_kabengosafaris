package com.itineraryledger.kabengosafaris.Accommodation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two properties may share one reservations desk.
 *
 * Acacia Farm Lodge in Karatu and Serengeti Acacia Bliss at Seronera are one company answering one
 * inbox: reservation1@karatuacacialodge.com takes bookings for both, and one switchboard number
 * serves both. The duplicate check asked {@code existsByEmail(address)} across EVERY accommodation,
 * so importing the second contract wrote the camp with no email address and no telephone number at
 * all -- and reported it as "already there", which was true of the address and false of the camp.
 *
 * <p>Nothing looks an accommodation up by its address, so there is nothing that wants the address
 * to be unique across properties. Within one property it still is: the same address twice on one
 * lodge is a mistake.
 *
 * <p>Customer emails keep their global check on purpose. There, one address really does mean one
 * person, and inbound mail is matched to a customer by it.
 */
class SisterPropertiesShareADeskTest {

    private static final Path ACCOMMODATION = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/Accommodation");
    private static final Path TRANSFER = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/DataTransfer/Modules/AccommodationTransfer.java");

    /** Every place that decides whether a contact row is a duplicate. */
    private static final List<String> DECIDERS = List.of(
        "Services/AccommodationEmailServices/CreateAccommodationEmailService.java",
        "Services/AccommodationEmailServices/UpdateAccommodationEmailService.java",
        "Services/AccommodationPhoneServices/CreateAccommodationPhoneService.java",
        "Services/AccommodationPhoneServices/UpdateAccommodationPhoneService.java");

    @Test
    @DisplayName("a contact row is a duplicate only within its own accommodation")
    void duplicateChecksAreScopedToTheAccommodation() throws IOException {
        for (String file : DECIDERS) {
            String source = Files.readString(ACCOMMODATION.resolve(file));
            String where = file.substring(file.lastIndexOf('/') + 1);

            assertFalse(source.contains("existsByEmail(") || source.contains("existsByPhoneNumber("),
                where + " asks whether the address exists ANYWHERE. Two sister properties share a "
                    + "reservations desk, and this is what left Serengeti Acacia Bliss with no "
                    + "contact details. Scope it with existsByAccommodationIdAnd…");

            assertTrue(source.contains("existsByAccommodationIdAnd"),
                where + " must scope its duplicate check to the accommodation");
        }
    }

    @Test
    @DisplayName("an import skips a contact only when that property already has it")
    void theTransferModuleIsScopedToo() throws IOException {
        String source = Files.readString(TRANSFER);

        assertFalse(source.contains("existsByEmail(") || source.contains("existsByPhoneNumber("),
            "a bundle carrying two sister properties would skip the second one's shared address "
                + "and report it as already here, which is true of the address and false of the "
                + "property");
        assertTrue(source.contains("existsByAccommodationIdAndEmail(lodge.getId()")
                && source.contains("existsByAccommodationIdAndPhoneNumber(lodge.getId()"),
            "the import's dedupe must ask about the lodge it is writing");
    }

    @Test
    @DisplayName("customer contacts keep their global uniqueness")
    void customersAreDeliberatelyDifferent() throws IOException {
        Path customers = Path.of("src/main/java/com/itineraryledger/kabengosafaris/Customer/"
            + "Services/CustomerEmailServices/CreateCustomerEmailService.java");
        assertTrue(Files.readString(customers).contains("existsByEmail("),
            "one address means one customer: inbound mail is matched to a person by it, so this "
                + "check is global on purpose and was not meant to be swept along with the "
                + "accommodation fix");
    }
}
