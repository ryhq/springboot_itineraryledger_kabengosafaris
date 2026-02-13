package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO for error analysis
 * Provides error rate analysis by endpoint and status code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorAnalysisDTO {

    /**
     * HTTP status code
     */
    private Integer statusCode;

    /**
     * Endpoint/URI where errors occurred
     */
    private String endpoint;

    /**
     * Number of errors with this status code
     */
    private Long errorCount;

    /**
     * Percentage of total errors
     */
    private Double percentage;

    /**
     * Status category (4xx, 5xx)
     */
    private String statusCategory;

    /**
     * Most common error message or pattern
     */
    private String errorPattern;

    /**
     * Number of unique IPs encountering this error
     */
    private Long uniqueIPs;
}
