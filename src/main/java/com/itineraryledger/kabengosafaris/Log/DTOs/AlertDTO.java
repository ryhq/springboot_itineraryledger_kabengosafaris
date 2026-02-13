package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Alert Data Transfer Object
 *
 * Represents an alert triggered by the monitoring system
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDTO {

    /**
     * Unique alert identifier (generated)
     */
    private String alertId;

    /**
     * Type of alert: ERROR_RATE, SLOW_RESPONSE, SECURITY_THREAT, TRAFFIC_SPIKE
     */
    private String alertType;

    /**
     * Severity level: CRITICAL, HIGH, MEDIUM, LOW
     */
    private String severity;

    /**
     * Alert title (short description)
     */
    private String title;

    /**
     * Detailed alert message
     */
    private String message;

    /**
     * When the alert was triggered
     */
    private LocalDateTime triggeredAt;

    /**
     * Additional metadata (e.g., thresholds, values, counts)
     */
    private Map<String, Object> metadata;
}
