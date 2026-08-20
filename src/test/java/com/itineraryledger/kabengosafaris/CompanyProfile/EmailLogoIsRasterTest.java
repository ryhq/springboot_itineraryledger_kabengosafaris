package com.itineraryledger.kabengosafaris.CompanyProfile;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That the logo an email points at is something a mail client can actually draw.
 *
 * Gmail and Outlook do not render SVG — they show a broken-image box — so a welcome email whose
 * header pointed at the company's vector mark opened with exactly that box above the greeting. The
 * email slot refuses an SVG upload for this reason, but a company that has only uploaded vectors
 * still needs a logo in its mail, so the endpoint converts one on the way out.
 *
 * Converting is a claim about a library, not a design decision, so it is executed rather than
 * assumed: transcode a real SVG and check the first eight bytes are a PNG signature.
 */
class EmailLogoIsRasterTest {

    private static final String SVG =
        "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 240 120\" width=\"240\" height=\"120\">"
            + "<rect width=\"240\" height=\"120\" fill=\"#014225\"/>"
            + "<circle cx=\"60\" cy=\"60\" r=\"40\" fill=\"#f0c14b\"/></svg>";

    @Test
    @DisplayName("an SVG logo transcodes to a real PNG")
    void svgBecomesPng() throws Exception {
        PNGTranscoder transcoder = new PNGTranscoder();
        transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, 600f);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transcoder.transcode(
            new TranscoderInput(new ByteArrayInputStream(SVG.getBytes(StandardCharsets.UTF_8))),
            new TranscoderOutput(out));
        byte[] png = out.toByteArray();

        assertTrue(png.length > 500, "the conversion produced only " + png.length + " bytes");
        byte[] signature = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A };
        for (int i = 0; i < signature.length; i++) {
            assertEquals(signature[i], png[i], "byte " + i + " is not the PNG signature");
        }
    }

    @Test
    @DisplayName("no shipped email or signature forces the logo into a square")
    void logosKeepTheirShape() throws Exception {
        /*
         * The email slot can now hold either the icon (square) or the full lockup (wide), because it
         * borrows whatever the company uploaded. A fixed width AND height would squash the lockup
         * into the icon's proportions — the sort of thing nobody notices until a client replies
         * asking why the logo looks wrong.
         */
        List<Path> roots = List.of(
            Path.of("src/main/resources/templates/email-templates"),
            Path.of("src/main/resources/templates/email-signatures"));

        for (Path root : roots) {
            if (!Files.isDirectory(root)) continue;
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".html")).toList()) {
                    String html = Files.readString(file);
                    int at = html.indexOf("{{companyLogoUrl}}");
                    while (at >= 0) {
                        int open = html.lastIndexOf('<', at);
                        int close = html.indexOf('>', at);
                        String tag = html.substring(open, close + 1);
                        assertFalse(tag.matches("(?s).*\\sheight=\"\\d+\".*"),
                            file.getFileName() + " pins the logo's height: " + tag
                                + " — use width plus height:auto, or a wide lockup is squashed");
                        at = html.indexOf("{{companyLogoUrl}}", close);
                    }
                }
            }
        }
    }
}
