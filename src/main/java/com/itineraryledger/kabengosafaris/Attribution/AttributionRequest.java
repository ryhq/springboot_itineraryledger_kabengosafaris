package com.itineraryledger.kabengosafaris.Attribution;

import lombok.Data;

/**
 * The arrival tags a public form sends alongside whatever the visitor typed.
 *
 * Every value here came out of a URL the visitor could edit, so nothing on it is
 * trusted: {@link AttributionService} caps, strips and re-derives before any of it
 * is stored. The channel is deliberately absent — the browser does not get to say
 * what channel it is.
 */
@Data
public class AttributionRequest {

    private String source;
    private String medium;
    private String campaign;
    private String content;
    private String term;

    private String clickId;
    private String clickIdType;

    private String landingPage;
    private String referrer;

    private String firstSource;
    private String firstMedium;
    private String firstCampaign;

    /** ISO-8601 instant of the first visit we ever saw from this browser. */
    private String firstSeenAt;

    private Integer touchCount;
}
