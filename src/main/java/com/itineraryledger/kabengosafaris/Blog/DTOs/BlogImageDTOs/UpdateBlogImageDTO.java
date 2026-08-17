package com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Patch semantics: null means "leave it alone".
 *
 * displayOrder is not here — ordering is the reorder endpoint's job, so two people dragging
 * images cannot write conflicting single values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBlogImageDTO {
    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
}
