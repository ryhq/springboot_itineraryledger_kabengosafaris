package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A gallery image of an article. Alt text and caption are read aloud, so both translate. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicBlogImageDTO {
    private String url;
    @Translatable private String alt;
    @Translatable private String caption;
    private Boolean isCover;
    private Integer displayOrder;
    private Integer width;
    private Integer height;
}
