package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for hourly traffic metrics
 * Provides 24-hour traffic pattern analysis
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HourlyMetricsDTO {

    /**
     * Hour of day (0-23)
     */
    private Integer hour;

    /**
     * Total number of requests in this hour
     */
    private Long requestCount;

    /**
     * Average response time in milliseconds
     */
    private Double avgResponseTime;

    /**
     * Number of successful requests
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
     * Number of unique IPs in this hour
     */
    private Long uniqueIPs;

    /**
     * Total bandwidth consumed in this hour (bytes)
     */
    private Long totalBandwidth;

    /**
     * Number of suspicious requests in this hour
     */
    private Long suspiciousCount;

    /**
     * Number of bot requests in this hour
     */
    private Long botCount;
}
