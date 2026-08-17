package com.itineraryledger.kabengosafaris.Blog.Services.BlogServices;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogBlockDTO;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogFaqDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The article's content, in and out of storage.
 *
 * ONE implementation of the block JSON, the slug and the reading time, shared by create,
 * update, the public reader and the seeder — so an article written by the initializer and one
 * written in the panel cannot end up in different shapes.
 *
 * Unreadable JSON returns an EMPTY list rather than throwing: a post whose body cannot be
 * parsed should still be listable and fixable, not a 500 on every page that mentions it.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BlogContentService {

    /** Average adult reading speed, near enough for a "6 min read" label. */
    private static final int WORDS_PER_MINUTE = 200;

    private final ObjectMapper objectMapper;

    public List<BlogBlockDTO> readBlocks(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<BlogBlockDTO> blocks = objectMapper.readValue(json, new TypeReference<List<BlogBlockDTO>>() {});
            return blocks != null ? blocks : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Blog body JSON could not be parsed, treating it as empty: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<BlogFaqDTO> readFaqs(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            List<BlogFaqDTO> faqs = objectMapper.readValue(json, new TypeReference<List<BlogFaqDTO>>() {});
            return faqs != null ? faqs : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Blog FAQ JSON could not be parsed, treating it as empty: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public String writeJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("Blog content could not be serialised", e);
            return null;
        }
    }

    /** Every word the reader actually reads: paragraph text, headings, bullets, captions. */
    public int wordCount(List<BlogBlockDTO> blocks) {
        if (blocks == null) return 0;
        int words = 0;
        for (BlogBlockDTO block : blocks) {
            if (block == null) continue;
            words += countWords(block.getText());
            words += countWords(block.getCaption());
            if (block.getItems() != null) {
                for (String item : block.getItems()) words += countWords(item);
            }
        }
        return words;
    }

    /** Never zero: a one-line post is still "1 min read", and 0 reads as missing data. */
    public int estimateReadMinutes(List<BlogBlockDTO> blocks) {
        int words = wordCount(blocks);
        return Math.max(1, (int) Math.ceil((double) words / WORDS_PER_MINUTE));
    }

    /**
     * A URL-safe slug from a title.
     *
     * Accents are folded rather than dropped so "Ngorongoro Krater" and "Krater" do not
     * collide into the same address.
     */
    public String slugify(String input) {
        if (input == null || input.isBlank()) return null;
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String slug = normalized.toLowerCase(Locale.ENGLISH)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
        if (slug.length() > 200) slug = slug.substring(0, 200).replaceAll("-$", "");
        return slug.isBlank() ? null : slug;
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}
