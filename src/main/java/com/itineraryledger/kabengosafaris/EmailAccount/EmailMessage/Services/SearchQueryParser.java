package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

/**
 * §8 — parses search bar input into structured operators.
 *
 * Supported operators (any combination, in any order):
 *   from:&lt;substr&gt;        to:&lt;substr&gt;     subject:&lt;substr&gt;
 *   has:attachment        is:unread        is:starred     is:flagged
 *   label:&lt;substr&gt;        before:YYYY-MM-DD                after:YYYY-MM-DD
 *
 * Anything left over after stripping operator tokens is treated as a
 * free-text query against subject + from + to + snippet.
 *
 * Tokenisation is whitespace-split; values with spaces are not supported
 * (operators can be repeated instead, e.g. label:quote label:booking).
 */
public class SearchQueryParser {

    @Data
    public static class Parsed {
        private final List<String> from = new ArrayList<>();
        private final List<String> to = new ArrayList<>();
        private final List<String> subject = new ArrayList<>();
        private final List<String> label = new ArrayList<>();
        private Boolean hasAttachment;
        private Boolean isUnread;
        private Boolean isStarred;
        private Boolean isFlagged;
        private LocalDateTime before;
        private LocalDateTime after;
        private String freeText;
    }

    public static Parsed parse(String input) {
        Parsed out = new Parsed();
        if (input == null || input.isBlank()) {
            out.setFreeText("");
            return out;
        }
        List<String> bare = new ArrayList<>();
        for (String tok : input.trim().split("\\s+")) {
            int colon = tok.indexOf(':');
            if (colon <= 0 || colon == tok.length() - 1) {
                bare.add(tok);
                continue;
            }
            String key = tok.substring(0, colon).toLowerCase();
            String val = tok.substring(colon + 1);
            switch (key) {
                case "from" -> out.getFrom().add(val);
                case "to" -> out.getTo().add(val);
                case "subject" -> out.getSubject().add(val);
                case "label" -> out.getLabel().add(val);
                case "has" -> { if ("attachment".equalsIgnoreCase(val)) out.setHasAttachment(true); else bare.add(tok); }
                case "is" -> {
                    switch (val.toLowerCase()) {
                        case "unread" -> out.setIsUnread(true);
                        case "read" -> out.setIsUnread(false);
                        case "starred" -> out.setIsStarred(true);
                        case "flagged" -> out.setIsFlagged(true);
                        default -> bare.add(tok);
                    }
                }
                case "before" -> {
                    LocalDateTime d = parseDate(val);
                    if (d != null) out.setBefore(d); else bare.add(tok);
                }
                case "after" -> {
                    LocalDateTime d = parseDate(val);
                    if (d != null) out.setAfter(d); else bare.add(tok);
                }
                default -> bare.add(tok);
            }
        }
        out.setFreeText(String.join(" ", bare));
        return out;
    }

    private static LocalDateTime parseDate(String val) {
        try {
            return LocalDate.parse(val).atStartOfDay();
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
