package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That each email logo is used against the background it was uploaded for.
 *
 * The shipped emails paint their header band in the dark accent. Pointing that band at the
 * light-background copy is how a welcome email opened with a faint green outline where the mark
 * should be — a logo nobody can see, on the first screen a new customer ever gets.
 *
 * The failure is invisible in code review because both variables are plausible URLs and either one
 * renders *something*. So it is checked structurally: whatever sits inside a header band asks for
 * the dark-header slot, and whatever sits on white asks for the other.
 *
 * Naming, since it is the trap underneath: a slot is named for the BACKGROUND it is for, never the
 * ink it is drawn in. The two readings are exact opposites.
 */
class EmailLogoMatchesItsBackgroundTest {

    private static final Path EMAILS = Path.of("src/main/resources/templates/email-templates");
    private static final Path SIGNATURES = Path.of("src/main/resources/templates/email-signatures");

    private List<Path> htmlIn(Path root) throws IOException {
        if (!Files.isDirectory(root)) return List.of();
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(f -> f.toString().endsWith(".html")).toList();
        }
    }

    @Test
    @DisplayName("a logo inside the coloured header band uses the dark-header slot")
    void headerBandsUseTheDarkSlot() throws IOException {
        List<String> wrong = new ArrayList<>();

        for (Path file : htmlIn(EMAILS)) {
            String html = Files.readString(file);
            int at = html.indexOf("{{companyLogoUrl}}");
            while (at >= 0) {
                /*
                 * Is it inside the header band? The band is a div that opens before the image and
                 * has not closed yet — near enough to look back a few hundred characters for the
                 * class, which is how these templates are all built.
                 */
                String before = html.substring(Math.max(0, at - 600), at);
                if (before.contains("email-header")) {
                    wrong.add(file.getFileName() + " puts the light-background logo on the dark band");
                }
                at = html.indexOf("{{companyLogoUrl}}", at + 1);
            }
        }

        assertTrue(wrong.isEmpty(),
            "Use {{companyLogoEmailDarkUrl}} inside a header band — the light-background copy "
                + "disappears into the dark accent: " + wrong);
    }

    @Test
    @DisplayName("a signature sits on white, so it uses the light-background slot")
    void signaturesUseTheLightSlot() throws IOException {
        List<String> wrong = new ArrayList<>();

        for (Path file : htmlIn(SIGNATURES)) {
            String html = Files.readString(file);
            if (html.contains("{{companyLogoEmailDarkUrl}}")) {
                wrong.add(file.getFileName().toString());
            }
        }

        assertTrue(wrong.isEmpty(),
            "A signature is quoted into a reply on a white background, so the light-ink mark would "
                + "vanish. Use {{companyLogoUrl}}: " + wrong);
    }

    @Test
    @DisplayName("no email or signature carries a hardcoded brand colour in an icon data URI")
    void iconsFollowTheBrand() throws IOException {
        List<String> offenders = new ArrayList<>();
        List<Path> all = new ArrayList<>(htmlIn(EMAILS));
        all.addAll(htmlIn(SIGNATURES));

        for (Path file : all) {
            String html = Files.readString(file);
            /*
             * A colour inside a data: URI is written %23rrggbb, which slipped past every check that
             * looked for '#'. The signature icons stayed a bronze nobody chose for six templates.
             */
            java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("%23([0-9a-fA-F]{6})").matcher(html);
            while (m.find()) {
                offenders.add(file.getFileName() + " -> %23" + m.group(1));
            }
        }

        assertTrue(offenders.isEmpty(),
            "Use %23{{companyAccentBare}} so the icon follows the brand: " + offenders);
    }
}
