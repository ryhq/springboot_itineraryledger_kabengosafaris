package com.itineraryledger.kabengosafaris.Blog.DTOs;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One block of an article's body.
 *
 * The type decides which fields matter: "p"/"h2"/"h3" use text, "ul" uses items, "image"
 * uses url/alt/caption. Unknown properties are ignored on the way in so a body written by a
 * newer editor cannot fail to load in an older server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlogBlockDTO {

    /** p | h2 | h3 | ul | image */
    private String type;

    private String text;

    private List<String> items;

    private String url;
    private String alt;
    private String caption;
}
