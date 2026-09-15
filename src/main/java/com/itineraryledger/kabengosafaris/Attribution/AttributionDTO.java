package com.itineraryledger.kabengosafaris.Attribution;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

/**
 * How a lead arrived, as the panel reads it.
 *
 * The channel carries its display name alongside the enum so a screen never has to
 * keep its own copy of the wording, and the same goes for the first-touch channel:
 * those two are what a campaign report groups by.
 */
@Data
@Builder
public class AttributionDTO {

    private AcquisitionChannel channel;
    private String channelDisplayName;
    private String source;
    private String medium;
    private String campaign;
    private String content;
    private String term;
    private String clickId;
    private String clickIdType;
    private String landingPage;
    private String referrer;

    private AcquisitionChannel firstChannel;
    private String firstChannelDisplayName;
    private String firstSource;
    private String firstMedium;
    private String firstCampaign;
    private LocalDateTime firstSeenAt;
    private Integer touchCount;

    /**
     * True when the last touch differs from the first, which is the case worth
     * showing: it means one channel introduced them and another closed them, and a
     * report that credits only one of the two is describing half the story.
     */
    private boolean multiTouch;

    /** Whether the click was bought, so a screen can mark spend without repeating the enum list. */
    private boolean paid;
}
