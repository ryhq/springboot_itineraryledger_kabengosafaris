package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyTemplateModel;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyPlaceholderRenderer;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateRenderer;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateStorageService;

/**
 * That {@code ${company.*}} actually resolves in a PDF template.
 *
 * Worth its own test because the 19 shipped PDF templates were rewritten to depend on it: if the
 * expression silently produced nothing, every invoice would print a blank company block, and the
 * first person to find out would be a client.
 */
class PdfCompanyModelTest {

    @Test
    @DisplayName("a template reaches the company through both th:text and inlined [[...]]")
    void companyResolvesInThymeleaf() {
        PdfTemplateRenderer renderer = renderer();

        String html = renderer.renderFromString("""
            <html><body>
              <h1 th:text="${company.name}">placeholder</h1>
              <p>Contact [[${company.email}]] on [[${company.phone}]]</p>
              <p th:text="${company.address}">placeholder</p>
              <p>TIN [[${company.tin}]] · [[${company.website}]]</p>
              <p th:text="${company.bank.accountNumber}">placeholder</p>
              <p th:text="${company.formalName}">placeholder</p>
              <p>&copy; [[${company.year}]]</p>
              <img th:if="${company.hasLogo()}" th:src="${company.logoLightUrl}"/>
            </body></html>
            """, Map.of());

        assertTrue(html.contains("Test Tours"), "th:text did not resolve the company name:\n" + html);
        assertTrue(html.contains("hello@example.com"), "inlined [[${company.email}]] did not resolve:\n" + html);
        assertTrue(html.contains("+255 700 000 001"), "inlined phone did not resolve");
        assertTrue(html.contains("Arusha, Tanzania"), "th:text address did not resolve");
        assertTrue(html.contains("123-456-789"), "inlined TIN did not resolve");
        assertTrue(html.contains("example.com"), "inlined website did not resolve");
        assertTrue(html.contains("0150123456700"), "the nested bank object did not resolve");
        assertTrue(html.contains("Test Tours Ltd"), "formalName should prefer the registered entity");
        assertTrue(html.contains(String.valueOf(java.time.Year.now().getValue())), "the year did not resolve");
        assertTrue(html.contains("logo-light"), "hasLogo()/th:src did not render the logo");
        assertFalse(html.contains("placeholder"), "every placeholder body should have been replaced:\n" + html);
    }

    @Test
    @DisplayName("an unfilled profile prints blanks, never the word null and never an <img> with no src")
    void emptyProfileIsSafe() {
        CompanyIdentityService identity = mock(CompanyIdentityService.class);
        when(identity.templateModel()).thenReturn(CompanyTemplateModel.builder()
            .name("Your Company").legalName("").email("").phone("").address("").website("")
            .tin("").vrn("").registrationNumber("").licenceNumber("").currency("")
            .emails(List.of()).phones(List.of()).socials(Map.of())
            .phoneSecondary("")
            .logoUrl("").logoLightUrl("").logoDarkUrl("").faviconUrl("")
            .year(java.time.Year.now().getValue())
            .bank(CompanyTemplateModel.Bank.builder().bankName("").accountName("").accountHolder("")
                .accountNumber("").swift("").iban("").currency("").build())
            .build());

        String html = new PdfTemplateRenderer(identity, new CompanyPlaceholderRenderer(identity), mock(PdfTemplateStorageService.class))
            .renderFromString("""
                <html><body>
                  <p th:text="${company.tin}">x</p>
                  <p>[[${company.address}]]</p>
                  <img th:if="${company.hasLogo()}" th:src="${company.logoLightUrl}"/>
                  <p th:if="${company.bank.present}">Pay to [[${company.bank.accountNumber}]]</p>
                </body></html>
                """, Map.of());

        assertFalse(html.contains("null"), "a blank field must print nothing, not 'null':\n" + html);
        assertFalse(html.contains("<img"), "no logo means no image tag at all");
        assertFalse(html.contains("Pay to"), "no bank account means the payment block is left out");
    }

