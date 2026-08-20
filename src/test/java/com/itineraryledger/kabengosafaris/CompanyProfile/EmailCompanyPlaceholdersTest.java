package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentMatchers;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyPlaceholderRenderer;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateService;

/**
 * That a shipped email template's {{company…}} placeholders are actually filled.
 *
 * The trap this guards: substitution used to be driven by the EVENT's declared variable list, so a
 * placeholder no event mentioned survived into the sent message as literal braces — the reader would
 * have seen "{{companyName}}" in the footer. Rendered here against a REAL template file, with an
 * event that declares nothing at all, which is the worst case.
 */
class EmailCompanyPlaceholdersTest {

    private static final Path TEMPLATE =
        Path.of("src/main/resources/templates/email-templates/send_invoice_default.html");

    @Test
    @DisplayName("a real template renders the company's details, and leaves no braces behind")
    void companyPlaceholdersAreFilled() throws Exception {
        String html = render(Files.readString(TEMPLATE));

        assertTrue(html.contains("Test Tours"), "the company name never reached the footer");
        assertTrue(html.contains("hello@example.com"), "the reply address never reached the footer");
        assertTrue(html.contains(String.valueOf(java.time.Year.now().getValue())),
            "the copyright line should carry this year");
        assertFalse(html.contains("{{company"), "a company placeholder survived to the reader:\n"
            + snippetAround(html, "{{company"));
        assertFalse(html.contains("{{currentYear}}"), "the year placeholder survived to the reader");
    }

    @Test
    @DisplayName("an optional block for something the company does not have disappears entirely")
    void optionalBlocksDisappear() {
        String html = render("""
            <p>Follow us:</p>
            {{#companyInstagram}}<a href="{{companyInstagram}}">Instagram</a>{{/companyInstagram}}
            {{#companyFacebook}}<a href="{{companyFacebook}}">Facebook</a>{{/companyFacebook}}
            """);

        assertTrue(html.contains("instagram.com/example"), "the page it DOES have should be linked");
        assertFalse(html.contains("Facebook"), "a page it does not have must leave no dead link:\n" + html);
    }

    // ------------------------------------------------------------------ fixture

    private String render(String templateContent) {
        CompanyIdentityService identity = mock(CompanyIdentityService.class);
        when(identity.variables()).thenReturn(Map.of(
            "companyName", "Test Tours",
            "companyLegalName", "Test Tours Ltd",
            "companyEmail", "hello@example.com",
            "companyPhone", "+255 700 000 001",
            "companyPhoneSecondary", "",
            "companyAddress", "Arusha, Tanzania",
            "companyWebsite", "example.com",
            "companyInstagram", "https://instagram.com/example",
            "companyFacebook", "",
            "companyLogoUrl", "https://api.example.com/api/public/company/assets/logo-email"));

        EmailEvent event = new EmailEvent();
        event.setId(1L);
        event.setName("INVOICE_SENT");
        event.setEnabled(true);
        /* the worst case on purpose: the event declares NOTHING */
        event.setVariablesJson("[]");

        EmailTemplate template = new EmailTemplate();
        template.setId(1L);
        template.setFileName("send_invoice_default.html");
        template.setEnabled(true);
        template.setIsDefault(true);

        EmailEventRepository events = mock(EmailEventRepository.class);
        when(events.findByName(anyString())).thenReturn(Optional.of(event));

        EmailTemplateRepository templates = mock(EmailTemplateRepository.class);
        /* the renderer picks the default template through a Specification, so stub THAT overload */
        when(templates.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<EmailTemplate>>any()))
            .thenReturn(List.of(template));

        EmailTemplateService service = mock(EmailTemplateService.class);
        when(service.readTemplateFile(anyString())).thenReturn(templateContent);

        /* the company pass is its own component now, shared with signatures */
        CompanyPlaceholderRenderer companyRenderer = new CompanyPlaceholderRenderer(identity);
        return new EmailTemplateRenderer(events, templates, service, identity, companyRenderer)
            .renderTemplate("INVOICE_SENT", Map.of());
    }

    private String snippetAround(String html, String needle) {
        int at = html.indexOf(needle);
        return at < 0 ? "" : html.substring(Math.max(0, at - 80), Math.min(html.length(), at + 80));
    }
}
