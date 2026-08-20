package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That nothing takes a template and mails it without resolving the company first.
 *
 * The template files are written entirely in company variables now — the name, the accent colour,
 * the logo — so a code path that substitutes its own variables and stops has not sent a plain
 * version of the email. It has sent a broken one: "Thank you for registering with !" above a
 * layout with no colours, because the accent lives inside the stylesheet and a mail client that
 * meets `{{` in CSS discards the block rather than guessing.
 *
 * That is precisely what "Send me a test" did — the one feature whose entire purpose is to show
 * somebody what their customer will receive. It had its own private substitution loop, so the pass
 * added to the real renderer never ran there.
 *
 * A source-level check, because the alternative is standing up mail. It is deliberately narrow:
 * anything that both READS a template file and SENDS or RENDERS must mention the shared pass.
 */
class CompanyPassOnEverySendTest {

    private static final Path JAVA = Paths.get("src/main/java");
    private static final String PASS = "companyPlaceholderRenderer.apply(";

    /** Reads a stored template, and either sends it or hands back finished output. */
    private boolean rendersForDelivery(String source, String fileName) {
        boolean readsATemplate = source.contains("readTemplateFile(");
        boolean delivers = source.contains("sendHtmlEmail")
            || source.contains("sendHtmlEmailWithAttachment")
            || fileName.endsWith("Renderer.java");
        return readsATemplate && delivers;
    }

    @Test
    @DisplayName("every path that renders a stored template for delivery applies the company pass")
    void everyDeliveryPathResolvesTheCompany() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(JAVA)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                String name = file.getFileName().toString();
                if (!rendersForDelivery(source, name)) continue;
                if (!source.contains(PASS)) {
                    offenders.add(JAVA.relativize(file).toString());
                }
            }
        }

        assertTrue(offenders.isEmpty(),
            "These render a stored template for delivery but never resolve the company's variables, "
                + "so {{companyName}} and {{companyAccent}} reach the recipient unsubstituted: "
                + offenders + ". Apply CompanyPlaceholderRenderer to the finished output.");
    }
}
