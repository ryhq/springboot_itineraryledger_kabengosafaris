package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.dialect.SpringStandardDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * PdfTemplateRenderer - Renders PDF templates using Thymeleaf with Spring EL
 *
 * Uses SpringTemplateEngine with SpringEL (not OGNL) for expression evaluation,
 * which is compatible with Spring Boot's managed Thymeleaf dependencies.
 *
 * Responsibilities:
 * - Process Thymeleaf templates with provided data
 * - Add utility objects for date/number formatting
 * - Return rendered HTML ready for PDF conversion
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PdfTemplateRenderer {

    private final com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService companyIdentityService;
    private final com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyPlaceholderRenderer companyPlaceholderRenderer;

    private final PdfTemplateStorageService storageService;

    /**
     * Render a PDF template with provided data model
     *
     * @param templateFileName The HTML template file name
     * @param dataModel Map containing variables (e.g., "itinerary" → FullItineraryDTO)
     * @return Rendered HTML string
     */
    public String renderTemplate(String templateFileName, Map<String, Object> dataModel) {
        try {
            // Load template content from storage
            String templateContent = storageService.readTemplateFile(templateFileName);

            if (templateContent == null) {
                throw new RuntimeException("Template file not found: " + templateFileName);
            }

            return renderFromString(templateContent, dataModel);

        } catch (Exception e) {
            log.error("Failed to render PDF template: {}", templateFileName, e);
            throw new RuntimeException("Failed to render PDF template: " + e.getMessage(), e);
        }
    }

    /**
     * Render a template from string content
     *
     * @param templateContent The HTML template content
     * @param dataModel Map containing variables
     * @return Rendered HTML string
     */
    public String renderFromString(String templateContent, Map<String, Object> dataModel) {
        try {
            // Extract DOCTYPE if present (Thymeleaf HTML mode strips it)
            String doctype = "";
            String contentToProcess = templateContent;
            if (templateContent.trim().toLowerCase().startsWith("<!doctype")) {
                int endIndex = templateContent.indexOf('>');
                if (endIndex != -1) {
                    doctype = templateContent.substring(0, endIndex + 1).trim() + "\n";
                    contentToProcess = templateContent.substring(endIndex + 1).trim();
                }
            }

            // Create string template resolver
            StringTemplateResolver resolver = new StringTemplateResolver();
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setCacheable(false); // Templates might change frequently

            // Create Spring template engine (uses SpringEL instead of OGNL)
            SpringTemplateEngine templateEngine = new SpringTemplateEngine();
            templateEngine.setTemplateResolver(resolver);
            templateEngine.setEnableSpringELCompiler(true);

            // Set the dialect to use SpringEL
            SpringStandardDialect dialect = new SpringStandardDialect();
            templateEngine.setDialect(dialect);

            // Add Java 8 Time dialect for #temporals support
            templateEngine.addDialect(new Java8TimeDialect());

            // Create context with data model
            Context context = new Context();

            // Add all data model entries
            if (dataModel != null) {
                dataModel.forEach(context::setVariable);
            }

            // Add utility objects
            addUtilityObjects(context);

            // Process template
            String renderedHtml = templateEngine.process(contentToProcess, context);

            /*
             * Then the company's own {{tokens}}, over the finished HTML.
             *
             * Two mechanisms on purpose. ${company.x} reads the model, which is right for text and for
             * th: attributes — but Thymeleaf's inlining does not touch a style="" attribute at all, and
             * inside a <style> block th:inline="css" ESCAPES what it substitutes: a colour came out as
             * `\#0f4a35` and a length as `\31 0px`, neither of which is valid CSS. A brand colour lives
             * in exactly those two places, so it is a plain token replaced here — the same
             * {{companyAccent}} an email template uses, resolved by the same component.
             */
            renderedHtml = companyPlaceholderRenderer.apply(renderedHtml);

            // Prepend DOCTYPE back to rendered HTML
            if (!doctype.isEmpty()) {
                renderedHtml = doctype + renderedHtml;
            }

            log.debug("PDF template rendered successfully");
            return renderedHtml;

        } catch (Exception e) {
            log.error("Failed to render PDF template from string", e);
            throw new RuntimeException("Failed to render PDF template: " + e.getMessage(), e);
        }
    }

    /**
     * Add utility objects to the Thymeleaf context
     * These are available in templates as ${util.methodName()}
     */
    private void addUtilityObjects(Context context) {
        /*
         * Who is sending this document.
         *
         * Every PDF prints the company's name, address and TIN, and until now it printed them
         * because the words were in the file — 17 of the 19 shipped PDF templates carried them.
         * Added HERE because both render paths pass through this method, so a caller cannot forget;
         * a data model that supplies its own `company` still wins, for previews.
         */
        if (!context.containsVariable("company")) {
            context.setVariable("company", companyIdentityService.templateModel());
        }

        // Date formatter utility
        context.setVariable("dateUtil", new DateUtil());

        // Number formatter utility
        context.setVariable("numberUtil", new NumberUtil());

        // Current date/time
        context.setVariable("now", LocalDateTime.now());
        context.setVariable("today", LocalDate.now());
    }

    /**
     * Date formatting utility class for templates
     * Usage in template: ${dateUtil.format(date, 'dd MMM yyyy')}
     */
    public static class DateUtil {

        public String format(LocalDateTime dateTime, String pattern) {
            if (dateTime == null) return "";
            try {
                return dateTime.format(DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                return dateTime.toString();
            }
        }

        public String format(LocalDate date, String pattern) {
            if (date == null) return "";
            try {
                return date.format(DateTimeFormatter.ofPattern(pattern));
            } catch (Exception e) {
                return date.toString();
            }
        }

        public String formatDate(LocalDateTime dateTime) {
            return format(dateTime, "dd MMM yyyy");
        }

        public String formatDateTime(LocalDateTime dateTime) {
            return format(dateTime, "dd MMM yyyy HH:mm");
        }

        public String formatTime(LocalDateTime dateTime) {
            return format(dateTime, "HH:mm");
        }
    }

    /**
     * Number formatting utility class for templates
     * Usage in template: ${numberUtil.currency(amount, 'USD')}
     */
    public static class NumberUtil {

        public String format(Number number, int decimals) {
            if (number == null) return "";
            return String.format("%,." + decimals + "f", number.doubleValue());
        }

        public String formatInt(Number number) {
            if (number == null) return "";
            return String.format("%,d", number.longValue());
        }

        public String currency(Number amount, String currencyCode) {
            if (amount == null) return "";
            return currencyCode + " " + String.format("%,.2f", amount.doubleValue());
        }

        public String percentage(Number value) {
            if (value == null) return "";
            return String.format("%.1f%%", value.doubleValue());
        }
    }
}
