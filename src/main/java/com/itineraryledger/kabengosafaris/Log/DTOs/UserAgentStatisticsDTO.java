package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for user agent statistics
 * Provides browser and operating system distribution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAgentStatisticsDTO {

    /**
     * Browser name or OS name
     */
    private String name;

    /**
     * Type: BROWSER or OS
     */
    private String type;

    /**
     * Number of requests from this browser/OS
     */
    private Long requestCount;

    /**
     * Percentage of total requests
     */
    private Double percentage;

    /**
     * Device type distribution for this browser/OS
     */
    private String deviceType;

    /**
     * Number of unique IPs using this browser/OS
     */
    private Long uniqueIPs;
}
