package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A reply saved as a draft has to thread where sending it would have.
 *
 * <p>Found on a supplier thread with four people copied. A reply to "TIN REQUEST" was drafted
 * rather than sent, and {@code saveDraft} passed null for the original message: no In-Reply-To,
 * no References, and a thread id of its own. The subject said RE: and nothing else agreed, so at
 * the other end it would have arrived as a new conversation beside the question it answered.
 *
 * <p>{@code updateDraft} was worse, because it rebuilds the .eml from scratch. Even once a draft
 * threaded correctly, opening it to fix a typo stripped the headers again and overwrote the
 * message id. The reply un-replied itself on save, silently.
 *
 * <p>The send path has always resolved this. These two just never did.
 */
class ADraftedReplyIsStillAReplyTest {

    private static final Path COMPOSE = Path.of(
        "src/main/java/com/itineraryledger/kabengosafaris/EmailAccount/EmailMessage/Services/EmailComposeService.java");

    private String source() throws IOException {
        return Files.readString(COMPOSE);
    }

    @Test
    @DisplayName("neither draft path builds its message with a null original any more")
    void noDraftPathThrowsAwayTheOriginal() throws IOException {
        String src = source();
        assertFalse(src.contains("buildMimeMessage(mailSender, account, dto, attachments, null)"),
            "a draft built with a null original carries no In-Reply-To or References, so it "
                + "arrives as a new conversation beside the message it answers");
    }

    @Test
    @DisplayName("a drafted reply carries the three threading fields the sent copy carries")
    void theDraftRowCarriesTheThread() throws IOException {
        String src = source();
        assertTrue(src.contains(".inReplyTo(inReplyTo)"), "the draft row must store In-Reply-To");
        assertTrue(src.contains(".references(references)"), "the draft row must store References");
        assertTrue(src.contains(".threadId(threadId)"), "the draft row must store its thread");
    }

    @Test
    @DisplayName("editing a draft keeps the thread it was saved into")
    void editingDoesNotUnthread() throws IOException {
        String src = source();
        assertTrue(src.contains("draft.setInReplyTo(replyTo.getMessageId())"),
            "updateDraft rebuilds the .eml, so it must put the threading back or a typo fix "
                + "turns a reply into a new conversation");
        assertTrue(src.contains("draft.setThreadId(replyTo.getThreadId())"),
            "updateDraft must keep the draft in its original thread");
    }
}
