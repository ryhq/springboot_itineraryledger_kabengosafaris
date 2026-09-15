package com.itineraryledger.kabengosafaris.Attribution;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * How somebody arrived, carried on every lead the public site creates.
 *
 * Two touches are kept, not one, because they answer different questions. The
 * last touch is what converted them and is what an ad platform will be judged on.
 * The first touch is what introduced them, and for a trip that takes months to
 * decide it is frequently a different channel entirely: credit the last click
 * alone and the campaign that actually found the customer looks worthless.
 *
 * Embedded rather than joined. A lead has exactly one arrival and it never
 * changes after insert, so a row of its own would only buy a join.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attribution {

    /* ---------- last touch: the visit that produced the lead ---------- */

    @Enumerated(EnumType.STRING)
    @Column(name = "attr_channel", length = 30)
    private AcquisitionChannel channel;

    @Column(name = "attr_source", length = 120)
    private String source;

    @Column(name = "attr_medium", length = 120)
    private String medium;

    @Column(name = "attr_campaign", length = 180)
    private String campaign;

    /** utm_content — which creative, which variant. The unit of an A/B test. */
    @Column(name = "attr_content", length = 180)
    private String content;

    /** utm_term — the keyword that was bid on. */
    @Column(name = "attr_term", length = 180)
    private String term;

    /**
     * The platform's own click identifier (gclid, fbclid, msclkid…). Kept whole
     * because it is the only thing that can be matched back against the ad
     * platform's own report when our count and theirs disagree.
     */
    @Column(name = "attr_click_id", length = 255)
    private String clickId;

    @Column(name = "attr_click_id_type", length = 20)
    private String clickIdType;

    /** The first page of the converting visit — which page the ad actually sold. */
    @Column(name = "attr_landing_page", length = 500)
    private String landingPage;

    @Column(name = "attr_referrer", length = 500)
    private String referrer;

    /* ---------- first touch: the visit that introduced them ---------- */

    @Enumerated(EnumType.STRING)
    @Column(name = "attr_first_channel", length = 30)
    private AcquisitionChannel firstChannel;

    @Column(name = "attr_first_source", length = 120)
    private String firstSource;

    @Column(name = "attr_first_medium", length = 120)
    private String firstMedium;

    @Column(name = "attr_first_campaign", length = 180)
    private String firstCampaign;

    @Column(name = "attr_first_seen_at")
    private LocalDateTime firstSeenAt;

    /**
     * How many distinct visits preceded the form. One means they converted on the
     * visit that found us; a high count against a paid first touch means the ad
     * worked and the closing channel is merely taking the credit.
     */
    @Column(name = "attr_touch_count")
    private Integer touchCount;

    /** True when nothing was captured at all, so the panel can say so rather than guess. */
    public boolean isEmpty() {
        return channel == null && source == null && referrer == null && landingPage == null;
    }
}
