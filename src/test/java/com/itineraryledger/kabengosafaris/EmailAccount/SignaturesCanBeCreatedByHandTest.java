package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That a signature written by a person can actually be saved.
 *
 * It could not. `isSystemDefault` is NOT NULL on the column and had no default on the field, and
 * the hand-made path never set it. The insert therefore failed at COMMIT, after the method's own
 * try/catch had already returned 201 — the class is @Transactional, so the violation surfaced from
 * the commit rather than the catch, and what the office saw was "An unexpected error occurred"
 * naming no field at all.
 *
 * The automatic path always set it, so every signature in either company had been made by the
 * system and nobody had ever successfully written one. Two of the mailboxes still have none.
 *
 * Both halves are pinned here: the builder says what it is, and the field cannot be null even if a
 * third path forgets.
 */
class SignaturesCanBeCreatedByHandTest {

    private static final Path ENTITY = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "EmailAccount/EmailAccountSignatures/ModalEntity/EmailAccountSignature.java");
    private static final Path CREATE = Path.of("src/main/java/com/itineraryledger/kabengosafaris/"
        + "EmailAccount/EmailAccountSignatures/Services/EmailAccountSignatureCreateService.java");

    @Test
    @DisplayName("the hand-made path sets isSystemDefault, so the insert does not fail at commit")
    void theBuilderSaysWhatItIs() throws IOException {
        String source = Files.readString(CREATE);

        /* The service has two builders: the one a person's request reaches, and the automatic one. */
        Matcher builders = Pattern.compile("EmailAccountSignature\\.builder\\(\\)(.*?)\\.build\\(\\)",
            Pattern.DOTALL).matcher(source);
        int found = 0;
        while (builders.find()) {
            found++;
            assertTrue(builders.group(1).contains(".isSystemDefault("),
                "a builder here does not set isSystemDefault. The column is NOT NULL, so the row is "
                    + "rejected at commit, outside the try/catch, and the caller is told only that "
                    + "something unexpected happened.");
        }
        assertEquals(2, found,
            "expected the hand-made and the automatic builder; found " + found);
    }

    @Test
    @DisplayName("and the field cannot be null even if a third path forgets")
    void theFieldDefendsItself() throws IOException {
        String entity = Files.readString(ENTITY);
        int at = entity.indexOf("private Boolean isSystemDefault");
        assertTrue(at > 0, "the field has moved");

        String before = entity.substring(Math.max(0, at - 200), at);
        assertTrue(before.contains("@Builder.Default"),
            "a NOT NULL column with no default on the field is a commit-time failure waiting for "
                + "the next builder that forgets it");
        assertTrue(entity.contains("private Boolean isSystemDefault = false;"),
            "the default should be false: a signature is a system one only when the system made it");
    }

    @Test
    @DisplayName("a NOT NULL boolean on this entity either defaults or is always set")
    void everyRequiredFlagIsSafe() throws IOException {
        /*
         * The same shape would break the same way. isDefault and enabled are safe because the
         * builder passes Boolean.TRUE.equals(...), which is false rather than null, but a reader
         * should not have to work that out for each one.
         */
        String entity = Files.readString(ENTITY);
        String create = Files.readString(CREATE);

        for (String flag : new String[] {"isDefault", "enabled", "isSystemDefault"}) {
            boolean defaulted = entity.contains("private Boolean " + flag + " = ");
            boolean alwaysSet = create.split("\\." + flag + "\\(", -1).length - 1 >= 2;
            assertTrue(defaulted || alwaysSet,
                flag + " is NOT NULL, has no default, and is not set by both builders");
        }
    }
}
