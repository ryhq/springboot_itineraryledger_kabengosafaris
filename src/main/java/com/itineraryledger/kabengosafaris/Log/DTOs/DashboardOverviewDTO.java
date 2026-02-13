package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for dashboard overview metrics
 * Provides high-level summary of access log analytics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardOverviewDTO {

    /**
     * Total number of requests
     */
    private Long totalRequests;

    /**
     * Number of unique IP addresses
     */
    private Long uniqueIPs;

    /**
     * Error rate as a percentage (0-100)
     */
    private Double errorRate;

    /**
     * Average response time in milliseconds
     */
    private Double avgResponseTime;

    /**
     * Number of successful requests (2xx, 3xx)
     */
    private Long successfulRequests;

    /**
     * Number of client errors (4xx)
     */
    private Long clientErrors;

    /**
     * Number of server errors (5xx)
     */
    private Long serverErrors;

    /**
     * Number of suspicious requests
     */
    private Long suspiciousRequests;

    /**
     * Number of bot requests
     */
    private Long botRequests;

    /**
     * Number of slow requests
     */
    private Long slowRequests;

    /**
     * Average response size in bytes
     */
    private Double avgResponseSize;

    /**
     * Total bandwidth consumed in bytes
     */
    private Long totalBandwidth;

    /**
     * Most accessed endpoint
     */
    private String topEndpoint;

    /**
     * Number of requests to top endpoint
     */
    private Long topEndpointCount;

    /**
     * Date for this overview (YYYY-MM-DD)
     */
    private String date;
}
