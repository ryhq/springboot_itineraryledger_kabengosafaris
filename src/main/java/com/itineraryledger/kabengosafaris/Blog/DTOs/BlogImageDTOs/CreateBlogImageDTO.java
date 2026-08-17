package com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One file in a multipart upload.
 *
 * The panel posts an INDEXED list (`images[0].blogId`, `images[0].image`, …), which is why
 * this is a per-item DTO rather than one object with a list of files.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBlogImageDTO {

    @NotNull(message = "Blog ID is required")
    private String blogId;

    @NotNull(message = "Image file is required")
    private MultipartFile image;

    private String altText;
    private String caption;
    private String description;
    /** Make this image the post's cover. */
    private Boolean isPrimary;
}