    @Test
    @DisplayName("a document asks for one logo and gets the widest one that exists")
    void documentLogoPrefersTheFullLockup() {
        CompanyTemplateModel withBoth = CompanyTemplateModel.builder()
            .logoLightUrl("https://api.example.com/icon")
            .logoFullUrl("https://api.example.com/full")
            .build();
        assertEquals("https://api.example.com/full", withBoth.getDocumentLogoUrl(),
            "a letterhead has room for the wordmark, so the lockup wins");
        assertTrue(withBoth.hasFullLogo());

        CompanyTemplateModel iconOnly = CompanyTemplateModel.builder()
            .logoLightUrl("https://api.example.com/icon")
            .logoFullUrl("")
            .build();
        assertEquals("https://api.example.com/icon", iconOnly.getDocumentLogoUrl(),
            "with no lockup uploaded the mark is better than nothing");
        assertFalse(iconOnly.hasFullLogo());
    }

    @Test
    @DisplayName("the brand reaches a CSS block, which is where a template's colours live")
    void brandResolvesInsideStyle() {
        String html = renderer().renderFromString("""
            <html><head><style>
              .band { background: {{companyAccentDark}}; }
              .rule { border-color: {{companyAccent}}; }
              .callout { background: {{companyAccentSoft}}; color: {{companyAccentContrast}}; }
              .card { border-radius: {{companyRadius}}; }
            </style></head>
            <body><div class="band" style="border-top: 2px solid {{companyAccent}}">x</div></body></html>
            """, Map.of());

        /*
         * A brand colour lives in a <style> block and in style="" attributes, and Thymeleaf serves
         * neither: inlining never touches an attribute, and inside <style> th:inline="css" ESCAPES what
         * it writes — `\#0f4a35` for a colour, `\31 0px` for a length, both invalid. So the brand is a
         * plain token, and this asserts it arrives usable rather than merely present.
         */
        assertTrue(html.contains("background: #0f4a35;"), "the darker shade, unescaped:\n" + html);
        assertTrue(html.contains("border-color: #1c7a58;"), "the accent, unescaped");
        assertTrue(html.contains("border-top: 2px solid #1c7a58"),
            "and in a style attribute, which Thymeleaf inlining never reaches");
        assertFalse(html.contains("\\#"), "an escaped colour is not a colour");
        assertTrue(html.contains("10px"), "the radius should be in the sheet");
        assertFalse(html.contains("{{company"), "a token left behind would ship to the reader");
    }

    private PdfTemplateRenderer renderer() {
        CompanyIdentityService identity = mock(CompanyIdentityService.class);
        when(identity.templateModel()).thenReturn(CompanyTemplateModel.builder()
            .name("Test Tours")
            .legalName("Test Tours Ltd")
            .tagline("Go well")
            .email("hello@example.com")
            .phone("+255 700 000 001")
            .phoneSecondary("")
            .address("Arusha, Tanzania")
            .website("example.com")
            .tin("123-456-789")
            .vrn("40-123456-X")
            .registrationNumber("R-1")
            .licenceNumber("L-1")
            .currency("USD")
            .accent("#1c7a58").accentContrast("#ffffff").accentDark("#0f4a35").accentSoft("#e8f5f0")
            .radius("10px").font("\"Geist Variable\", sans-serif")
            .emails(List.of("hello@example.com"))
            .phones(List.of("+255 700 000 001"))
            .socials(Map.of("INSTAGRAM", "https://instagram.com/example"))
            .logoUrl("https://api.example.com/api/public/company/assets/logo-email")
            .logoLightUrl("https://api.example.com/api/public/company/assets/logo-light")
            .logoDarkUrl("https://api.example.com/api/public/company/assets/logo-dark")
            .faviconUrl("https://api.example.com/api/public/company/assets/favicon-light")
            .year(java.time.Year.now().getValue())
            .bank(CompanyTemplateModel.Bank.builder()
                .bankName("CRDB Bank").accountName("Test Tours USD").accountHolder("Test Tours Ltd")
                .accountNumber("0150123456700").swift("CORUTZTZ").iban("").currency("USD").build())
            .build());

        when(identity.variables()).thenReturn(Map.of(
            "companyName", "Test Tours",
            "companyAccent", "#1c7a58",
            "companyAccentContrast", "#ffffff",
            "companyAccentDark", "#0f4a35",
            "companyAccentSoft", "#e8f5f0",
            "companyRadius", "10px"));

        return new PdfTemplateRenderer(identity, new CompanyPlaceholderRenderer(identity), mock(PdfTemplateStorageService.class));
    }
}
