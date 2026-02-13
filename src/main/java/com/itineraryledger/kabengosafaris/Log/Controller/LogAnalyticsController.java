package com.itineraryledger.kabengosafaris.Log.Controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Log.DTOs.*;
import com.itineraryledger.kabengosafaris.Log.Services.LogAnalyticsService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for access log analytics
 *
 * Provides analytics endpoints for:
 * - Dashboard overview with key metrics
 * - Endpoint usage statistics
 * - Hourly traffic patterns
 * - Geographic distribution
 * - User agent analysis
 * - Error rate analysis
 *
 * All endpoints require PERM_READ_LOG permission
 */
@RestController
@RequestMapping("/api/logs/analytics")
@RequiredArgsConstructor
@Slf4j
public class LogAnalyticsController {

    private final LogAnalyticsService analyticsService;

    /**
     * Get dashboard overview with key metrics
     *
     * Provides high-level summary including:
     * - Total requests
     * - Unique IPs
     * - Error rate
     * - Average response time
     * - Top endpoint
     * - Security metrics (suspicious requests, bots)
     *
     * @param date Date to analyze (optional, defaults to today)
     * @return Dashboard overview metrics
     */
    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<DashboardOverviewDTO>> getDashboardOverview(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            // Validate date
            if (date != null && date.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
                );
            }

            log.info("Retrieving dashboard overview - date: {}", date != null ? date : "today");

            DashboardOverviewDTO overview = analyticsService.getDashboardOverview(date);

