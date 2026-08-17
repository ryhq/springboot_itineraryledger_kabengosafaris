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
 * A card on the blog index.
 *
 * `date` rather than `publishDate`: this is the website's BlogPost field name, and keeping it
 * means the Next.js side needs no mapping layer at all.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicBlogListDTO {

    private String slug;
    @Translatable private String title;
    @Translatable private String excerpt;
    private LocalDate date;
    private String author;
    private Integer readMinutes;
    /** Not translated: tags are also filter keys, and a translated key matches nothing. */
    private List<String> tags;
    private String coverImageUrl;
}
