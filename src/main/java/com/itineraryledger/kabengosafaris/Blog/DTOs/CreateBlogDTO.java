package com.itineraryledger.kabengosafaris.Blog.DTOs;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** What is required to write a new article. The slug is derived from the title when absent. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBlogDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must be at most 500 characters")
    private String title;

    @Size(max = 255, message = "Slug must be at most 255 characters")
    private String slug;

    private String excerpt;
    private String author;
    private LocalDate publishDate;
    /** Left blank, it is estimated from the body. */
    private Integer readMinutes;
    private List<String> tags;
    private List<BlogBlockDTO> body;
    private List<BlogFaqDTO> faqs;
    private Boolean isPublished;
    private Integer displayOrder;
    private String metaTitle;
    private String metaDescription;
}
