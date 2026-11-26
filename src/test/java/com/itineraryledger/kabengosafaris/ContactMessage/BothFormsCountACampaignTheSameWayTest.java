package com.itineraryledger.kabengosafaris.ContactMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A campaign is counted the same way wherever somebody landed.
 *
 * <p>The website has two front doors. Most people use the planner; some write on the contact
 * page instead, and an ad click can land on either. Both have recorded the arrival since lead
 * attribution shipped - the columns are on both tables - but only the inquiry side ever read it
 * back. The contact side stored a channel, a campaign and a click id and exposed none of them:
 * no field on the DTO, no dimension on the filter, no counter in the stats.
 *
 * <p>So a campaign's leads were split between a list that counted them and a list that did not,
 * and the half that chose the contact form looked like traffic from nowhere. That is worse than
 * not tracking it at all, because the number that <i>was</i> reported looked complete.
 *
 * <p>This pins the two sides together. It compares the shape rather than the behaviour, because
 * the failure is always the same: a dimension added to one module and forgotten on the other.
 */
class BothFormsCountACampaignTheSameWayTest {

    private static final Path ROOT = Path.of("src/main/java/com/itineraryledger/kabengosafaris");

    private static final Path INQUIRY_FILTER =
        ROOT.resolve("BookingInquiry/Specifications/BookingInquiryFilter.java");
    private static final Path MESSAGE_FILTER =
        ROOT.resolve("ContactMessage/Specifications/ContactMessageFilter.java");
    private static final Path MESSAGE_SPEC =
        ROOT.resolve("ContactMessage/Specifications/ContactMessageSpecification.java");
    private static final Path MESSAGE_DTO =
        ROOT.resolve("ContactMessage/DTOs/ContactMessageDTO.java");
    private static final Path MESSAGE_LIST_DTO =
        ROOT.resolve("ContactMessage/DTOs/ContactMessageListItemDTO.java");
    private static final Path MESSAGE_GET =
        ROOT.resolve("ContactMessage/Services/ContactMessageGetService.java");

    /** Every way of asking "where did this come from", as the inquiry list asks it. */
    private static final List<String> DIMENSIONS =
        List.of("channel", "channels", "campaigns", "sources", "tracking");

    @Test
    @DisplayName("both lists can be filtered by the same arrival dimensions")
    void neitherFormIsMissingADimension() throws IOException {
        String inquiry = Files.readString(INQUIRY_FILTER);
        String message = Files.readString(MESSAGE_FILTER);

        List<String> missing = new ArrayList<>();
        for (String dimension : DIMENSIONS) {
            boolean onInquiry = declaresField(inquiry, dimension);
            boolean onMessage = declaresField(message, dimension);
            if (onInquiry && !onMessage) missing.add(dimension);
        }

        assertTrue(missing.isEmpty(),
            "A booking inquiry can be filtered by " + missing + " and a contact message cannot, "
                + "so a campaign's leads are split between a list that counts them and one that "
                + "does not. Add the field to ContactMessageFilter - it is bound with "
                + "@ModelAttribute, so a dimension with no field there is silently ignored.");
    }

    @Test
    @DisplayName("the contact list can act on every dimension it advertises")
    void aDimensionWithNoSpecificationIsADeadFilter() throws IOException {
        String spec = Files.readString(MESSAGE_SPEC);

        for (String method : List.of("byChannel", "byChannels", "byCampaigns", "bySources",
                                     "paidChannels", "untracked", "byTracking")) {
            assertTrue(spec.contains("Specification<ContactMessage> " + method + "("),
                method + " is missing from ContactMessageSpecification, so the matching filter "
                    + "value would be accepted and then quietly do nothing");
        }
    }

    @Test
    @DisplayName("the arrival is read back, not only written down")
    void whatIsStoredIsAlsoShown() throws IOException {
        /*
         * The whole fault this test exists for: the column was on the table and nothing ever
         * selected it. Storage without read-back is indistinguishable from not recording it.
         */
        assertTrue(Files.readString(MESSAGE_DTO).contains("attribution"),
            "ContactMessageDTO has no attribution field, so the record page cannot show where "
                + "the message came from however carefully it was recorded");

        String listDto = Files.readString(MESSAGE_LIST_DTO);
        assertTrue(listDto.contains("channel") && listDto.contains("campaign"),
            "the list rows carry no channel, so there is nothing for a 'Came from' column to show");

        String get = Files.readString(MESSAGE_GET);
        assertTrue(get.contains("attributionService.toDto"),
            "nothing maps the stored arrival onto the DTO, so the field would always be null");
        assertTrue(get.contains("\"untracked\"") && get.contains("\"paid\""),
            "without a paid and an untracked counter the channel breakdown reads as though it "
                + "covered every message, when most of these predate any arrival being recorded");
    }

    /** `private X name;` for the given name, whatever the type in front of it. */
    private static boolean declaresField(String source, String name) {
        return source.matches("(?s).*\\bprivate\\s+[\\w<>, .]+\\s+" + name + "\\s*;.*");
    }
}
