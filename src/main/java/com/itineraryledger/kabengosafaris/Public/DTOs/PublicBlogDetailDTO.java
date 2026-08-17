package com.itineraryledger.kabengosafaris.Public.DTOs;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One article, in full — the shape the website's BlogPost interface already has, plus the
 * images the file-based version never had.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicBlogDetailDTO {

    private String slug;
    @Translatable private String title;
    @Translatable private String excerpt;
    private LocalDate date;
    private String author;
    private Integer readMinutes;
    private List<String> tags;

    private List<PublicBlogBlockDTO> body;
    private List<PublicBlogFaqDTO> faqs;

    private String coverImageUrl;
    private List<PublicBlogImageDTO> images;

    /* SEO overrides; the page falls back to title/excerpt when these are empty */
    @Translatable private String metaTitle;
    @Translatable private String metaDescription;
}
