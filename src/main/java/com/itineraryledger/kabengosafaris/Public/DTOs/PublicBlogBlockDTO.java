package com.itineraryledger.kabengosafaris.Public.DTOs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.Public.Annotations.Translatable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One block of an article's body, in the shape the website already renders:
 * {@code {type:"p",text}}, {@code {type:"h2",text}}, {@code {type:"ul",items}},
 * {@code {type:"image",url,alt,caption}}.
 *
 * Everything a reader reads is @Translatable — including the bullet items, which are a
 * List&lt;String&gt; rather than a nested DTO precisely to keep this JSON identical to the
 * file-based content the site ships today.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicBlogBlockDTO {

    private String type;

    @Translatable private String text;

    @Translatable private List<String> items;

    /** Not translated: a URL is a URL. */
    private String url;

    @Translatable private String alt;
    @Translatable private String caption;
}
