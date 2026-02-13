package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for endpoint-level analytics
 * Provides usage statistics for individual endpoints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EndpointAnalyticsDTO {

    /**
     * Request URI/endpoint
     */
    private String endpoint;

    /**
     * Total number of requests to this endpoint
     */
    private Long requestCount;

    /**
     * Average response time in milliseconds
     */
    private Double avgResponseTime;

    /**
     * Minimum response time in milliseconds
     */
    private Long minResponseTime;

    /**
     * Maximum response time in milliseconds
     */
    private Long maxResponseTime;

    /**
     * Number of successful requests (2xx, 3xx)
     */
    private Long successCount;

    /**
     * Number of errors (4xx, 5xx)
     */
    private Long errorCount;

    /**
     * Error rate as a percentage (0-100)
     */
    private Double errorRate;

    /**
     * Average response size in bytes
     */
    private Double avgResponseSize;

    /**
     * Total bandwidth consumed by this endpoint in bytes
     */
    private Long totalBandwidth;

    /**
     * Number of unique IPs accessing this endpoint
     */
    private Long uniqueIPs;

    /**
     * Most common HTTP method for this endpoint
     */
    private String mostCommonMethod;

    /**
     * Number of suspicious requests to this endpoint
     */
    private Long suspiciousCount;
}
