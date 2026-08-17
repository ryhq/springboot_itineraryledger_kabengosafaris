package com.itineraryledger.kabengosafaris.Response;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

/**
 * Turning a STORED content type and filename into headers that cannot fail.
 *
 * A stored mime type is not ours: it arrives on an uploaded file or, worse, straight off a mail
 * header, and a real Content-Type carries parameters. One attachment in this system is stored as
 *
 *   application/pdf; name=Sunshine Azure Zanzibar Beach Hotel- Availability Chart - as of ….pdf
 *
 * — an unquoted parameter containing spaces. {@code MediaType.parseMediaType} throws
 * {@code InvalidMediaTypeException} on that, which extends IllegalArgumentException, which the
 * global handler maps to 400. So downloading that one attachment answered "400 Bad Request"
 * while the one beside it worked, and nothing in the log looked like a bug in downloading.
 *
 * The type is the part before the first ';' — the parameters describe the file, not the format,
 * and are no business of ours when we are the ones serving the bytes.
 */
public final class ContentTypes {

    private ContentTypes() {}

    /**
     * The safest MediaType for a stored value: the bare type, or octet-stream.
     *
     * Never throws. A download that serves the wrong-but-generic type is a file the browser
     * offers to save; a download that throws is a 400 nobody can act on.
     */
    public static MediaType safe(String storedMimeType) {
        if (storedMimeType == null || storedMimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        String bare = storedMimeType.split(";")[0].trim();
        if (bare.isEmpty()) return MediaType.APPLICATION_OCTET_STREAM;
        try {
            return MediaType.parseMediaType(bare);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * A Content-Disposition value that cannot corrupt the response.
     *
     * A stored filename can hold a quote (which would end the quoted string early) or a CR/LF
     * (which is header injection); both are stripped. Non-ASCII names also get the RFC 5987
     * {@code filename*} form, so "Réservation.pdf" saves under its own name rather than as
     * mojibake.
     */
    public static String attachment(String fileName) {
        String name = fileName == null || fileName.isBlank() ? "download" : fileName;
        // no quotes, no line breaks, no path separators
        String cleaned = name.replaceAll("[\\r\\n\"\\\\/]+", "_").trim();
        String ascii = cleaned.replaceAll("[^\\x20-\\x7E]", "_");
        String encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    /** The same, for a file meant to be shown in the browser rather than saved. */
    public static String inline(String fileName) {
        return attachment(fileName).replaceFirst("^attachment", "inline");
    }
}
