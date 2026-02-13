package com.itineraryledger.kabengosafaris.Log.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Log.Services.AccessLogService;
import com.itineraryledger.kabengosafaris.Log.Services.LogExportService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;

/**
 * Controller for accessing Tomcat access logs with advanced filtering and analysis
 *
 * Features:
 * - Retrieve paginated logs with 25+ filter options
 * - Security threat detection
 * - Bot detection and classification
 * - Performance monitoring
 * - Export to multiple formats (TEXT, CSV, JSON, EXCEL)
 *
 * Permissions:
 * - All endpoints require PERM_READ_LOG permission
 */
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    @Autowired
    private AccessLogService accessLogService;

    @Autowired
    private LogExportService logExportService;

    /**
     * Get access logs with comprehensive filtering and pagination
     *
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param date Date to retrieve logs for (null = today)
     * @param remoteAddress Filter by remote IP address
     * @param localAddress Filter by local IP address
     * @param localPort Filter by local port
     * @param remoteHost Filter by remote host
     * @param requestMethod Filter by HTTP method (GET, POST, etc.)
     * @param requestUri Filter by request URI (partial match)
     * @param status Filter by HTTP status code
     * @param statusCategory Filter by status category (2xx, 3xx, 4xx, 5xx)
     * @param responseSizeBytes Filter by response size in bytes
     * @param responseSizeBytesArgument Comparison operator for response size
     * @param timeTakenMillis Filter by time taken in milliseconds
     * @param timeTakenMillisArgument Comparison operator for time taken
     * @param userAgent Filter by user agent (partial match)
     * @param referer Filter by referer (partial match)
     * @param xForwardedFor Filter by X-Forwarded-For header
     * @param host Filter by Host header
     * @param isSuspicious Filter by suspicious flag
     * @param threatType Filter by threat type (SQL_INJECTION, XSS, etc.)
     * @param minThreatScore Filter by minimum threat score (0-100)
     * @param isBot Filter by bot flag
     * @param botType Filter by bot type (SEARCH_ENGINE, SCRAPER, etc.)
     * @param isSlowRequest Filter by slow request flag
     * @param performanceGrade Filter by performance grade (A, B, C, D, F)
     * @param browserName Filter by browser name
     * @param operatingSystem Filter by operating system
     * @param deviceType Filter by device type (DESKTOP, MOBILE, TABLET)
     * @return ResponseEntity with paginated logs and summary statistics
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<?> getLogs(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String remoteAddress,
        @RequestParam(required = false) String localAddress,
        @RequestParam(required = false) Integer localPort,
        @RequestParam(required = false) String remoteHost,
        @RequestParam(required = false) String requestMethod,
        @RequestParam(required = false) String requestUri,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String statusCategory,
        @RequestParam(required = false) Long responseSizeBytes,
        @RequestParam(required = false) String responseSizeBytesArgument,
        @RequestParam(required = false) Long timeTakenMillis,
        @RequestParam(required = false) String timeTakenMillisArgument,
        @RequestParam(required = false) String userAgent,
        @RequestParam(required = false) String referer,
        @RequestParam(required = false) String xForwardedFor,
        @RequestParam(required = false) String host,
        @RequestParam(required = false) Boolean isSuspicious,
        @RequestParam(required = false) String threatType,
        @RequestParam(required = false) Integer minThreatScore,
        @RequestParam(required = false) Boolean isBot,
        @RequestParam(required = false) String botType,
        @RequestParam(required = false) Boolean isSlowRequest,
        @RequestParam(required = false) String performanceGrade,
        @RequestParam(required = false) String browserName,
        @RequestParam(required = false) String operatingSystem,
        @RequestParam(required = false) String deviceType
    ) {
        log.info("Retrieving access logs - page: {}, size: {}, date: {}", page, size, date);

        return accessLogService.getLogs(
            page, size, date,
            remoteAddress, localAddress, localPort, remoteHost,
            requestMethod, requestUri, status, statusCategory,
            responseSizeBytes, responseSizeBytesArgument,
            timeTakenMillis, timeTakenMillisArgument,
            userAgent, referer, xForwardedFor, host,
            isSuspicious, threatType, minThreatScore,
            isBot, botType,
            isSlowRequest, performanceGrade,
            browserName, operatingSystem, deviceType
        );
    }

    /**
     * Export logs in specified format with comprehensive filtering
     * Uses the same 25+ filter parameters as the main getLogs endpoint
     *
     * @param format Export format (TEXT, CSV, JSON, EXCEL)
     * @param date Date to export (null = today)
     * @param remoteAddress Filter by remote IP address
     * @param localAddress Filter by local IP address
     * @param localPort Filter by local port
     * @param remoteHost Filter by remote host
     * @param requestMethod Filter by HTTP method (GET, POST, etc.)
     * @param requestUri Filter by request URI (partial match)
     * @param status Filter by HTTP status code
     * @param statusCategory Filter by status category (2xx, 3xx, 4xx, 5xx)
     * @param responseSizeBytes Filter by response size in bytes
     * @param responseSizeBytesArgument Comparison operator for response size
     * @param timeTakenMillis Filter by time taken in milliseconds
     * @param timeTakenMillisArgument Comparison operator for time taken
     * @param userAgent Filter by user agent (partial match)
     * @param referer Filter by referer (partial match)
     * @param xForwardedFor Filter by X-Forwarded-For header
     * @param host Filter by Host header
     * @param isSuspicious Filter by suspicious flag
     * @param threatType Filter by threat type (SQL_INJECTION, XSS, etc.)
     * @param minThreatScore Filter by minimum threat score (0-100)
     * @param isBot Filter by bot flag
     * @param botType Filter by bot type (SEARCH_ENGINE, SCRAPER, etc.)
     * @param isSlowRequest Filter by slow request flag
     * @param performanceGrade Filter by performance grade (A, B, C, D, F)
     * @param browserName Filter by browser name
     * @param operatingSystem Filter by operating system
     * @param deviceType Filter by device type (DESKTOP, MOBILE, TABLET)
     * @return ResponseEntity with file download
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<Resource> exportLogs(
        @RequestParam String format,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String remoteAddress,
        @RequestParam(required = false) String localAddress,
        @RequestParam(required = false) Integer localPort,
        @RequestParam(required = false) String remoteHost,
        @RequestParam(required = false) String requestMethod,
        @RequestParam(required = false) String requestUri,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) String statusCategory,
        @RequestParam(required = false) Long responseSizeBytes,
        @RequestParam(required = false) String responseSizeBytesArgument,
        @RequestParam(required = false) Long timeTakenMillis,
        @RequestParam(required = false) String timeTakenMillisArgument,
        @RequestParam(required = false) String userAgent,
        @RequestParam(required = false) String referer,
        @RequestParam(required = false) String xForwardedFor,
        @RequestParam(required = false) String host,
        @RequestParam(required = false) Boolean isSuspicious,
        @RequestParam(required = false) String threatType,
        @RequestParam(required = false) Integer minThreatScore,
        @RequestParam(required = false) Boolean isBot,
        @RequestParam(required = false) String botType,
        @RequestParam(required = false) Boolean isSlowRequest,
        @RequestParam(required = false) String performanceGrade,
        @RequestParam(required = false) String browserName,
        @RequestParam(required = false) String operatingSystem,
        @RequestParam(required = false) String deviceType
    ) {
        log.info("Exporting access logs - format: {}, date: {}", format, date);

        return logExportService.exportLogs(
            format, date,
            remoteAddress, localAddress, localPort, remoteHost,
            requestMethod, requestUri, status, statusCategory,
            responseSizeBytes, responseSizeBytesArgument,
            timeTakenMillis, timeTakenMillisArgument,
            userAgent, referer, xForwardedFor, host,
            isSuspicious, threatType, minThreatScore,
            isBot, botType,
            isSlowRequest, performanceGrade,
            browserName, operatingSystem, deviceType
        );
    }

    /**
     * Handle requests for individual log retrieval by ID
     *
     * Log IDs are generated on-the-fly during file parsing and are only valid
     * within the context of a paginated response. They cannot be used to retrieve
     * individual logs directly because:
     * 1. IDs are not persisted - they're calculated during each request
     * 2. IDs represent line numbers in the log file, which change with rotation
     * 3. Retrieving by ID would require parsing the entire file
     *
     * @param id The obfuscated log ID
     * @return Error response explaining the limitation
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Void>> getLogById(@PathVariable String id) {
        log.warn("Attempted to retrieve individual log by ID: {}", id);

        return ResponseEntity.status(404).body(
            ApiResponse.error(
                404,
                "Individual log retrieval is not supported. Log IDs are for reference only within paginated responses. " +
                "Please use the paginated logs endpoint (GET /api/logs) with appropriate filters to find specific logs.",
                "LOG_BY_ID_NOT_SUPPORTED"
            )
        );
    }
}
