package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.CompanyProfile.DTOs.CompanyTemplateModel;
import com.itineraryledger.kabengosafaris.CompanyProfile.Services.CompanyIdentityService;
import com.itineraryledger.kabengosafaris.PdfDocument.Services.PdfTemplateValidationService;

/**
 * The build fails if one company's details are typed into a shipped template again.
 *
 * This exists because the templates carried "Kabengo Safaris", an Arusha postal box and two Tanzanian
 * phone numbers in 290 places, which is fine for exactly one company and wrong for the second. The
 * details now come from the company profile at render time; a literal that reappears is a regression
 * that would be discovered by a client of the WRONG company reading their own invoice, so it is
 * discovered here instead.
 *
 * Adding a company: nothing to do. Adding a template: use the variables listed in the failure message.
 */
class CompanyLiteralsInTemplatesTest {

    private static final Path RESOURCES = Paths.get("src/main/resources");

    /** Where a company's own details must never be hardcoded. */
    private static final List<String> SCANNED = List.of(
        "templates/email-templates", "templates/email-signatures", "templates/pdf-templates",
        "schemas/email-events");

    /**
     * The literals. Deliberately specific: this catches a company's details, not any mention of a
     * place — "Arusha" alone is a legitimate safari start point in sample data, "Arusha, Tanzania" as
     * a template's address block is not.
     */
    private static final Map<Pattern, String> FORBIDDEN = new LinkedHashMap<>();
    static {
        FORBIDDEN.put(Pattern.compile("Kabengo", Pattern.CASE_INSENSITIVE), "{{companyName}} / ${company.name}");
        FORBIDDEN.put(Pattern.compile("Jatelo", Pattern.CASE_INSENSITIVE), "{{companyName}} / ${company.name}");
        FORBIDDEN.put(Pattern.compile("[A-Za-z0-9._%-]+@(?!example\\.)[A-Za-z0-9.-]+\\.(com|co\\.tz|org|net)"),
            "{{companyEmail}} / ${company.email}");
        FORBIDDEN.put(Pattern.compile("www\\.[A-Za-z0-9-]+\\.(com|co\\.tz)"), "{{companyWebsite}} / ${company.website}");
        FORBIDDEN.put(Pattern.compile("\\+255\\s?\\d{3}\\s?\\d{3}\\s?\\d{3}"), "{{companyPhone}} / ${company.phone}");
        FORBIDDEN.put(Pattern.compile("P\\.?\\s?O\\.?\\s?Box\\s+\\d+"), "{{companyAddress}} / ${company.address}");
        FORBIDDEN.put(Pattern.compile("Arusha,\\s*Tanzania"), "{{companyAddress}} / ${company.address}");
        FORBIDDEN.put(Pattern.compile("(&copy;|©)\\s*20\\d\\d"), "{{currentYear}} / ${company.year}");
    }

