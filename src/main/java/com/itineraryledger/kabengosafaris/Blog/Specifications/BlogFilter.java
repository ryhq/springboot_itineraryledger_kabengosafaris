package com.itineraryledger.kabengosafaris.Blog.Specifications;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

/**
 * Everything a caller can narrow the blog list by, in one object — so the rows, the counters
 * and the record walk are asked the same question.
 */
@Data
public class BlogFilter {

    /** Free text across title, slug, excerpt, author, tag and the body itself. */
    private String keyword;

    private String author;
    private String tag;

    /** Published / draft, as a multi-value facet: ["published","draft"] cancels out. */
    private List<String> statuses;
    private Boolean isPublished;

    /** Data quality the panel can act on. */
    private List<String> qualities;

    private LocalDate publishedAfter;
    private LocalDate publishedBefore;
}
