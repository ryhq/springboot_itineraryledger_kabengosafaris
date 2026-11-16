package com.itineraryledger.kabengosafaris.Text;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The characters we do not type, and what they become.
 *
 * <p>Content across this installation read as machine-written, and the tell was never the wording.
 * It was punctuation nobody reaches for on a keyboard: em dashes joining clauses, middle dots
 * separating a footer, curly quotes, ellipsis glyphs, check marks in subject lines. A reader does
 * not think "an em dash"; they think "this was not written by a person", and on a quotation for a
 * safari that is the wrong first impression.
 *
 * <p>{@code src/main/resources/typography.json} is the definition. It is committed identically in
 * four repositories that cannot import each other, the same arrangement as {@code WebsiteCacheTags}
 * and for the same reason, and {@code TypographyMapIsOneDefinitionTest} fails the build if this
 * class and that file disagree.
 *
 * <h2>What this deliberately does not do</h2>
 *
 * <p>It never touches a letter carrying a diacritic, and never touches anything outside the Latin
 * script. That is not a nicety. Five locale catalogues are built from accented letters, one is
 * entirely Cyrillic, the websites list language names natively, and a Spanish lodge called Sueños
 * does not become Suenos because its name offended a regular expression. Stripping accents is the
 * one move in this whole exercise that loses meaning rather than style.
 *
 * <p>It is also not applied to anything a customer, a visitor or a supplier wrote. Their words are
 * a record, and tidying a record is falsifying it.
 */
public final class Typography {

    private Typography() {
    }

    /**
     * One character in, one string out.
     *
     * <p>The spacing comes from the text, not from here. {@code " · "} becomes {@code " | "} and
     * {@code "a·b"} becomes {@code "a|b"} without the map having to know which it is looking at.
     *
     * <p>No replacement introduces a character that is itself a key, so the order of application
     * does not matter and folding twice changes nothing the second time.
     */
    public static final Map<Character, String> FOLD;

    /**
     * Box-drawing art, which only ever appears in comments, log banners and developer notes.
     *
     * <p>Separate from {@link #FOLD} so that a sweep of customer-facing text cannot reshape a
     * diagram by accident, and so the internal sweep can be one commit somebody is free to drop.
     * The replacements keep the shape: a banner still looks like a banner.
     */
    public static final Map<Character, String> FOLD_INTERNAL;

    /**
     * Characters that must not survive, whose replacement depends on the sentence.
     *
     * <p>The value is the guidance a person needs, not a substitution. An arrow between two places
     * is the word "to"; the same arrow on a button is decoration and goes. A star rating is an
     * interface element and belongs in an SVG, not in a string. {@code km²} is reworded to
     * {@code sq km} rather than substituted.
     */
    public static final Map<Character, String> MANUAL;

    /** Characters that are content, not decoration. */
    public static final Map<Character, String> KEEP;