    @Test
    @DisplayName("no shipped template names a company, an address, a number or a year of its own")
    void noCompanyLiterals() throws IOException {
        List<String> offences = new ArrayList<>();

        for (Path file : scanned()) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                for (Map.Entry<Pattern, String> rule : FORBIDDEN.entrySet()) {
                    Matcher m = rule.getKey().matcher(line);
                    if (m.find()) {
                        offences.add(String.format("%s:%d  '%s'  ->  use %s",
                            RESOURCES.relativize(file), i + 1, m.group(), rule.getValue()));
                    }
                }
            }
        }

        assertTrue(offences.isEmpty(), () -> String.format("""
            %d company literal(s) found in shipped templates.

            These files are seeded into EVERY company's installation, so a company's own name, email,
            website, phone, postal box or a fixed year cannot be typed into them. Replace it with the
            variable named beside each hit:

            %s

            Email templates and signatures use {{name}} and can hide an optional block with
            {{#name}}...{{/name}}. PDF templates use ${company.name} in th:text/th:src or [[${company.name}]]
            inline. The full list of variables is in CompanyIdentityService.Snapshot#variables and
            CompanyTemplateModel.
            """, offences.size(), String.join("\n", offences)));
    }

    @Test
    @DisplayName("every {{company…}} placeholder a template uses is one the renderer actually supplies")
    void everyCompanyPlaceholderExists() throws IOException {
        Set<String> supplied = new HashSet<>(CompanyIdentityService.Snapshot.empty("x").variables().keySet());
        supplied.add("currentYear");   // added per render, not part of the cached snapshot

        Pattern placeholder = Pattern.compile("\\{\\{[#/]?(company[A-Za-z]*|currentYear)\\}\\}");
        List<String> unknown = new ArrayList<>();

        for (Path file : scanned()) {
            if (!file.toString().contains("email-")) continue;
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = placeholder.matcher(lines.get(i));
                while (m.find()) {
                    if (!supplied.contains(m.group(1))) {
                        unknown.add(RESOURCES.relativize(file) + ":" + (i + 1) + "  " + m.group());
                    }
                }
            }
        }

        assertTrue(unknown.isEmpty(), () -> "placeholders nothing fills, which ship to the reader as "
            + "literal braces:\n" + String.join("\n", unknown)
            + "\n\navailable: " + new TreeSet<>(supplied));
    }

    @Test
    @DisplayName("every ${company.x} a PDF template reads exists on the model")
    void everyCompanyExpressionResolves() throws IOException {
        Pattern expression = Pattern.compile("\\$\\{company\\.([A-Za-z]+)");
        List<String> unknown = new ArrayList<>();

        for (Path file : scanned()) {
            if (!file.toString().contains("pdf-templates")) continue;
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                Matcher m = expression.matcher(lines.get(i));
                while (m.find()) {
                    String property = m.group(1);
                    if (!hasProperty(CompanyTemplateModel.class, property)) {
                        unknown.add(RESOURCES.relativize(file) + ":" + (i + 1) + "  ${company." + property + "}");
                    }
                }
            }
        }

        assertTrue(unknown.isEmpty(), () -> "a PDF template reads something the company model does not "
            + "have — Thymeleaf would throw mid-render, so no document at all would come out:\n"
            + String.join("\n", unknown));
    }

    @Test
    @DisplayName("the rewritten PDF templates are still valid documents")
    void pdfTemplatesStillValidate() throws IOException {
        PdfTemplateValidationService validator = new PdfTemplateValidationService();
        List<String> broken = new ArrayList<>();

        try (Stream<Path> files = Files.list(RESOURCES.resolve("templates/pdf-templates"))) {
            for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                try {
                    validator.validateForPdf(Files.readString(file), file.getFileName().toString());
                } catch (Exception e) {
                    broken.add(file.getFileName() + ": " + e.getMessage());
                }
            }
        }

        assertTrue(broken.isEmpty(), () -> "the substitutions left markup OpenHTMLtoPDF will refuse:\n"
            + String.join("\n", broken));
    }

    // ------------------------------------------------------------------ helpers

    private List<Path> scanned() throws IOException {
        List<Path> files = new ArrayList<>();
        for (String dir : SCANNED) {
            Path path = RESOURCES.resolve(dir);
            assertTrue(Files.isDirectory(path), "scanned directory is missing: " + path
                + " — if it moved, this guard is silently checking nothing");
            try (Stream<Path> stream = Files.walk(path)) {
                stream.filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".html") || f.toString().endsWith(".json"))
                    .forEach(files::add);
            }
        }
        assertFalse(files.isEmpty(), "nothing was scanned");
        return files;
    }

    private boolean hasProperty(Class<?> type, String property) {
        String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (String candidate : List.of("get" + suffix, "is" + suffix, property, property + "()")) {
            try {
                type.getMethod(candidate.replace("()", ""));
                return true;
            } catch (NoSuchMethodException ignored) { /* try the next shape */ }
        }
        return false;
    }
}
