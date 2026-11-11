package com.itineraryledger.kabengosafaris.EmailAccount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.HtmlToText;

/**
 * The one-line preview under a subject in the mailbox list is the first thing anybody reads of a
 * message, and it must be the message's own first sentence.
 *
 * <p>Written from a live fault. Two copies of {@code extractSnippet} reduced an HTML body with
 * {@code html.replaceAll("<[^>]+>", "")}. That removes the TAGS and keeps everything between
 * them, so the contents of {@code <title>} and the whole {@code <style>} block became the text:
 * every templated message in the Sent list previewed as
 * "Welcome to Kabengo Safaris /* Reset &amp; Base *&#47; body { margin: 0; padding: 0;
 * font-family: 'Segoe UI'...". 975 sent messages read that way, and the fix was already in the
 * codebase — {@link HtmlToText} drops head, style and script before anything else and had no
 * caller on either path.
 *
 * <p>So this test guards the behaviour AND the shortcut that caused it: a naive tag-strip must not
 * reappear in a file that produces a snippet. The behaviour test alone would pass against a third
 * copy of the bug written somewhere new.
 */
class APreviewIsProseNotStylesheetTest {

    private static final Path MAIN = Paths.get("src/main/java/com/itineraryledger/kabengosafaris");

    /** `replaceAll("<[^>]+>", "")` and its close relatives, however the quotes are written. */
    private static final Pattern NAIVE_TAG_STRIP =
        Pattern.compile("replaceAll\\s*\\(\\s*\"<\\[\\^>\\]\\+>\"");

    private static final String TEMPLATE_SHAPED_BODY = """
        <html>
          <head>
            <title>Welcome to Kabengo Safaris</title>
            <style>
              /* Reset & Base */
              body { margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, sans-serif; }
            </style>
          </head>
          <body>
            <!-- Styles are inline on purpose: a style block here would leave the letter
                 unstyled in Gmail and Outlook. -->
            <!--[if mso]>
              <table role="presentation"><tr><td>
            <![endif]-->
            <p>Hello Beatrice,</p>
            <p>Could you kindly share your correct bank account details?</p>
          </body>
        </html>
        """;

    @Test
    @DisplayName("the shared converter keeps the prose and discards the stylesheet")
    void theConverterKeepsProseAndDropsCss() {
        String text = HtmlToText.convert(TEMPLATE_SHAPED_BODY);

        assertFalse(text.contains("margin: 0"), "a CSS declaration reached the reader:\n" + text);
        assertFalse(text.contains("font-family"), "a CSS property reached the reader:\n" + text);
        assertFalse(text.contains("Reset & Base"), "a CSS comment reached the reader:\n" + text);
        assertFalse(text.contains("Welcome to Kabengo Safaris"),
            "the <title> is chrome, not body text:\n" + text);

        assertFalse(text.contains("unstyled in Gmail"),
            "a note we wrote to ourselves inside an HTML comment reached the reader:\n" + text);
        assertFalse(text.contains("endif"),
            "the tail of an Outlook conditional comment reached the reader:\n" + text);
        assertFalse(text.contains("-->"),
            "a comment terminator reached the reader:\n" + text);

        assertTrue(text.contains("Hello Beatrice,"), "the greeting was lost:\n" + text);
        assertTrue(text.startsWith("Hello"),
            "the preview must open on the message's first sentence, not on anything before it:\n" + text);
    }

    @Test
    @DisplayName("no file that builds a snippet strips tags by hand")
    void nobodyStripsTagsByHand() throws IOException {
        List<String> offenders = new ArrayList<>();

        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                // HtmlToText itself is the one place allowed to work at the regex level.
                if (file.getFileName().toString().equals("HtmlToText.java")) continue;
                if (!source.contains("nippet")) continue;
                if (NAIVE_TAG_STRIP.matcher(withoutComments(source)).find()) {
                    offenders.add(MAIN.relativize(file).toString());
                }
            }
        }

        assertEquals(List.of(), offenders,
            "These build a preview by stripping tags with a regex, which keeps the contents of "
                + "<style>, <title> and <script>. Call HtmlToText.convert(html) instead.");
    }

    /**
     * Code only. A comment is allowed to quote the broken pattern — the one above this test's own
     * fix does exactly that, to say what not to write, and a scanner that cannot tell the
     * difference teaches people to delete the explanation instead of the mistake.
     */
    private static String withoutComments(String source) {
        return source
            .replaceAll("(?s)/\\*.*?\\*/", " ")
            .replaceAll("(?m)//.*$", " ");
    }
}
