package com.itineraryledger.kabengosafaris.EmailEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Translation.Services.TranslationService;

/**
 * Somebody who wrote to us in German should not be answered in English.
 *
 * <p>A German family enquired through the German site. "de" was stored on the inquiry, and the
 * acknowledgement went out in English, because nothing carried the language as far as the email.
 *
 * <p>The template stays English and is translated on the way out, the same way a PDF is. Which
 * makes the fallbacks the thing worth testing: an acknowledgement that arrives in the wrong
 * language is a small disappointment, one that never arrives because the translation engine was
 * unreachable is a customer who thinks the form is broken.
 */
class AnAcknowledgementSpeaksTheirLanguageTest {

    private static final String ENGLISH_HTML = "<html><body><p>Thank you for telling us.</p></body></html>";
    private static final String GERMAN_HTML = "<html><body><p>Danke für Ihre Nachricht.</p></body></html>";

    private EmailTemplateRenderer renderer;
    private EmailSendingService mail;
    private TranslationService translations;
    private CustomerAcknowledgementSender sender;

    @BeforeEach
    void setUp() throws Exception {
        renderer = mock(EmailTemplateRenderer.class);
        mail = mock(EmailSendingService.class);
        translations = mock(TranslationService.class);
        CompanyIdentityService company = mock(CompanyIdentityService.class);
        when(company.variables()).thenReturn(Map.of("companyWebsite", "", "companyPhone", ""));
        when(renderer.renderTemplate(anyString(), anyMap())).thenReturn(ENGLISH_HTML);

        sender = new CustomerAcknowledgementSender(renderer, mail, company, translations);
    }

    private String bodySentTo(String language) {
        org.mockito.Mockito.clearInvocations(mail);   // each send is asserted on its own
        sender.send("BOOKING_INQUIRY_RECEIVED", "an.ge20@web.de",
            "We have your safari enquiry (INQ-0001-09-26)", Map.of(), language);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mail, timeout(2000)).sendHtmlEmail(eq("an.ge20@web.de"), anyString(), body.capture());
        return body.getValue();
    }

    private String subjectSentTo(String language) {
        org.mockito.Mockito.clearInvocations(mail);
        sender.send("BOOKING_INQUIRY_RECEIVED", "an.ge20@web.de",
            "We have your safari enquiry (INQ-0001-09-26)", Map.of(), language);
        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        verify(mail, timeout(2000)).sendHtmlEmail(eq("an.ge20@web.de"), subject.capture(), anyString());
        return subject.getValue();
    }

    @Test
    @DisplayName("a German enquirer is answered in German")
    void germanGetsGerman() {
        when(translations.translateHtml(ENGLISH_HTML, "de")).thenReturn(GERMAN_HTML);

        assertEquals(GERMAN_HTML, bodySentTo("de"),
            "the locale was on the inquiry all along; it just never reached the email");
    }

    @Test
    @DisplayName("no language, or English, sends the template untouched and never calls the engine")
    void englishIsNotTranslated() {
        assertEquals(ENGLISH_HTML, bodySentTo(null));
        assertEquals(ENGLISH_HTML, bodySentTo("en"));
        verify(translations, org.mockito.Mockito.never()).translateHtml(anyString(), anyString());
    }

    @Test
    @DisplayName("a translation engine that is down still sends the email, in English")
    void aDeadEngineDoesNotEatTheEmail() {
        when(translations.translateHtml(anyString(), anyString()))
            .thenThrow(new RuntimeException("Read timed out"));

        assertEquals(ENGLISH_HTML, bodySentTo("de"),
            "arriving in the wrong language is a disappointment; not arriving is a lost customer");
    }

    @Test
    @DisplayName("a translation that comes back empty is not sent as an empty email")
    void emptyTranslationFallsBack() {
        when(translations.translateHtml(ENGLISH_HTML, "de")).thenReturn("   ");

        assertEquals(ENGLISH_HTML, bodySentTo("de"));
    }

    @Test
    @DisplayName("the enquiry reference survives translation of the subject")
    void theReferenceIsNotTranslated() throws Exception {
        when(translations.translateHtml(anyString(), anyString())).thenReturn(GERMAN_HTML);
        when(translations.translatePlainText("We have your safari enquiry", "en", "de"))
            .thenReturn("Ihre Safari-Anfrage ist bei uns");

        String subject = subjectSentTo("de");

        assertTrue(subject.endsWith("(INQ-0001-09-26)"),
            "the reference is what the customer quotes back and what we search on: " + subject);
        assertTrue(subject.startsWith("Ihre Safari-Anfrage"), subject);
    }

    @Test
    @DisplayName("a subject the engine cannot translate is sent as it stands, reference and all")
    void subjectFallsBackWhole() throws Exception {
        when(translations.translateHtml(anyString(), anyString())).thenReturn(GERMAN_HTML);
        when(translations.translatePlainText(anyString(), any(), any()))
            .thenThrow(new RuntimeException("engine unreachable"));

        assertEquals("We have your safari enquiry (INQ-0001-09-26)", subjectSentTo("de"));
    }
}
