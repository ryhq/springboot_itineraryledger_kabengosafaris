package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Everything the public website publishes, in one response, in English.
 *
 * <p>This exists for the website's {@code /llms-full.txt} - the file a language model reads when
 * somebody asks it what a Tanzanian safari costs. Built from the list endpoints it would have
 * taken 194 detail requests, because the list DTOs deliberately omit the prose: a park list has
 * no {@code fullDescription} or {@code wildlife}, a lodge list no {@code details} or
 * {@code priceRange}, a safari list no days. On a cold deploy every one of those is uncached,
 * which is a slow and fragile way to build a static file.
 *
 * <p>Each collection carries the same DTO its own detail endpoint serves, built by the same
 * converter, so a record can never read one way here and another way on its page.
 *
 * <h2>English, whatever the caller asks for</h2>
 *
 * <p>The {@code Accept-Language} header is ignored. Translation happens on demand and is cached
 * on a hash of the English, so answering this endpoint in German would push roughly 300 KB
 * through the translation provider in one request. The llms.txt convention is a single file
 * anyway, and machine translation of this company's prose has a poor record.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicCorpusDTO {

    /** When this was assembled, so the file can say so rather than imply freshness. */
    private String generatedAt;

    /** Always "en". Stated rather than assumed, so a reader need not guess. */
    private String language;

    private List<PublicParkDetailDTO> parks;

    /**
     * Conservation fees, non-resident adult, by park and season.
     *
     * <p>A parallel list keyed by {@code parkSlug} rather than a field on the park, because the
     * park DTO is shared with the detail endpoint and this endpoint has no business changing
     * what that one serves.
     */
    private List<PublicParkFeeDTO> parkFees;

    private List<PublicAccommodationDTO> accommodations;
    private List<PublicActivityDetailDTO> activities;
    private List<PublicItineraryDTO> safaris;
    private List<PublicBlogDetailDTO> articles;
}
