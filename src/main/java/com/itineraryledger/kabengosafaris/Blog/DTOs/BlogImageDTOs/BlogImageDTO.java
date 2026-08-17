package com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** An uploaded blog image, as the panel and the website read it. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BlogImageDTO {

    private String id;
    private String blogId;
    private String blogTitle;
    private String fileName;
    private String originalFileName;
    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
    private Long fileSize;
    private String fileSizeFormatted;
    private String mimeType;
    private Integer width;
    private Integer height;
    /** Served by the API: /api/blog-images/{id}/file */
    private String imageUrl;
    private String fileImageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