            if (overview.getTotalRequests() == 0) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "No logs found for the specified date: " +
                        (date != null ? date.toString() : LocalDate.now().toString()), "NO_LOGS_FOUND")
                );
            }

            return ResponseEntity.ok(
                ApiResponse.success(200, "Dashboard overview retrieved successfully", overview)
            );

        } catch (Exception e) {
            log.error("Error generating dashboard overview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to generate dashboard overview: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get endpoint statistics for top N endpoints
     *
     * Provides detailed usage statistics per endpoint including:
     * - Request count
     * - Response time metrics (avg, min, max)
     * - Error rate
     * - Bandwidth usage
     * - Unique IP count
     *
     * @param date Date to analyze (optional, defaults to today)
     * @param limit Maximum number of endpoints to return (optional, defaults to 20)
     * @return List of endpoint statistics ordered by request count
     */
    @GetMapping("/endpoints")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEndpointStatistics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "20") int limit
    ) {
        try {
            // Validate date
            if (date != null && date.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
                );
            }

            // Validate limit
            if (limit < 1 || limit > 100) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Limit must be between 1 and 100", "INVALID_LIMIT")
                );
            }

            log.info("Retrieving endpoint statistics - date: {}, limit: {}", date != null ? date : "today", limit);

            List<EndpointAnalyticsDTO> endpoints = analyticsService.getEndpointStatistics(date, limit);

            if (endpoints.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "No endpoint data found for the specified date: " +
                        (date != null ? date.toString() : LocalDate.now().toString()), "NO_DATA_FOUND")
                );
            }

            Map<String, Object> response = Map.of(
                "endpoints", endpoints,
                "count", endpoints.size(),
                "date", date != null ? date.toString() : LocalDate.now().toString()
            );

            return ResponseEntity.ok(
                ApiResponse.success(200, "Endpoint statistics retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error generating endpoint statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to generate endpoint statistics: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get hourly traffic pattern metrics
     *
     * Provides 24-hour traffic analysis including:
     * - Request count per hour
     * - Average response time
     * - Error rate
     * - Unique IP count
     * - Security metrics
     *
     * @param date Date to analyze (optional, defaults to today)
     * @return List of hourly metrics (0-23 hours)
     */
    @GetMapping("/hourly")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHourlyMetrics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            // Validate date
            if (date != null && date.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
                );
            }

            log.info("Retrieving hourly metrics - date: {}", date != null ? date : "today");

            List<HourlyMetricsDTO> hourlyMetrics = analyticsService.getHourlyMetrics(date);

            Map<String, Object> response = Map.of(
                "hourlyMetrics", hourlyMetrics,
                "date", date != null ? date.toString() : LocalDate.now().toString()
            );

            return ResponseEntity.ok(
                ApiResponse.success(200, "Hourly metrics retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error generating hourly metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to generate hourly metrics: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get geographic distribution of requests
     *
     * Provides country-based analysis including:
     * - Request count per country
     * - Percentage of total requests
     * - Unique IP count
     * - Average response time
     * - Security metrics
     *
     * Note: Returns "Unknown" for all requests if GeoIP is not enabled
     *
     * @param date Date to analyze (optional, defaults to today)
     * @return List of geographic distribution metrics
     */
    @GetMapping("/geographic")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGeographicDistribution(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            // Validate date
            if (date != null && date.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
                );
            }

            log.info("Retrieving geographic distribution - date: {}", date != null ? date : "today");

            List<GeographicDistributionDTO> distribution = analyticsService.getGeographicDistribution(date);

            if (distribution.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "No geographic data found for the specified date: " +
                        (date != null ? date.toString() : LocalDate.now().toString()), "NO_DATA_FOUND")
                );
            }

            Map<String, Object> response = Map.of(
                "distribution", distribution,
                "count", distribution.size(),
                "date", date != null ? date.toString() : LocalDate.now().toString(),
                "note", "Geographic data requires GeoIP database. Returns 'Unknown' if not enabled."
            );

            return ResponseEntity.ok(
                ApiResponse.success(200, "Geographic distribution retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error generating geographic distribution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to generate geographic distribution: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get user agent statistics
     *
     * Provides browser and operating system analysis including:
     * - Request count per browser/OS
     * - Percentage of total requests
     * - Device type distribution
     * - Unique IP count
     *
     * @param date Date to analyze (optional, defaults to today)
     * @return List of user agent statistics
     */
    @GetMapping("/user-agents")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserAgentStatistics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            // Validate date
            if (date != null && date.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
                );
            }

            log.info("Retrieving user agent statistics - date: {}", date != null ? date : "today");

            List<UserAgentStatisticsDTO> statistics = analyticsService.getUserAgentStatistics(date);

            if (statistics.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "No user agent data found for the specified date: " +
                        (date != null ? date.toString() : LocalDate.now().toString()), "NO_DATA_FOUND")
                );
            }

            // Separate browsers and OS
            List<UserAgentStatisticsDTO> browsers = statistics.stream()
                .filter(stat -> "BROWSER".equals(stat.getType()))
                .toList();

            List<UserAgentStatisticsDTO> operatingSystems = statistics.stream()
                .filter(stat -> "OS".equals(stat.getType()))
                .toList();

            Map<String, Object> response = Map.of(
                "browsers", browsers,
                "operatingSystems", operatingSystems,
                "date", date != null ? date.toString() : LocalDate.now().toString()
            );

            return ResponseEntity.ok(
                ApiResponse.success(200, "User agent statistics retrieved successfully", response)
            );

        } catch (Exception e) {
            log.error("Error generating user agent statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to generate user agent statistics: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get error analysis
     *
     * Provides detailed error analysis including:
     * - Error count by status code and endpoint
     * - Percentage of total errors
     * - Status category (4xx, 5xx)
     * - Unique IP count
     *
     * @param date Date to analyze (optional, defaults to today)
     * @return List of error analysis metrics
     */
    @GetMapping("/errors")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getErrorAnalysis(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        try {
            // Validate date
            if (date != null && date.isAfter(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
                );
            }

            log.info("Retrieving error analysis - date: {}", date != null ? date : "today");

            List<ErrorAnalysisDTO> errors = analyticsService.getErrorAnalysis(date);

            // Separate by status category
            List<ErrorAnalysisDTO> clientErrors = errors.stream()
                .filter(error -> "4xx".equals(error.getStatusCategory()))
                .toList();

            List<ErrorAnalysisDTO> serverErrors = errors.stream()
                .filter(error -> "5xx".equals(error.getStatusCategory()))
                .toList();

            Map<String, Object> response = Map.of(
                "clientErrors", clientErrors,
                "serverErrors", serverErrors,
                "totalErrors", errors.size(),
                "date", date != null ? date.toString() : LocalDate.now().toString()
            );

            String message = errors.isEmpty()
                ? "No errors found for the specified date"
                : "Error analysis retrieved successfully";

            return ResponseEntity.ok(
                ApiResponse.success(200, message, response)
            );

        } catch (Exception e) {
            log.error("Error generating error analysis", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to generate error analysis: " + e.getMessage(), "INTERNAL_ERROR")
            );
        }
    }
}
