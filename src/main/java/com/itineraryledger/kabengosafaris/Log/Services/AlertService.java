package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;
import com.itineraryledger.kabengosafaris.Log.DTOs.AlertDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for detecting and managing alerts in access logs
 *
 * Monitors:
 * - Error rate spikes
 * - Slow response patterns
 * - Security threats
 * - Unusual traffic spikes
 *
 * Note: This service performs in-memory alert detection only.
 * Email/webhook notifications are not yet implemented.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private final AccessLogParserService accessLogParserService;
    private final AccessLogSettingGetterServices accessLogSettingGetterServices;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Default thresholds (can be overridden by settings)
    private static final double DEFAULT_ERROR_RATE_THRESHOLD = 0.10; // 10%
    private static final long DEFAULT_SLOW_RESPONSE_THRESHOLD = 3000L; // 3000ms
    private static final int DEFAULT_SECURITY_THREAT_THRESHOLD = 70; // threat score > 70
    private static final double DEFAULT_TRAFFIC_SPIKE_MULTIPLIER = 2.0; // 200% increase

    /**
     * Check if error rate exceeds threshold in the last hour
     *
     * @param date the date to check
     * @return AlertDTO if threshold exceeded, null otherwise
     */
    public AlertDTO checkErrorRateAlert(LocalDate date) {
        try {
            List<AccessLogDTO> logs = getLogsForDate(date);
            return checkErrorRateAlertFromLogs(logs);
        } catch (Exception e) {
            log.error("Error checking error rate alert for date: {}", date, e);
            return null;
        }
    }

    private AlertDTO checkErrorRateAlertFromLogs(List<AccessLogDTO> logs) {
        try {
            if (logs.isEmpty()) {
                return null;
            }

            // Get logs from last hour
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<AccessLogDTO> recentLogs = logs.stream()
                .filter(log -> log.getTimestampEpoch() != null)
                .filter(log -> {
                    LocalDateTime logTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(log.getTimestampEpoch()),
                        java.time.ZoneId.systemDefault()
                    );
                    return logTime.isAfter(oneHourAgo);
                })
                .collect(Collectors.toList());

            if (recentLogs.isEmpty()) {
                return null;
            }

            long totalRequests = recentLogs.size();
            long errorRequests = recentLogs.stream()
                .filter(log -> log.getStatus() != null && log.getStatus() >= 500)
                .count();

            double errorRate = (double) errorRequests / totalRequests;

            if (errorRate > DEFAULT_ERROR_RATE_THRESHOLD) {
                log.warn("ERROR_RATE alert triggered: {}% error rate ({}/{} requests)",
                    String.format("%.2f", errorRate * 100), errorRequests, totalRequests);

                return AlertDTO.builder()
                    .alertId(UUID.randomUUID().toString())
                    .alertType("ERROR_RATE")
                    .severity(errorRate > 0.25 ? "CRITICAL" : errorRate > 0.15 ? "HIGH" : "MEDIUM")
                    .title("High Error Rate Detected")
                    .message(String.format(
                        "Error rate of %.2f%% exceeds threshold of %.2f%%. " +
                        "%d out of %d requests in the last hour resulted in server errors (5xx).",
                        errorRate * 100,
                        DEFAULT_ERROR_RATE_THRESHOLD * 100,
                        errorRequests,
                        totalRequests
                    ))
                    .triggeredAt(LocalDateTime.now())
                    .metadata(Map.of(
                        "errorRate", errorRate,
                        "threshold", DEFAULT_ERROR_RATE_THRESHOLD,
                        "errorRequests", errorRequests,
                        "totalRequests", totalRequests,
                        "timeWindow", "1 hour"
                    ))
                    .build();
            }

            return null;

        } catch (Exception e) {
            log.error("Error checking error rate alert", e);
            return null;
        }
    }

    /**
     * Check for slow response patterns in the last 10 minutes
     *
     * @param date the date to check
     * @return AlertDTO if threshold exceeded, null otherwise
     */
    public AlertDTO checkSlowResponseAlert(LocalDate date) {
        try {
            List<AccessLogDTO> logs = getLogsForDate(date);
            return checkSlowResponseAlertFromLogs(logs);
        } catch (Exception e) {
            log.error("Error checking slow response alert for date: {}", date, e);
            return null;
        }
    }

    private AlertDTO checkSlowResponseAlertFromLogs(List<AccessLogDTO> logs) {
        try {
            if (logs.isEmpty()) {
                return null;
            }

            // Get logs from last 10 minutes
            LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
            List<AccessLogDTO> recentLogs = logs.stream()
                .filter(log -> log.getTimestampEpoch() != null)
                .filter(log -> log.getTimeTakenMillis() != null)
                .filter(log -> {
                    LocalDateTime logTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(log.getTimestampEpoch()),
                        java.time.ZoneId.systemDefault()
                    );
                    return logTime.isAfter(tenMinutesAgo);
                })
                .collect(Collectors.toList());

            if (recentLogs.isEmpty()) {
                return null;
            }

            // Get threshold from settings or use default
            long threshold = accessLogSettingGetterServices.getSlowRequestThreshold() != null
                ? accessLogSettingGetterServices.getSlowRequestThreshold()
                : DEFAULT_SLOW_RESPONSE_THRESHOLD;

            double avgResponseTime = recentLogs.stream()
                .mapToLong(AccessLogDTO::getTimeTakenMillis)
                .average()
                .orElse(0.0);

            if (avgResponseTime > threshold) {
                log.warn("SLOW_RESPONSE alert triggered: {}ms average response time (threshold: {}ms)",
                    String.format("%.2f", avgResponseTime), threshold);

                return AlertDTO.builder()
                    .alertId(UUID.randomUUID().toString())
                    .alertType("SLOW_RESPONSE")
                    .severity(avgResponseTime > threshold * 2 ? "CRITICAL" : avgResponseTime > threshold * 1.5 ? "HIGH" : "MEDIUM")
                    .title("Slow Response Time Detected")
                    .message(String.format(
                        "Average response time of %.2fms exceeds threshold of %dms. " +
                        "%d requests in the last 10 minutes are experiencing degraded performance.",
                        avgResponseTime,
                        threshold,
                        recentLogs.size()
                    ))
                    .triggeredAt(LocalDateTime.now())
                    .metadata(Map.of(
                        "avgResponseTime", avgResponseTime,
                        "threshold", threshold,
                        "requestCount", recentLogs.size(),
                        "timeWindow", "10 minutes"
                    ))
                    .build();
            }

            return null;

        } catch (Exception e) {
            log.error("Error checking slow response alert", e);
            return null;
        }
    }

    /**
     * Check for critical security threats
     *
     * @param date the date to check
     * @return List of AlertDTOs for each critical threat
     */
    public List<AlertDTO> checkSecurityThreatAlert(LocalDate date) {
        try {
            List<AccessLogDTO> logs = getLogsForDate(date);
            return checkSecurityThreatAlertFromLogs(logs);
        } catch (Exception e) {
            log.error("Error checking security threat alerts for date: {}", date, e);
            return new ArrayList<>();
        }
    }

    private List<AlertDTO> checkSecurityThreatAlertFromLogs(List<AccessLogDTO> logs) {
        List<AlertDTO> alerts = new ArrayList<>();

        try {
            if (logs.isEmpty()) {
                return alerts;
            }

            // Filter for high threat score entries
            List<AccessLogDTO> criticalThreats = logs.stream()
                .filter(log -> log.getThreatScore() != null)
                .filter(log -> log.getThreatScore() > DEFAULT_SECURITY_THREAT_THRESHOLD)
                .collect(Collectors.toList());

            for (AccessLogDTO threat : criticalThreats) {
                log.warn("SECURITY_THREAT alert triggered: threat score {} from {} - {}",
                    threat.getThreatScore(), threat.getRemoteAddress(), threat.getThreatType());

                String severity;
                if (threat.getThreatScore() >= 90) {
                    severity = "CRITICAL";
                } else if (threat.getThreatScore() >= 80) {
                    severity = "HIGH";
                } else {
                    severity = "MEDIUM";
                }

                AlertDTO alert = AlertDTO.builder()
                    .alertId(UUID.randomUUID().toString())
                    .alertType("SECURITY_THREAT")
                    .severity(severity)
                    .title("Security Threat Detected")
                    .message(String.format(
                        "Critical security threat detected from IP %s. " +
                        "Threat type: %s. Threat score: %d/100. " +
                        "Request: %s %s",
                        threat.getRemoteAddress(),
                        threat.getThreatType(),
                        threat.getThreatScore(),
                        threat.getRequestMethod(),
                        threat.getRequestUri()
                    ))
                    .triggeredAt(LocalDateTime.now())
                    .metadata(Map.of(
                        "threatScore", threat.getThreatScore(),
                        "threatType", threat.getThreatType() != null ? threat.getThreatType() : "UNKNOWN",
                        "remoteAddress", threat.getRemoteAddress() != null ? threat.getRemoteAddress() : "UNKNOWN",
                        "requestUri", threat.getRequestUri() != null ? threat.getRequestUri() : "UNKNOWN",
                        "requestMethod", threat.getRequestMethod() != null ? threat.getRequestMethod() : "UNKNOWN",
                        "logId", threat.getLogId() != null ? threat.getLogId() : "UNKNOWN"
                    ))
                    .build();

                alerts.add(alert);
            }

            if (!alerts.isEmpty()) {
                log.info("Total SECURITY_THREAT alerts generated: {}", alerts.size());
            }

        } catch (Exception e) {
            log.error("Error checking security threat alerts", e);
        }

        return alerts;
    }

    /**
     * Check for unusual traffic spikes by comparing current hour to previous hour
     *
     * @param date the date to check
     * @return AlertDTO if spike detected, null otherwise
     */
    public AlertDTO checkUnusualTrafficSpike(LocalDate date) {
        try {
            List<AccessLogDTO> logs = getLogsForDate(date);
            return checkUnusualTrafficSpikeFromLogs(logs);
        } catch (Exception e) {
            log.error("Error checking traffic spike alert for date: {}", date, e);
            return null;
        }
    }

    private AlertDTO checkUnusualTrafficSpikeFromLogs(List<AccessLogDTO> logs) {
        try {
            if (logs.isEmpty()) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime oneHourAgo = now.minusHours(1);
            LocalDateTime twoHoursAgo = now.minusHours(2);

            // Get current hour requests
            long currentHourRequests = logs.stream()
                .filter(log -> log.getTimestampEpoch() != null)
                .filter(log -> {
                    LocalDateTime logTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(log.getTimestampEpoch()),
                        java.time.ZoneId.systemDefault()
                    );
                    return logTime.isAfter(oneHourAgo);
                })
                .count();

            // Get previous hour requests
            long previousHourRequests = logs.stream()
                .filter(log -> log.getTimestampEpoch() != null)
                .filter(log -> {
                    LocalDateTime logTime = LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(log.getTimestampEpoch()),
                        java.time.ZoneId.systemDefault()
                    );
                    return logTime.isAfter(twoHoursAgo) && logTime.isBefore(oneHourAgo);
                })
                .count();

            // Avoid division by zero and require minimum baseline
            if (previousHourRequests < 10) {
                return null; // Not enough baseline data
            }

            double increase = (double) currentHourRequests / previousHourRequests;

            if (increase > DEFAULT_TRAFFIC_SPIKE_MULTIPLIER) {
                double percentageIncrease = (increase - 1.0) * 100;

                log.warn("TRAFFIC_SPIKE alert triggered: {}% increase ({} vs {} requests)",
                    String.format("%.2f", percentageIncrease), currentHourRequests, previousHourRequests);

                return AlertDTO.builder()
                    .alertId(UUID.randomUUID().toString())
                    .alertType("TRAFFIC_SPIKE")
                    .severity(increase > 5.0 ? "CRITICAL" : increase > 3.0 ? "HIGH" : "MEDIUM")
                    .title("Unusual Traffic Spike Detected")
                    .message(String.format(
                        "Traffic increased by %.2f%% in the last hour. " +
                        "Current hour: %d requests, Previous hour: %d requests. " +
                        "This exceeds the threshold of %.0f%% increase.",
                        percentageIncrease,
                        currentHourRequests,
                        previousHourRequests,
                        (DEFAULT_TRAFFIC_SPIKE_MULTIPLIER - 1.0) * 100
                    ))
                    .triggeredAt(LocalDateTime.now())
                    .metadata(Map.of(
                        "currentHourRequests", currentHourRequests,
                        "previousHourRequests", previousHourRequests,
                        "increase", increase,
                        "percentageIncrease", percentageIncrease,
                        "threshold", DEFAULT_TRAFFIC_SPIKE_MULTIPLIER
                    ))
                    .build();
            }

            return null;

        } catch (Exception e) {
            log.error("Error checking traffic spike alert for date: {}", e);
            return null;
        }
    }

    /**
     * Get all triggered alerts for a specific date
     *
     * @param date the date to check
     * @return List of all triggered alerts
     */
    public List<AlertDTO> getAllAlerts(LocalDate date) {
        List<AlertDTO> allAlerts = new ArrayList<>();

        try {
            // Read logs once and pass to all check methods
            List<AccessLogDTO> logs = getLogsForDate(date);
            if (logs.isEmpty()) {
                return allAlerts;
            }

            // Check error rate alert
            AlertDTO errorRateAlert = checkErrorRateAlertFromLogs(logs);
            if (errorRateAlert != null) {
                allAlerts.add(errorRateAlert);
            }

            // Check slow response alert
            AlertDTO slowResponseAlert = checkSlowResponseAlertFromLogs(logs);
            if (slowResponseAlert != null) {
                allAlerts.add(slowResponseAlert);
            }

            // Check security threat alerts
            List<AlertDTO> securityAlerts = checkSecurityThreatAlertFromLogs(logs);
            allAlerts.addAll(securityAlerts);

            // Check traffic spike alert
            AlertDTO trafficSpikeAlert = checkUnusualTrafficSpikeFromLogs(logs);
            if (trafficSpikeAlert != null) {
                allAlerts.add(trafficSpikeAlert);
            }

            if (!allAlerts.isEmpty()) {
                log.info("Total alerts generated for date {}: {}", date, allAlerts.size());
            }

        } catch (Exception e) {
            log.error("Error getting all alerts for date: {}", date, e);
        }

        return allAlerts;
    }

    /**
     * Get logs for a specific date from disk
     *
     * @param date the date to retrieve logs for
     * @return List of parsed AccessLogDTOs
     */
    private List<AccessLogDTO> getLogsForDate(LocalDate date) {
        Path logFilePath = resolveLogFilePath(date);

        if (!Files.exists(logFilePath)) {
            log.debug("Log file not found for date: {}", date);
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(logFilePath)) {
            return lines
                .map(line -> accessLogParserService.parse(line, UUID.randomUUID().toString()))
                .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error reading log file for date: {}", date, e);
            return Collections.emptyList();
        }
    }

    /**
     * Resolve log file path based on date
     *
     * @param date the date to resolve
     * @return Path to the log file
     */
    private Path resolveLogFilePath(LocalDate date) {
        String directory = accessLogSettingGetterServices.getLogDirectory();
        String prefix = accessLogSettingGetterServices.getLogPrefix();
        String suffix = accessLogSettingGetterServices.getLogSuffix();

        String filename;
        if (date != null) {
            filename = prefix + "." + date.format(DATE_FORMATTER) + suffix;
        } else {
            filename = prefix + "." + LocalDate.now().format(DATE_FORMATTER) + suffix;
        }

        return Paths.get(directory, filename);
    }
}
