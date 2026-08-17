package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A global FAQ, in the website's shape.
 *
 * `q`/`a` mirror the site's FaqItem interface; `question`/`answer` are the same values under
 * the names the API uses elsewhere, so either side can read it without a mapping layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicFaqDTO {
    @Translatable private String q;
    @Translatable private String a;
    private String category;
}
