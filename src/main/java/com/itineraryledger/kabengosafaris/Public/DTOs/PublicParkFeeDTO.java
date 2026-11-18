package com.itineraryledger.kabengosafaris.Public.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One park's conservation fee, for a non-resident adult, in one season.
 *
 * <p>This is the single most asked question about a Tanzanian safari and the site could not
 * answer it: the public park payload carries no fee of any kind, and the rate endpoint needs a
 * token. So it is published here, in the corpus, rather than typed into the website - a number
 * typed into a template is a number nobody updates when the park authority raises it.
 *
 * <h2>Which of the two stored numbers this is</h2>
 *
 * <p>A park tariff rate carries both a <b>rack</b> rate and an <b>STO</b> rate, and rack is
 * always 1.3x STO because that is this company's margin. The amount here is the <b>STO</b> one,
 * because for a government conservation fee that is the actual gate price - the figure the park
 * authority publishes and the traveller is really asking about. Rack would overstate it by 30%
 * and contradict the company's own published cost article.
 *
 * <p>That reasoning holds for a park fee and nowhere else. No other STO rate belongs in a public
 * payload: for a lodge it is what we negotiated, which is commercial. Hence this DTO is narrow
 * on purpose - conservation fees only, non-resident adult only - rather than a general rates
 * feed.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicParkFeeDTO {

    private String parkSlug;
    private String parkName;

    /** Always "Conservation Fee" today; carried so a reader never has to assume. */
    private String tariff;

    /** "Per Person", "Per Vehicle", "Per Group" - what the amount is charged against. */
    private String chargingBasis;

    private String nationCategory;
    private String ageCategory;

    /** "High Season", "Low Season", "Shoulder Season". */
    private String season;

    /** The gate price per person per 24 hours, in {@link #currency}. */
    private BigDecimal amount;

    private String currency;
}
