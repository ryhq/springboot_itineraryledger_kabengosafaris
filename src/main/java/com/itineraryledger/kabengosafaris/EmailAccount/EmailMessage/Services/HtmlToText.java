package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The plain-text reading of an HTML mail body.
 *
 * Every message this system sends went out as HTML only — {@code helper.setText(html, true)}, one
 * part, no alternative. That has two costs paid by the recipient rather than by us:
 *
 *  - spam filters score a message with no text/plain part worse, so our quotes are likelier to
 *    land in a junk folder;
 *  - anything reading text (a screen reader, a watch, a client set to plain text, a helpdesk that
 *    ingests mail) gets tag soup or nothing at all.
 *
 * A real HTML parser would be better, but jsoup is not on the classpath and a mail body is not
 * arbitrary markup — it is what our own composer produced. So this is deliberately conservative:
 * it never throws, and if anything looks wrong it degrades to the tags-stripped text.
 *
 * Links keep their destination — "click here" with the address discarded is useless in text.
 */
public final class HtmlToText {

    private HtmlToText() {}

    private static final Pattern SCRIPT_OR_STYLE =
        Pattern.compile("<(script|style|head)[^>]*>.*?</\\1>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ANCHOR =
        Pattern.compile("<a\\s[^>]*href\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern LIST_ITEM = Pattern.compile("<li[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern BREAK = Pattern.compile("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
    private static final Pattern RULE = Pattern.compile("<hr\\s*/?>", Pattern.CASE_INSENSITIVE);
    /* a paragraph is separated by a blank line, the way it reads on the page */
    private static final Pattern PARAGRAPH_END =
        Pattern.compile("</(p|h1|h2|h3|h4|h5|h6|ul|ol|blockquote|table)\\s*>", Pattern.CASE_INSENSITIVE);
    /* these only end a line */
    private static final Pattern BLOCK_END =
        Pattern.compile("</(div|tr|pre)\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern CELL_END = Pattern.compile("</(td|th)\\s*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    /*
     * Stand-ins for the angle brackets around a link's address.
     *
     * Writing "the Serengeti trip <https://…>" straight into the text meant the tag stripper
     * below removed it again — the address looks exactly like a tag — so every link in the text
     * half arrived as its label alone. These placeholders survive that pass and are swapped back
     * afterwards.
     */
    private static final String OPEN = "\u0001";
    private static final String CLOSE = "\u0002";

    public static String convert(String html) {
        if (html == null || html.isBlank()) return "";
        try {
            String text = SCRIPT_OR_STYLE.matcher(html).replaceAll(" ");

            // a link becomes "text <address>" unless the text already IS the address
            Matcher anchors = ANCHOR.matcher(text);
            StringBuilder withLinks = new StringBuilder();
            while (anchors.find()) {
                String href = anchors.group(1).trim();
                String label = TAG.matcher(anchors.group(2)).replaceAll("").trim();
                String replacement = label.isEmpty() || label.equalsIgnoreCase(href)
                    ? href
                    : label + " " + OPEN + href + CLOSE;
                anchors.appendReplacement(withLinks, Matcher.quoteReplacement(replacement));
            }
            anchors.appendTail(withLinks);
            text = withLinks.toString();

            text = LIST_ITEM.matcher(text).replaceAll("\n• ");
            text = BREAK.matcher(text).replaceAll("\n");
            text = RULE.matcher(text).replaceAll("\n----------\n");
            text = CELL_END.matcher(text).replaceAll("\t");
            text = PARAGRAPH_END.matcher(text).replaceAll("\n\n");
            text = BLOCK_END.matcher(text).replaceAll("\n");
            text = TAG.matcher(text).replaceAll("");

            text = unescape(text);
            text = text.replace(OPEN, "<").replace(CLOSE, ">");

            // tidy the whitespace the tags left behind
            text = text.replace("\r", "")
                       .replaceAll("[ \\t]+\\n", "\n")
                       .replaceAll("\\n{3,}", "\n\n")
                       .replaceAll("[ \\t]{2,}", " ");
            return text.trim();
        } catch (Exception e) {
            // never let the text half break the send: strip the tags and move on
            return TAG.matcher(html).replaceAll("").trim();
        }
    }

    private static String unescape(String value) {
        return value
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&lsquo;", "‘")
            .replace("&rsquo;", "’")
            .replace("&ldquo;", "“")
            .replace("&rdquo;", "”")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            // last, or an escaped entity would be decoded twice
            .replace("&amp;", "&");
    }
}