    static {
        Map<Character, String> fold = new LinkedHashMap<>();
        fold.put('—', "-");   // em dash
        fold.put('–', "-");   // en dash
        fold.put('‒', "-");   // figure dash
        fold.put('―', "-");   // horizontal bar
        fold.put('−', "-");   // minus sign
        fold.put('·', "|");   // middle dot
        fold.put('•', "-");   // bullet
        fold.put('…', "...");
        fold.put('⋯', "...");
        fold.put('’', "'");
        fold.put('‘', "'");
        fold.put('‚', "'");
        fold.put('“', "\"");
        fold.put('”', "\"");
        fold.put('„', "\"");
        fold.put('«', "\"");
        fold.put('»', "\"");
        fold.put('×', "x");
        fold.put('✕', "x");
        fold.put('≈', "~");
        fold.put('≥', ">=");
        fold.put('≤', "<=");
        fold.put('‹', "<");
        fold.put('›', ">");
        fold.put('⌘', "Cmd");
        fold.put(' ', " ");   // non-breaking space
        fold.put('​', "");    // zero-width space
        fold.put('­', "");    // soft hyphen
        FOLD = Collections.unmodifiableMap(fold);

        Map<Character, String> internal = new LinkedHashMap<>();
        for (char c : "─━".toCharArray()) internal.put(c, "-");
        for (char c : "│┃║".toCharArray()) internal.put(c, "|");
        for (char c : "┌┐└┘├┤┬┴┼".toCharArray()) {
            internal.put(c, "+");
        }
        for (char c : "╔╗╚╝╠╣╦╩╬".toCharArray()) {
            internal.put(c, "+");
        }
        internal.put('═', "=");
        for (char c : "█░▒▓".toCharArray()) internal.put(c, "#");
        internal.put('▲', "^");
        internal.put('▼', "v");
        internal.put('⊘', "x");
        internal.put('◍', "o");
        internal.put('◌', "o");
        internal.put('⤢', "[]");
        FOLD_INTERNAL = Collections.unmodifiableMap(internal);

        Map<Character, String> manual = new LinkedHashMap<>();
        manual.put('→', "a route takes the word: 'Arusha to Serengeti'. On a button the arrow "
            + "is decoration, so it goes.");
        manual.put('←', "same: '< Back' reads as a glyph nobody typed. Drop it, or use an "
            + "inline SVG chevron.");
        manual.put('↑', "spell the direction, or use an icon");
        manual.put('↓', "spell the direction, or use an icon");
        manual.put('↔', "spell the direction, or use an icon");
        manual.put('⇒', "'becomes', 'leads to', or a plain '->' in developer notes");
        manual.put('★', "a rating is an interface element, not a character: use the "
            + "StarRating SVG component");
        manual.put('☆', "as above");
        manual.put('●', "a tier meter is an interface element: use the TierDots SVG component");
        manual.put('○', "as above");
        manual.put('✓', "in a subject line it reads as clip-art. Say the thing: "
            + "'Backup completed'.");
        manual.put('✗', "say the thing: 'Backup failed'");
        manual.put('⚠', "say the thing. A warning sign in a subject line is not more urgent, "
            + "only harder to search.");
        manual.put('²', "'14,763 km²' becomes '14,763 sq km'. Rewording, not substitution.");
        manual.put('³', "spell the unit");
        manual.put('°', "spell it, or keep it if it is a real coordinate. Eight occurrences, "
            + "decide at the diff.");
        manual.put('∞', "'and over'");
        manual.put('½', "'half'");
        manual.put('™', "drop it");
        manual.put('®', "drop it");
        MANUAL = Collections.unmodifiableMap(manual);

        Map<Character, String> keep = new LinkedHashMap<>();
        keep.put('€', "currency");
        keep.put('£', "currency");
        keep.put('¥', "currency");
        keep.put('©', "copyright, in every footer");
        keep.put('$', "currency");
        KEEP = Collections.unmodifiableMap(keep);
    }

    /**
     * Applies {@link #FOLD}. Null in, null out, because a null description is a description that
     * was never written and must stay that way rather than becoming an empty string.
     */
    public static String fold(String text) {
        return apply(text, FOLD);
    }

    /** Applies {@link #FOLD} and {@link #FOLD_INTERNAL} together. */
    public static String foldAll(String text) {
        return apply(apply(text, FOLD), FOLD_INTERNAL);
    }

    private static String apply(String text, Map<Character, String> map) {
        if (text == null || text.isEmpty()) return text;

        /*
         * Walk once and only build when there is something to build. Almost every string reaching
         * this is already clean, and this sits on the deserialisation path of every request.
         */
        StringBuilder out = null;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String replacement = map.get(c);
            if (replacement == null) {
                if (out != null) out.append(c);
                continue;
            }
            if (out == null) out = new StringBuilder(text.length() + 8).append(text, 0, i);
            out.append(replacement);
        }
        return out == null ? text : out.toString();
    }

    /**
     * Whether this character has to go, by any of the three routes.
     *
     * <p>What the guards ask. {@link #KEEP}, letters with diacritics and non-Latin scripts answer
     * false, which is the whole point of asking here rather than testing for "not ASCII".
     */
    public static boolean isUnwanted(char c) {
        return FOLD.containsKey(c) || FOLD_INTERNAL.containsKey(c) || MANUAL.containsKey(c);
    }
}
