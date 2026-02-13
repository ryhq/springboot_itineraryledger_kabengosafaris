package com.itineraryledger.kabengosafaris.Log.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;
import com.itineraryledger.kabengosafaris.Log.Services.AccessLogParserService;
import com.itineraryledger.kabengosafaris.Log.Services.AccessLogSettingGetterServices;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller for security analysis features of access logs
 *
 * Features:
 * - Retrieve detected security threats with filtering
 * - Get suspicious IP addresses
 * - Security summary statistics
 *
 * Permissions:
 * - All endpoints require PERM_READ_LOG permission
 */
@RestController
@RequestMapping("/api/logs/security")
@RequiredArgsConstructor
@Slf4j
public class LogSecurityController {

    @Autowired
    private AccessLogParserService parserService;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private AccessLogSettingGetterServices settings;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Get detected security threats from access logs
     *
     * @param date Date to retrieve threats for (null = today)
     * @param threatType Filter by threat type (SQL_INJECTION, XSS, PATH_TRAVERSAL, COMMAND_INJECTION, SUSPICIOUS_USER_AGENT, SCANNING)
     * @param minThreatScore Filter by minimum threat score (0-100)
     * @return List of access logs with detected threats
     */
    @GetMapping("/threats")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecurityThreats(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false) String threatType,
        @RequestParam(required = false) Integer minThreatScore
    ) {
        log.info("Retrieving security threats - date: {}, threatType: {}, minThreatScore: {}",
            date, threatType, minThreatScore);

        // Validate date
        if (date != null && date.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
            );
        }

        // Validate minThreatScore
        if (minThreatScore != null && (minThreatScore < 0 || minThreatScore > 100)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid threat score: must be 0-100", "INVALID_THREAT_SCORE")
            );
        }

        // Resolve log file path
        Path logFilePath = resolveLogFilePath(date);

        if (!Files.exists(logFilePath)) {
            String message = date == null
                ? "Today's logs not found"
                : "Logs for " + date.format(DATE_FORMATTER) + " not found";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(404, message, "LOG_FILE_NOT_FOUND")
            );
        }

        try (Stream<String> lines = Files.lines(logFilePath)) {
            AtomicLong counter = new AtomicLong(0);

            List<AccessLogDTO> threats = lines
                .map(line -> {
                    long id = counter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    return parserService.parse(line, encodedId);
                })
                .filter(dto -> dto.getIsSuspicious() != null && dto.getIsSuspicious())
                .filter(dto -> threatType == null || (dto.getThreatType() != null && dto.getThreatType().contains(threatType)))
                .filter(dto -> minThreatScore == null || (dto.getThreatScore() != null && dto.getThreatScore() >= minThreatScore))
                .collect(Collectors.toList());

            // Reverse for latest first
            Collections.reverse(threats);

            Map<String, Object> response = Map.of(
                "threats", threats,
                "count", threats.size()
            );

            String message = threats.isEmpty()
                ? "No security threats detected"
                : "Security threats retrieved successfully";

            return ResponseEntity.ok(
                ApiResponse.success(200, message, response)
            );

        } catch (IOException ex) {
            log.error("Error reading log file: {}", logFilePath, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Error reading log file: " + ex.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get suspicious IP addresses from access logs
     * Groups threats by IP address
     *
     * @param date Date to retrieve suspicious IPs for (null = today)
     * @param minThreatScore Filter by minimum threat score (default: 50)
     * @return Map of IP addresses with their associated threats
     */
    @GetMapping("/ips")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuspiciousIPs(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(required = false, defaultValue = "50") Integer minThreatScore
    ) {
        log.info("Retrieving suspicious IPs - date: {}, minThreatScore: {}", date, minThreatScore);

        // Validate date
        if (date != null && date.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
            );
        }

        // Validate minThreatScore
        if (minThreatScore < 0 || minThreatScore > 100) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid threat score: must be 0-100", "INVALID_THREAT_SCORE")
            );
        }

        // Resolve log file path
        Path logFilePath = resolveLogFilePath(date);

        if (!Files.exists(logFilePath)) {
            String message = date == null
                ? "Today's logs not found"
                : "Logs for " + date.format(DATE_FORMATTER) + " not found";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(404, message, "LOG_FILE_NOT_FOUND")
            );
        }

        try (Stream<String> lines = Files.lines(logFilePath)) {
            AtomicLong counter = new AtomicLong(0);

            List<AccessLogDTO> suspiciousLogs = lines
                .map(line -> {
                    long id = counter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    return parserService.parse(line, encodedId);
                })
                .filter(dto -> dto.getIsSuspicious() != null && dto.getIsSuspicious())
                .filter(dto -> dto.getThreatScore() != null && dto.getThreatScore() >= minThreatScore)
                .collect(Collectors.toList());

            // Group by IP address
            Map<String, List<AccessLogDTO>> ipGroups = suspiciousLogs.stream()
                .filter(dto -> dto.getRemoteAddress() != null)
                .collect(Collectors.groupingBy(AccessLogDTO::getRemoteAddress));

            // Calculate statistics per IP
            Map<String, Map<String, Object>> ipStatistics = new LinkedHashMap<>();
            ipGroups.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size())) // Sort by count descending
                .forEach(entry -> {
                    String ip = entry.getKey();
                    List<AccessLogDTO> logs = entry.getValue();

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("threatCount", logs.size());
                    stats.put("threats", logs);

                    // Calculate max threat score
                    OptionalInt maxScore = logs.stream()
                        .filter(dto -> dto.getThreatScore() != null)
                        .mapToInt(AccessLogDTO::getThreatScore)
                        .max();
                    stats.put("maxThreatScore", maxScore.isPresent() ? maxScore.getAsInt() : 0);

                    // Calculate average threat score
                    OptionalDouble avgScore = logs.stream()
                        .filter(dto -> dto.getThreatScore() != null)
                        .mapToInt(AccessLogDTO::getThreatScore)
                        .average();
                    stats.put("avgThreatScore", avgScore.isPresent() ? avgScore.getAsDouble() : 0.0);

                    // Collect unique threat types
                    Set<String> threatTypes = logs.stream()
                        .filter(dto -> dto.getThreatType() != null)
                        .flatMap(dto -> Arrays.stream(dto.getThreatType().split(",\\s*")))
                        .collect(Collectors.toSet());
                    stats.put("threatTypes", threatTypes);

                    ipStatistics.put(ip, stats);
                });

            Map<String, Object> response = Map.of(
                "suspiciousIPs", ipStatistics,
                "totalIPs", ipGroups.size()
            );

            String message = ipGroups.isEmpty()
                ? "No suspicious IPs detected"
                : "Suspicious IPs retrieved successfully";

            return ResponseEntity.ok(
                ApiResponse.success(200, message, response)
            );

        } catch (IOException ex) {
            log.error("Error reading log file: {}", logFilePath, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Error reading log file: " + ex.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get security summary statistics
     *
     * @param date Date to retrieve summary for (null = today)
     * @return Security summary with threat counts and statistics
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSecuritySummary(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        log.info("Retrieving security summary - date: {}", date);

        // Validate date
        if (date != null && date.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
            );
        }

        // Resolve log file path
        Path logFilePath = resolveLogFilePath(date);

        if (!Files.exists(logFilePath)) {
            String message = date == null
                ? "Today's logs not found"
                : "Logs for " + date.format(DATE_FORMATTER) + " not found";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(404, message, "LOG_FILE_NOT_FOUND")
            );
        }

        try (Stream<String> lines = Files.lines(logFilePath)) {
            AtomicLong counter = new AtomicLong(0);

            List<AccessLogDTO> allLogs = lines
                .map(line -> {
                    long id = counter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    return parserService.parse(line, encodedId);
                })
                .collect(Collectors.toList());

            // Filter suspicious logs
            List<AccessLogDTO> suspiciousLogs = allLogs.stream()
                .filter(dto -> dto.getIsSuspicious() != null && dto.getIsSuspicious())
                .collect(Collectors.toList());

            // Calculate statistics
            Map<String, Object> summary = new HashMap<>();

            // Total threats
            summary.put("totalThreats", suspiciousLogs.size());
            summary.put("totalRequests", allLogs.size());
            summary.put("threatPercentage",
                allLogs.isEmpty() ? 0.0 : (suspiciousLogs.size() * 100.0 / allLogs.size()));

            // Unique suspicious IPs
            Set<String> suspiciousIPs = suspiciousLogs.stream()
                .map(AccessLogDTO::getRemoteAddress)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            summary.put("uniqueSuspiciousIPs", suspiciousIPs.size());

            // Threats by type
            Map<String, Long> threatsByType = suspiciousLogs.stream()
                .filter(dto -> dto.getThreatType() != null)
                .flatMap(dto -> Arrays.stream(dto.getThreatType().split(",\\s*")))
                .collect(Collectors.groupingBy(type -> type, Collectors.counting()));
            summary.put("threatsByType", threatsByType);

            // Average threat score
            OptionalDouble avgThreatScore = suspiciousLogs.stream()
                .filter(dto -> dto.getThreatScore() != null)
                .mapToInt(AccessLogDTO::getThreatScore)
                .average();
            summary.put("averageThreatScore",
                avgThreatScore.isPresent() ? avgThreatScore.getAsDouble() : 0.0);

            // Max threat score
            OptionalInt maxThreatScore = suspiciousLogs.stream()
                .filter(dto -> dto.getThreatScore() != null)
                .mapToInt(AccessLogDTO::getThreatScore)
                .max();
            summary.put("maxThreatScore",
                maxThreatScore.isPresent() ? maxThreatScore.getAsInt() : 0);

            // Threat severity distribution
            Map<String, Long> severityDistribution = new HashMap<>();
            severityDistribution.put("CRITICAL (80-100)", suspiciousLogs.stream()
                .filter(dto -> dto.getThreatScore() != null && dto.getThreatScore() >= 80)
                .count());
            severityDistribution.put("HIGH (50-79)", suspiciousLogs.stream()
                .filter(dto -> dto.getThreatScore() != null && dto.getThreatScore() >= 50 && dto.getThreatScore() < 80)
                .count());
            severityDistribution.put("MEDIUM (30-49)", suspiciousLogs.stream()
                .filter(dto -> dto.getThreatScore() != null && dto.getThreatScore() >= 30 && dto.getThreatScore() < 50)
                .count());
            severityDistribution.put("LOW (0-29)", suspiciousLogs.stream()
                .filter(dto -> dto.getThreatScore() != null && dto.getThreatScore() < 30)
                .count());
            summary.put("severityDistribution", severityDistribution);

            // Top 10 most suspicious IPs
            Map<String, Long> ipThreatCounts = suspiciousLogs.stream()
                .filter(dto -> dto.getRemoteAddress() != null)
                .collect(Collectors.groupingBy(AccessLogDTO::getRemoteAddress, Collectors.counting()));

            List<Map.Entry<String, Long>> topIPs = ipThreatCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .collect(Collectors.toList());

            List<Map<String, Object>> topSuspiciousIPs = topIPs.stream()
                .map(entry -> {
                    Map<String, Object> ipInfo = new HashMap<>();
                    ipInfo.put("ip", entry.getKey());
                    ipInfo.put("threatCount", entry.getValue());
                    return ipInfo;
                })
                .collect(Collectors.toList());
            summary.put("topSuspiciousIPs", topSuspiciousIPs);

            return ResponseEntity.ok(
                ApiResponse.success(200, "Security summary retrieved successfully", summary)
            );

        } catch (IOException ex) {
            log.error("Error reading log file: {}", logFilePath, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Error reading log file: " + ex.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Resolve log file path based on date
     */
    private Path resolveLogFilePath(LocalDate date) {
        String directory = settings.getLogDirectory();
        String prefix = settings.getLogPrefix();
        String suffix = settings.getLogSuffix();

        String filename;
        if (date != null) {
            filename = prefix + "." + date.format(DATE_FORMATTER) + suffix;
        } else {
            filename = prefix + "." + LocalDate.now().format(DATE_FORMATTER) + suffix;
        }

        return Paths.get(directory, filename);
    }
}
