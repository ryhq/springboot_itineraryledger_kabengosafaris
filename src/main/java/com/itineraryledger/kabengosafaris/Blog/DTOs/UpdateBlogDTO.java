package com.itineraryledger.kabengosafaris.Blog.DTOs;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patch semantics: null means "leave it alone", which is what click-to-edit needs — a single
 * field can be saved without carrying the other twenty back.
 *
 * The exception is deliberate: body, faqs and tags are LISTS, and an empty list is a real
 * value ("this article has no bullet blocks any more"), so those are applied whenever present.
 *
 * NO slug, by policy: an article's address is decided when it is created and never changes.
 * The field is absent rather than validated, so no shape of request can move a live URL —
 * every link to it, in search results, bookmarks and messages already sent, keeps working.
 * Retitling is free; the address is a promise.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBlogDTO {

    @Size(max = 500, message = "Title must be at most 500 characters")
    private String title;

    private String excerpt;
    private String author;
    private LocalDate publishDate;
    private Integer readMinutes;
    private List<String> tags;
    private List<BlogBlockDTO> body;
    private List<BlogFaqDTO> faqs;
    private Boolean isPublished;
    private Integer displayOrder;
    private String metaTitle;
    private String metaDescription;
}
