package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Exceptions.PdfTemplateValidationException;
import com.itineraryledger.kabengosafaris.PdfDocument.Exceptions.PdfTemplateValidationException.ValidationError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfTemplateValidationService - Validates HTML templates before PDF generation
 *
 * OpenHTMLToPDF supports HTML5 with some XHTML-like requirements:
 * - Well-formed XML structure (properly closed tags)
 * - Valid entity references
 * - SVG support via Batik
 *
 * This service validates templates and provides detailed, user-friendly error messages.
 *
 * Common issues detected:
 * - XML declaration inside document body (embedded SVG with <?xml ...?>)
 * - Missing doctype
 * - Unclosed tags
 * - Self-closing tag issues
 * - Character encoding problems
 */
@Service
@Slf4j
public class PdfTemplateValidationService {

    // Patterns for common issues
    private static final Pattern EMBEDDED_XML_DECLARATION = Pattern.compile(
            "<\\?xml\\s+version[^?]*\\?>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XHTML_DOCTYPE = Pattern.compile(
            "<!DOCTYPE\\s+html\\s+PUBLIC\\s+\"-//W3C//DTD\\s+XHTML",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern HTML5_DOCTYPE = Pattern.compile(
            "<!doctype\\s+html\\s*>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XHTML_NAMESPACE = Pattern.compile(
            "xmlns\\s*=\\s*[\"']http://www\\.w3\\.org/1999/xhtml[\"']",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNCLOSED_BR = Pattern.compile(
            "<br\\s*>(?!</br>)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNCLOSED_HR = Pattern.compile(
            "<hr\\s*>(?!</hr>)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNCLOSED_IMG = Pattern.compile(
            "<img\\s+[^>]*[^/]>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNCLOSED_INPUT = Pattern.compile(
            "<input\\s+[^>]*[^/]>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNCLOSED_META = Pattern.compile(
            "<meta\\s+[^>]*[^/]>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNCLOSED_LINK = Pattern.compile(
            "<link\\s+[^>]*[^/]>",
            Pattern.CASE_INSENSITIVE
    );

    // Pattern to find unescaped ampersands (& not followed by # or valid entity name ending in ;)
    private static final Pattern UNESCAPED_AMPERSAND = Pattern.compile(
            "&(?![#a-zA-Z][a-zA-Z0-9]*;)"
    );

    // Common HTML entities that need to be converted to numeric equivalents
    private static final Map<String, String> HTML_ENTITIES = new HashMap<>();
    static {
        // Common HTML entities -> numeric equivalents
        HTML_ENTITIES.put("&nbsp;", "&#160;");
        HTML_ENTITIES.put("&copy;", "&#169;");
        HTML_ENTITIES.put("&reg;", "&#174;");
        HTML_ENTITIES.put("&trade;", "&#8482;");
        HTML_ENTITIES.put("&mdash;", "&#8212;");
        HTML_ENTITIES.put("&ndash;", "&#8211;");
        HTML_ENTITIES.put("&lsquo;", "&#8216;");
        HTML_ENTITIES.put("&rsquo;", "&#8217;");
        HTML_ENTITIES.put("&ldquo;", "&#8220;");
        HTML_ENTITIES.put("&rdquo;", "&#8221;");
        HTML_ENTITIES.put("&bull;", "&#8226;");
        HTML_ENTITIES.put("&hellip;", "&#8230;");
        HTML_ENTITIES.put("&euro;", "&#8364;");
        HTML_ENTITIES.put("&pound;", "&#163;");
        HTML_ENTITIES.put("&yen;", "&#165;");
        HTML_ENTITIES.put("&cent;", "&#162;");
        HTML_ENTITIES.put("&deg;", "&#176;");
        HTML_ENTITIES.put("&plusmn;", "&#177;");
        HTML_ENTITIES.put("&times;", "&#215;");
        HTML_ENTITIES.put("&divide;", "&#247;");
        HTML_ENTITIES.put("&frac12;", "&#189;");
        HTML_ENTITIES.put("&frac14;", "&#188;");
        HTML_ENTITIES.put("&frac34;", "&#190;");
        HTML_ENTITIES.put("&rarr;", "&#8594;");
        HTML_ENTITIES.put("&larr;", "&#8592;");
        HTML_ENTITIES.put("&uarr;", "&#8593;");
        HTML_ENTITIES.put("&darr;", "&#8595;");
    }

    /**
     * Validate rendered HTML for PDF generation compatibility
     *
     * OpenHTMLToPDF accepts both HTML5 and XHTML doctypes but requires
     * well-formed XML structure. This method validates the HTML and throws
     * an exception with detailed errors if validation fails.
     *
     * @param html The rendered HTML content
     * @param templateName Optional template name for error reporting
     * @throws PdfTemplateValidationException if validation fails
     */
    public void validateForPdf(String html, String templateName) {
        List<ValidationError> errors = new ArrayList<>();

        if (html == null || html.isBlank()) {
            throw new PdfTemplateValidationException(
                    "Template content is empty",
                    templateName,
                    List.of(ValidationError.of(
                            "EMPTY_TEMPLATE",
                            "The rendered template is empty",
                            "Ensure the template file exists and contains valid HTML"
                    ))
            );
        }

        // Check for embedded XML declarations (common issue with SVG)
        checkEmbeddedXmlDeclarations(html, errors);

        // Check doctype - now accepts both HTML5 and XHTML
        checkDoctype(html, errors);

        // Note: XHTML namespace is not strictly required for OpenHTMLToPDF
        // but is recommended for compatibility
        // checkXhtmlNamespace(html, errors);  // Disabled for OpenHTMLToPDF

        // Check for unclosed self-closing tags
        checkUnclosedTags(html, errors);

        // Note: Unescaped ampersands will be handled by normalizeHtmlEntities()
        // We don't need to fail validation for them anymore
        // checkUnescapedAmpersands(html, errors);  // Disabled - handled by normalization

        // If we found pre-validation issues, throw them now
        if (!errors.isEmpty()) {
            throw new PdfTemplateValidationException(
                    buildErrorSummary(errors),
                    templateName,
                    errors
            );
        }

        // Try XML parsing for comprehensive validation
        validateXmlStructure(html, templateName, errors);
    }

    /**
     * Quick validation without throwing exceptions - returns validation result
     *
     * @param html The HTML content to validate
     * @return ValidationResult with success status and any errors found
     */
    public ValidationResult validate(String html, String templateName) {
        try {
            validateForPdf(html, templateName);
            return new ValidationResult(true, List.of(), "Template is valid for PDF generation");
        } catch (PdfTemplateValidationException e) {
            return new ValidationResult(false, e.getErrors(), e.getMessage());
        }
    }

    /**
     * Check for XML declarations embedded in the document body
     * This is a common issue when SVG files with XML declarations are inlined
     */
    private void checkEmbeddedXmlDeclarations(String html, List<ValidationError> errors) {
        Matcher matcher = EMBEDDED_XML_DECLARATION.matcher(html);
        int searchStart = 0;

        // Skip the document-level XML declaration if present at the beginning
        if (html.trim().startsWith("<?xml")) {
            int firstEnd = html.indexOf("?>");
            if (firstEnd != -1) {
                searchStart = firstEnd + 2;
            }
        }

        while (matcher.find(searchStart)) {
            int lineNumber = getLineNumber(html, matcher.start());
            String context = getContextAround(html, matcher.start(), 50);

            errors.add(new ValidationError(
                    "EMBEDDED_XML_DECLARATION",
                    "XML declaration found inside document body at line " + lineNumber,
                    lineNumber,
                    null,
                    "Remove the <?xml version=\"1.0\"...?> declaration from embedded SVG elements. " +
                            "When embedding SVG inline in HTML, the XML declaration should be removed.",
                    context
            ));

            searchStart = matcher.end();
        }
    }

    /**
     * Check for proper doctype declaration
     * OpenHTMLToPDF accepts both HTML5 and XHTML doctypes
     */
    private void checkDoctype(String html, List<ValidationError> errors) {
        boolean hasXhtmlDoctype = XHTML_DOCTYPE.matcher(html).find();
        boolean hasHtml5Doctype = HTML5_DOCTYPE.matcher(html).find();

        // OpenHTMLToPDF accepts both HTML5 and XHTML doctypes
        if (!hasXhtmlDoctype && !hasHtml5Doctype) {
            errors.add(ValidationError.of(
                    "MISSING_DOCTYPE",
                    "No DOCTYPE declaration found. A doctype is recommended for consistent rendering.",
                    "Add '<!DOCTYPE html>' at the beginning of the template for HTML5, or use XHTML doctype:\n" +
                            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" " +
                            "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            ));
        }
        // HTML5 doctype is now valid - no error needed
    }

    /**
     * Check for XHTML namespace in html tag (optional for OpenHTMLToPDF)
     */
    @SuppressWarnings("unused")
    private void checkXhtmlNamespace(String html, List<ValidationError> errors) {
        if (!XHTML_NAMESPACE.matcher(html).find()) {
            errors.add(ValidationError.of(
                    "MISSING_XHTML_NAMESPACE",
                    "XHTML namespace not found in <html> tag (recommended but not required)",
                    "For better compatibility, add xmlns=\"http://www.w3.org/1999/xhtml\" to your <html> tag:\n" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
            ));
        }
    }

    /**
     * Check for unclosed self-closing tags (well-formed XML requires explicit closing)
     */
    private void checkUnclosedTags(String html, List<ValidationError> errors) {
        checkUnclosedTag(html, UNCLOSED_BR, "<br>", "<br />", errors);
        checkUnclosedTag(html, UNCLOSED_HR, "<hr>", "<hr />", errors);
        checkUnclosedTag(html, UNCLOSED_IMG, "<img>", "<img ... />", errors);
        checkUnclosedTag(html, UNCLOSED_INPUT, "<input>", "<input ... />", errors);
        checkUnclosedTag(html, UNCLOSED_META, "<meta>", "<meta ... />", errors);
        checkUnclosedTag(html, UNCLOSED_LINK, "<link>", "<link ... />", errors);
    }

    private void checkUnclosedTag(String html, Pattern pattern, String tagName,
                                   String correctForm, List<ValidationError> errors) {
        Matcher matcher = pattern.matcher(html);
        int count = 0;
        int firstLine = -1;

        while (matcher.find()) {
            count++;
            if (firstLine == -1) {
                firstLine = getLineNumber(html, matcher.start());
            }
        }

        if (count > 0) {
            errors.add(ValidationError.ofWithLine(
                    "UNCLOSED_TAG",
                    String.format("Found %d unclosed %s tag(s). Self-closing tags are required.", count, tagName),
                    firstLine,
                    String.format("Change %s to %s (self-closing with space before /)", tagName, correctForm)
            ));
        }
    }

    /**
     * Check for unescaped ampersands that will cause XML parsing errors.
     * Note: This is now handled by normalizeHtmlEntities() during PDF generation.
     */
    @SuppressWarnings("unused")
    private void checkUnescapedAmpersands(String html, List<ValidationError> errors) {
        Matcher matcher = UNESCAPED_AMPERSAND.matcher(html);
        int count = 0;
        int firstLine = -1;
        String firstContext = null;

        while (matcher.find()) {
            // Skip ampersands inside CDATA sections, comments, style, or script tags
            int pos = matcher.start();
            if (isInsideComment(html, pos) || isInsideCdata(html, pos) ||
                isInsideTag(html, pos, "style") || isInsideTag(html, pos, "script")) {
                continue;
            }

            count++;
            if (firstLine == -1) {
                firstLine = getLineNumber(html, pos);
                firstContext = getContextAround(html, pos, 30);
            }
        }

        if (count > 0) {
            errors.add(new ValidationError(
                    "UNESCAPED_AMPERSAND",
                    String.format("Found %d unescaped '&' character(s). First occurrence at line %d", count, firstLine),
                    firstLine,
                    null,
                    "Ampersands must be escaped as '&amp;'. Common cases:\n" +
                            "- URLs: ?param1=a&param2=b → ?param1=a&amp;param2=b\n" +
                            "- Text: 'Tom & Jerry' → 'Tom &amp; Jerry'\n" +
                            "- Check data fields that may contain '&' characters",
                    firstContext
            ));
        }
    }

    /**
     * Check if a position is inside an HTML/XML comment
     */
    private boolean isInsideComment(String html, int position) {
        int lastCommentStart = html.lastIndexOf("<!--", position);
        if (lastCommentStart == -1) return false;

        int lastCommentEnd = html.lastIndexOf("-->", position);
        return lastCommentStart > lastCommentEnd;
    }

    /**
     * Check if a position is inside a CDATA section
     */
    private boolean isInsideCdata(String html, int position) {
        int lastCdataStart = html.lastIndexOf("<![CDATA[", position);
        if (lastCdataStart == -1) return false;

        int lastCdataEnd = html.lastIndexOf("]]>", position);
        return lastCdataStart > lastCdataEnd;
    }

    /**
     * Check if a position is inside a specific tag (e.g., style, script)
     */
    private boolean isInsideTag(String html, int position, String tagName) {
        String lowerHtml = html.toLowerCase();
        String openTag = "<" + tagName.toLowerCase();
        String closeTag = "</" + tagName.toLowerCase() + ">";

        int lastOpenTag = lowerHtml.lastIndexOf(openTag, position);
        if (lastOpenTag == -1) return false;

        // Find where the opening tag ends (the > character)
        int openTagEnd = lowerHtml.indexOf(">", lastOpenTag);
        if (openTagEnd == -1 || openTagEnd >= position) return false;

        int lastCloseTag = lowerHtml.lastIndexOf(closeTag, position);
        return lastOpenTag > lastCloseTag;
    }

    /**
     * Normalize HTML for well-formed XML compliance.
     * - Converts HTML entities to numeric equivalents
     * - Escapes remaining unescaped ampersands (in CSS comments, text, etc.)
     *
     * This is required for OpenHTMLToPDF as it needs well-formed XML input.
     *
     * @param html The HTML content to normalize
     * @return Normalized XML-compliant content
     */
    public String normalizeHtmlEntities(String html) {
        String result = html;

        // First, convert named HTML entities to numeric equivalents
        for (Map.Entry<String, String> entry : HTML_ENTITIES.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        // Now escape any remaining unescaped ampersands
        // Use regex to find & not followed by # or letter+semicolon (valid entity)
        // Replace with &amp;
        result = UNESCAPED_AMPERSAND.matcher(result).replaceAll("&amp;");

        return result;
    }

    /**
     * Validate XML structure using SAX parser
     */
    private void validateXmlStructure(String html, String templateName, List<ValidationError> existingErrors) {
        List<ValidationError> xmlErrors = new ArrayList<>();

        // Normalize HTML entities to numeric equivalents before validation
        // This prevents false positives for common entities like &nbsp;
        String normalizedHtml = normalizeHtmlEntities(html);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setNamespaceAware(true);

            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();

            // Custom error handler to collect all errors
            builder.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) {
                    // Ignore warnings
                }

                @Override
                public void error(SAXParseException e) {
                    xmlErrors.add(createXmlError(e, "XML_ERROR"));
                }

                @Override
                public void fatalError(SAXParseException e) {
                    xmlErrors.add(createXmlError(e, "XML_FATAL_ERROR"));
                }
            });

            builder.parse(new InputSource(new StringReader(normalizedHtml)));

        } catch (SAXParseException e) {
            xmlErrors.add(createXmlError(e, "XML_PARSE_ERROR"));
        } catch (Exception e) {
            // If parsing completely fails, add a generic error
            xmlErrors.add(ValidationError.of(
                    "XML_PARSE_FAILED",
                    "Failed to parse template as XML: " + e.getMessage(),
                    "Ensure the template is valid HTML with all tags properly closed"
            ));
        }

        if (!xmlErrors.isEmpty()) {
            List<ValidationError> allErrors = new ArrayList<>(existingErrors);
            allErrors.addAll(xmlErrors);

            throw new PdfTemplateValidationException(
                    buildErrorSummary(allErrors),
                    templateName,
                    allErrors
            );
        }
    }

    /**
     * Create a ValidationError from SAXParseException with user-friendly message
     */
    private ValidationError createXmlError(SAXParseException e, String errorType) {
        String message = e.getMessage();
        String suggestion = getSuggestionForXmlError(message);

        return new ValidationError(
                errorType,
                message,
                e.getLineNumber(),
                e.getColumnNumber(),
                suggestion,
                null
        );
    }

    /**
     * Get a helpful suggestion based on the XML error message
     */
    private String getSuggestionForXmlError(String message) {
        if (message == null) {
            return "Check the template for XML/HTML syntax errors";
        }

        String lowerMessage = message.toLowerCase();

        if (lowerMessage.contains("markup in the document preceding the root element")) {
            return "There is content before the root <html> element. " +
                    "Check for embedded XML declarations (<?xml...?>) inside SVG elements or other content before <!DOCTYPE>";
        }

        if (lowerMessage.contains("must be terminated by the matching end-tag")) {
            return "A tag is not properly closed. All tags must have matching closing tags " +
                    "or be self-closing (e.g., <br /> instead of <br>)";
        }

        if (lowerMessage.contains("element type") && lowerMessage.contains("must be declared")) {
            return "Unknown HTML element found. Ensure all elements are valid HTML elements";
        }

        if (lowerMessage.contains("attribute") && lowerMessage.contains("must be quoted")) {
            return "All attribute values must be quoted (e.g., class=\"myclass\" not class=myclass)";
        }

        if (lowerMessage.contains("the entity") && lowerMessage.contains("was referenced")) {
            return "HTML entities must use numeric equivalents. " +
                    "Replace named entities with their numeric versions (e.g., &nbsp; → &#160;, &copy; → &#169;). " +
                    "Check for special characters in text content that may need escaping.";
        }

        if (lowerMessage.contains("entity name must immediately follow")) {
            return "Found an unescaped '&' character. Ampersands must be escaped as '&amp;' " +
                    "or used only in valid entity references. Check for URLs with query parameters " +
                    "(e.g., ?foo=1&bar=2 should be ?foo=1&amp;bar=2) or other unescaped ampersands in text.";
        }

        if (lowerMessage.contains("document root element")) {
            return "The document must have a single root <html> element containing all content";
        }

        return "Check the template for XML/HTML syntax errors at the indicated line and column";
    }

    /**
     * Build a summary message from multiple errors
     */
    private String buildErrorSummary(List<ValidationError> errors) {
        if (errors.isEmpty()) {
            return "Template validation failed";
        }

        if (errors.size() == 1) {
            return "Template validation failed: " + errors.get(0).getMessage();
        }

        return String.format("Template validation failed with %d error(s). " +
                "Primary issue: %s", errors.size(), errors.get(0).getMessage());
    }

    /**
     * Get line number for a position in the string
     */
    private int getLineNumber(String text, int position) {
        if (position < 0 || position >= text.length()) {
            return 1;
        }

        int lineNumber = 1;
        for (int i = 0; i < position; i++) {
            if (text.charAt(i) == '\n') {
                lineNumber++;
            }
        }
        return lineNumber;
    }

    /**
     * Get context around a position for error reporting
     */
    private String getContextAround(String text, int position, int contextLength) {
        int start = Math.max(0, position - contextLength);
        int end = Math.min(text.length(), position + contextLength);

        String before = start > 0 ? "..." : "";
        String after = end < text.length() ? "..." : "";

        return before + text.substring(start, end).replace("\n", "\\n") + after;
    }

    /**
     * Result of template validation
     */
    public record ValidationResult(
            boolean valid,
            List<ValidationError> errors,
            String message
    ) {}
}
