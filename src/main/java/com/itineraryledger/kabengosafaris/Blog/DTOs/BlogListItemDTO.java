package com.itineraryledger.kabengosafaris.Blog.DTOs;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A row in the blog list.
 *
 * No body: an article is thousands of words and a list of twenty would be megabytes for a
 * table that shows the title.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogListItemDTO {

    private String id;
    private String slug;
    private String title;
    private String excerpt;
    private String author;
    private LocalDate publishDate;
    private Integer readMinutes;
    private List<String> tags;
    private Boolean isPublished;
    private Integer displayOrder;
    private String coverImageUrl;
    private Long imageCount;
    private Integer blockCount;
    private Integer faqCount;
    private Boolean hasCover;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
