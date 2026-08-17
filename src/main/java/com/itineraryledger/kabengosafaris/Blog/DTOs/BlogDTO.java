package com.itineraryledger.kabengosafaris.Blog.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs.BlogImageDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One article, in full, for the management panel. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogDTO {

    private String id;
    private String slug;
    private String title;
    private String excerpt;
    private String author;
    private LocalDate publishDate;
    private Integer readMinutes;
    private List<String> tags;
    /** The block list, parsed from storage — never the raw JSON string. */
    private List<BlogBlockDTO> body;
    private List<BlogFaqDTO> faqs;
    private Boolean isPublished;
    /** Set the first time the article was published; the slug is frozen once it exists. */
    private LocalDateTime firstPublishedAt;
    private Integer displayOrder;
    private String metaTitle;
    private String metaDescription;

    private List<BlogImageDTO> images;
    private String coverImageUrl;
    private Long imageCount;

    /** Read from the body, so the editor can show what the site will show. */
    private Integer wordCount;

    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
