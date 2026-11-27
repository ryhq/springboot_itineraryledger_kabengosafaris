package com.itineraryledger.kabengosafaris.EmailAccount;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * "Save this, do not send it" has to mean that on every endpoint that can send.
 *
 * <p>{@code ComposeEmailDTO} carries an {@code isDraft} flag and four endpoints take that same
 * DTO: compose, reply, replyAll and forward. Only compose ever read it. The other three
 * accepted the field, ignored it, and sent the message - so a reply written as a draft was
 * delivered to a customer, with an attachment, and there is no recalling an email.
 *
 * <p>A flag that is accepted and ignored is worse than one that is rejected, because the caller
 * has no way to find out. It is worst of all when the value being ignored means "do not send".
 *
 * <p>So the check now sits at the single exit - {@code sendEmail} - rather than on each caller,
 * and this is what stops it being put back on the callers and forgotten on the next one. The
 * test reads the source rather than exercising a mail server, because the thing that broke was
 * the shape of the code and not the behaviour of SMTP.
 */
class ADraftIsNeverDeliveredTest {

    private static final Path COMPOSE_SERVICE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/EmailAccount/EmailMessage/Services/"
            + "EmailComposeService.java");

    @Test
    @DisplayName("the one method that sends refuses to send a draft")
    void theGuardIsAtTheExit() throws IOException {
        String body = Files.readString(COMPOSE_SERVICE);

        int sendEmail = body.indexOf("private ResponseEntity<ApiResponse<?>> sendEmail(");
        assertTrue(sendEmail > 0,
            "sendEmail is gone or renamed - if sending moved, the guard has to move with it");

        /* The first thing it does, before a MIME message exists and before any socket opens. */
        String opening = body.substring(sendEmail, Math.min(body.length(), sendEmail + 500));
        assertTrue(opening.contains("Boolean.TRUE.equals(dto.getIsDraft())"),
            "sendEmail does not check isDraft, so every endpoint that calls it will deliver a "
                + "message somebody asked to be saved as a draft:\n" + opening);
        assertTrue(opening.contains("saveDraft("),
            "sendEmail notices isDraft but does not save the draft, so the message is lost "
                + "rather than sent, which is a different way of being wrong");
    }

    @Test
    @DisplayName("every endpoint that takes the flag reaches that guard")
    void noEndpointSendsAroundIt() throws IOException {
        String body = Files.readString(COMPOSE_SERVICE);

        /*
         * reply, replyAll and forward each take ComposeEmailDTO and each used to call sendEmail
         * directly. That is still fine - the guard is inside sendEmail - but none of them may
         * call the transports underneath it, which would bypass the check entirely.
         */
        List<String> transports = List.of("sendViaResendApi(", "sendViaSmtp(", "Transport.send(");
        List<String> offenders = new ArrayList<>();

        for (String method : List.of("reply", "replyAll", "forward", "composeAndSend")) {
            String source = methodBody(body, method);
            if (source == null) continue;     // renamed or removed; the test above still holds
            for (String transport : transports) {
                if (source.contains(transport)) {
                    offenders.add(method + " calls " + transport + " directly, going around the "
                        + "isDraft check in sendEmail");
                }
            }
        }

        assertTrue(offenders.isEmpty(), String.join("\n  ", offenders));
    }

    @Test
    @DisplayName("the flag is not checked in two places, which is how one of them drifts")
    void thereIsExactlyOneGuard() throws IOException {
        String body = Files.readString(COMPOSE_SERVICE);

        /*
         * It was previously checked in composeAndSend only. Duplicating it per caller is what
         * produced three endpoints that forgot, so there is deliberately one.
         */
        Matcher guard = Pattern.compile("Boolean\\.TRUE\\.equals\\(\\s*dto\\.getIsDraft\\(\\)\\s*\\)")
            .matcher(body);
        int count = 0;
        while (guard.find()) count++;

        assertEquals(1, count,
            "There should be exactly one isDraft guard, inside sendEmail. " + count + " found: a "
                + "check copied onto each caller is how three of the four came to be missing it.");
    }

    /** Everything between a method's signature and the start of the next one. */
    private static String methodBody(String source, String name) {
        Matcher m = Pattern.compile(
            "public ResponseEntity<ApiResponse<\\?>> " + name + "\\(").matcher(source);
        if (!m.find()) return null;
        int start = m.start();
        Matcher next = Pattern.compile("\n    (public|private) ").matcher(source);
        int end = next.find(start + 10) ? next.start() : source.length();
        return source.substring(start, end);
    }
}
