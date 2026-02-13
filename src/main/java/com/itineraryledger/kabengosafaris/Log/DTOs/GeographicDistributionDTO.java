package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for geographic distribution analytics
 * Provides country-based request distribution
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GeographicDistributionDTO {

    /**
     * Country code (ISO 3166-1 alpha-2)
     */
    private String countryCode;

    /**
     * Country name
     */
    private String countryName;

    /**
     * Number of requests from this country
     */
    private Long requestCount;

    /**
     * Percentage of total requests
     */
    private Double percentage;

    /**
     * Number of unique IPs from this country
     */
    private Long uniqueIPs;

    /**
     * Average response time for requests from this country
     */
    private Double avgResponseTime;

    /**
     * Number of suspicious requests from this country
     */
    private Long suspiciousCount;
}
