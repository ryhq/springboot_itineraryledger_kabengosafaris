package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountEnums;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccountProvider;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.ReceivingProtocol;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.SendingMethod;

/**
 * That an email account's enums can be sent by name.
 *
 * They used to be 1-based codes that appear in no enum and no response — 6 for CUSTOM, 1 for IMAP —
 * so a caller had to know them by heart. The panel sends the name it displays, and because the DTO
 * field was an Integer, Jackson rejected the ENTIRE body: "Required request body is missing or
 * malformed", naming neither the field nor the value, on a form where every box looked filled in.
 * Setting a mailbox's protocol to IMAP was impossible from the panel and the error said nothing
 * about protocols.
 *
 * Both forms are accepted now: the codes for whatever already sends them, names for everything else.
 */
class EmailAccountEnumsTest {

    @Test
    @DisplayName("names are accepted, which is what the panel sends")
    void namesWork() {
        assertEquals(ReceivingProtocol.IMAP, EmailAccountEnums.receivingProtocol("IMAP"));
        assertEquals(ReceivingProtocol.NONE, EmailAccountEnums.receivingProtocol("NONE"));
        assertEquals(SendingMethod.SMTP, EmailAccountEnums.sendingMethod("SMTP"));
        assertEquals(EmailAccountProvider.CUSTOM, EmailAccountEnums.provider("CUSTOM"));
        assertEquals(EmailAccountProvider.RESEND, EmailAccountEnums.provider("resend"),
            "case should not decide whether a form saves");
    }

    @Test
    @DisplayName("the old numeric codes still work, so nothing that already sends them breaks")
    void codesStillWork() {
        assertEquals(EmailAccountProvider.GMAIL, EmailAccountEnums.provider("1"));
        assertEquals(EmailAccountProvider.CUSTOM, EmailAccountEnums.provider("6"));
        assertEquals(EmailAccountProvider.RESEND, EmailAccountEnums.provider("7"));
        assertEquals(SendingMethod.API, EmailAccountEnums.sendingMethod("1"));
        assertEquals(SendingMethod.SMTP, EmailAccountEnums.sendingMethod("2"));
        assertEquals(ReceivingProtocol.IMAP, EmailAccountEnums.receivingProtocol("1"));
        assertEquals(ReceivingProtocol.POP3, EmailAccountEnums.receivingProtocol("2"));
    }

    @Test
    @DisplayName("a value that is neither is rejected, not guessed at")
    void nonsenseIsNull() {
        /*
         * EmailAccountProvider.fromString falls back to CUSTOM for anything it does not recognise,
         * which would turn a typo into a silently reconfigured account. Parsing a request must say
         * "no" so the caller is told which field it was.
         */
        assertNull(EmailAccountEnums.provider("SMOKE_SIGNAL"));
        assertNull(EmailAccountEnums.sendingMethod("CARRIER_PIGEON"));
        assertNull(EmailAccountEnums.receivingProtocol("SEMAPHORE"));
        assertNull(EmailAccountEnums.provider("99"));
    }

    @Test
    @DisplayName("the DTO fields stay String, or Jackson rejects the body before anyone parses it")
    void theDtoAcceptsBothShapes() throws IOException {
        /*
         * This is the actual defect. The parser above is only reachable if the request survives
         * deserialisation, and an Integer field does not survive "IMAP".
         */
        for (String dto : new String[] {"CreateEmailAccountDTO", "UpdateEmailAccountDTO"}) {
            String source = Files.readString(Path.of(
                "src/main/java/com/itineraryledger/kabengosafaris/EmailAccount/DTOs/" + dto + ".java"));
            for (String field : new String[] {"providerType", "sendingMethod", "receivingProtocol"}) {
                assertFalse(source.contains("private Integer " + field + ";"),
                    dto + "." + field + " is an Integer again — a request sending the enum's NAME"
                        + " will be refused as a malformed body, naming neither field nor value");
                assertTrue(source.contains("private String " + field + ";"),
                    dto + "." + field + " should be a String so both a name and a code parse");
            }
        }
    }

    @Test
    @DisplayName("every value the panel offers parses")
    void everyPanelOptionParses() {
        /*
         * The panel's dropdowns are the only writers here, so what they can offer must be
         * acceptable. Kept as literals deliberately: if someone adds an option there, this fails
         * and the API is extended to match, rather than the user meeting a malformed-body error.
         */
        for (String protocol : new String[] {"IMAP", "NONE"}) {
            assertNotNull(EmailAccountEnums.receivingProtocol(protocol), protocol);
        }
        for (String method : new String[] {"API", "SMTP"}) {
            assertNotNull(EmailAccountEnums.sendingMethod(method), method);
        }
        for (String provider : new String[] {
            "GMAIL", "OUTLOOK", "SENDGRID", "MAILGUN", "AWS_SES", "CUSTOM", "RESEND"}) {
            assertNotNull(EmailAccountEnums.provider(provider), provider);
        }
    }
}
