package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.*;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for generating analytics from access logs
 *
 * Provides aggregated metrics and insights including:
 * - Dashboard overview with key metrics
 * - Endpoint usage statistics
 * - Hourly traffic patterns
 * - Geographic distribution
 * - User agent analysis
 * - Error rate analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogAnalyticsService {

    private final AccessLogParserService parserService;
    private final AccessLogSettingGetterServices settings;
    private final IdObfuscator idObfuscator;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    /**
     * Get dashboard overview metrics for a specific date
     *
     * @param date Date to analyze (defaults to today if null)
     * @return Dashboard overview with key metrics
     */
    public DashboardOverviewDTO getDashboardOverview(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("Generating dashboard overview for date: {}", date);

        List<AccessLogDTO> logs = readAndParseLogs(date);

        if (logs.isEmpty()) {
            return createEmptyOverview(date);
        }

        long totalRequests = logs.size();
        long uniqueIPs = logs.stream()
            .map(AccessLogDTO::getRemoteAddress)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        long successfulRequests = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsSuccess()))
            .count();

        long clientErrors = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsClientError()))
            .count();

        long serverErrors = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsServerError()))
            .count();

        long totalErrors = clientErrors + serverErrors;
        double errorRate = (totalRequests > 0) ? (totalErrors * 100.0 / totalRequests) : 0.0;

        double avgResponseTime = logs.stream()
            .filter(dto -> dto.getTimeTakenMillis() != null)
            .mapToLong(AccessLogDTO::getTimeTakenMillis)
            .average()
            .orElse(0.0);

        double avgResponseSize = logs.stream()
            .filter(dto -> dto.getResponseSizeBytes() != null)
            .mapToLong(AccessLogDTO::getResponseSizeBytes)
            .average()
            .orElse(0.0);

        long totalBandwidth = logs.stream()
            .filter(dto -> dto.getResponseSizeBytes() != null)
            .mapToLong(AccessLogDTO::getResponseSizeBytes)
            .sum();

        long suspiciousRequests = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsSuspicious()))
            .count();

        long botRequests = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsBot()))
            .count();

        long slowRequests = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsSlowRequest()))
            .count();

        // Find top endpoint
        Map<String, Long> endpointCounts = logs.stream()
            .filter(dto -> dto.getRequestUri() != null)
            .collect(Collectors.groupingBy(AccessLogDTO::getRequestUri, Collectors.counting()));

        Map.Entry<String, Long> topEndpoint = endpointCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        return DashboardOverviewDTO.builder()
            .totalRequests(totalRequests)
            .uniqueIPs(uniqueIPs)
            .errorRate(Math.round(errorRate * 100.0) / 100.0)
            .avgResponseTime(Math.round(avgResponseTime * 100.0) / 100.0)
            .successfulRequests(successfulRequests)
            .clientErrors(clientErrors)
            .serverErrors(serverErrors)
            .suspiciousRequests(suspiciousRequests)
            .botRequests(botRequests)
            .slowRequests(slowRequests)
            .avgResponseSize(Math.round(avgResponseSize * 100.0) / 100.0)
            .totalBandwidth(totalBandwidth)
            .topEndpoint(topEndpoint != null ? topEndpoint.getKey() : null)
            .topEndpointCount(topEndpoint != null ? topEndpoint.getValue() : 0L)
            .date(date.format(DATE_FORMATTER))
            .build();
    }

    /**
     * Get endpoint statistics for top N endpoints
     *
     * @param date Date to analyze (defaults to today if null)
     * @param limit Maximum number of endpoints to return
     * @return List of endpoint analytics ordered by request count
     */
    public List<EndpointAnalyticsDTO> getEndpointStatistics(LocalDate date, int limit) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("Generating endpoint statistics for date: {}, limit: {}", date, limit);

        List<AccessLogDTO> logs = readAndParseLogs(date);

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // Group by endpoint
        Map<String, List<AccessLogDTO>> endpointGroups = logs.stream()
            .filter(dto -> dto.getRequestUri() != null)
            .collect(Collectors.groupingBy(AccessLogDTO::getRequestUri));

        // Calculate statistics for each endpoint
        return endpointGroups.entrySet().stream()
            .map(entry -> {
                String endpoint = entry.getKey();
                List<AccessLogDTO> endpointLogs = entry.getValue();

                long requestCount = endpointLogs.size();

                double avgResponseTime = endpointLogs.stream()
                    .filter(dto -> dto.getTimeTakenMillis() != null)
                    .mapToLong(AccessLogDTO::getTimeTakenMillis)
                    .average()
                    .orElse(0.0);

                Long minResponseTime = endpointLogs.stream()
                    .filter(dto -> dto.getTimeTakenMillis() != null)
                    .mapToLong(AccessLogDTO::getTimeTakenMillis)
                    .min()
                    .orElse(0L);

                Long maxResponseTime = endpointLogs.stream()
                    .filter(dto -> dto.getTimeTakenMillis() != null)
                    .mapToLong(AccessLogDTO::getTimeTakenMillis)
                    .max()
                    .orElse(0L);

                long successCount = endpointLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsSuccess()))
                    .count();

                long errorCount = endpointLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsClientError()) || Boolean.TRUE.equals(dto.getIsServerError()))
                    .count();

                double errorRate = (requestCount > 0) ? (errorCount * 100.0 / requestCount) : 0.0;

                double avgResponseSize = endpointLogs.stream()
                    .filter(dto -> dto.getResponseSizeBytes() != null)
                    .mapToLong(AccessLogDTO::getResponseSizeBytes)
                    .average()
                    .orElse(0.0);

                long totalBandwidth = endpointLogs.stream()
                    .filter(dto -> dto.getResponseSizeBytes() != null)
                    .mapToLong(AccessLogDTO::getResponseSizeBytes)
                    .sum();

                long uniqueIPs = endpointLogs.stream()
                    .map(AccessLogDTO::getRemoteAddress)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

                String mostCommonMethod = endpointLogs.stream()
                    .filter(dto -> dto.getRequestMethod() != null)
                    .collect(Collectors.groupingBy(AccessLogDTO::getRequestMethod, Collectors.counting()))
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);

                long suspiciousCount = endpointLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsSuspicious()))
                    .count();

                return EndpointAnalyticsDTO.builder()
                    .endpoint(endpoint)
                    .requestCount(requestCount)
                    .avgResponseTime(Math.round(avgResponseTime * 100.0) / 100.0)
                    .minResponseTime(minResponseTime)
                    .maxResponseTime(maxResponseTime)
                    .successCount(successCount)
                    .errorCount(errorCount)
                    .errorRate(Math.round(errorRate * 100.0) / 100.0)
                    .avgResponseSize(Math.round(avgResponseSize * 100.0) / 100.0)
                    .totalBandwidth(totalBandwidth)
                    .uniqueIPs(uniqueIPs)
                    .mostCommonMethod(mostCommonMethod)
                    .suspiciousCount(suspiciousCount)
                    .build();
            })
            .sorted(Comparator.comparingLong(EndpointAnalyticsDTO::getRequestCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Get hourly metrics showing 24-hour traffic pattern
     *
     * @param date Date to analyze (defaults to today if null)
     * @return List of hourly metrics for each hour (0-23)
     */
    public List<HourlyMetricsDTO> getHourlyMetrics(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("Generating hourly metrics for date: {}", date);

        List<AccessLogDTO> logs = readAndParseLogs(date);

        // Initialize all 24 hours with zero values
        Map<Integer, List<AccessLogDTO>> hourlyGroups = new HashMap<>();
        for (int i = 0; i < 24; i++) {
            hourlyGroups.put(i, new ArrayList<>());
        }

        // Group logs by hour
        logs.forEach(dto -> {
            if (dto.getTimestamp() != null) {
                try {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(dto.getTimestamp(), TIMESTAMP_FORMATTER);
                    int hour = zonedDateTime.getHour();
                    hourlyGroups.get(hour).add(dto);
                } catch (Exception e) {
                    log.debug("Failed to parse timestamp for hourly grouping: {}", dto.getTimestamp());
                }
            }
        });

        // Calculate metrics for each hour
        return hourlyGroups.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> {
                Integer hour = entry.getKey();
                List<AccessLogDTO> hourLogs = entry.getValue();

                long requestCount = hourLogs.size();

                double avgResponseTime = hourLogs.stream()
                    .filter(dto -> dto.getTimeTakenMillis() != null)
                    .mapToLong(AccessLogDTO::getTimeTakenMillis)
                    .average()
                    .orElse(0.0);

                long successCount = hourLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsSuccess()))
                    .count();

                long errorCount = hourLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsClientError()) || Boolean.TRUE.equals(dto.getIsServerError()))
                    .count();

                double errorRate = (requestCount > 0) ? (errorCount * 100.0 / requestCount) : 0.0;

                long uniqueIPs = hourLogs.stream()
                    .map(AccessLogDTO::getRemoteAddress)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

                long totalBandwidth = hourLogs.stream()
                    .filter(dto -> dto.getResponseSizeBytes() != null)
                    .mapToLong(AccessLogDTO::getResponseSizeBytes)
                    .sum();

                long suspiciousCount = hourLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsSuspicious()))
                    .count();

                long botCount = hourLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsBot()))
                    .count();

                return HourlyMetricsDTO.builder()
                    .hour(hour)
                    .requestCount(requestCount)
                    .avgResponseTime(Math.round(avgResponseTime * 100.0) / 100.0)
                    .successCount(successCount)
                    .errorCount(errorCount)
                    .errorRate(Math.round(errorRate * 100.0) / 100.0)
                    .uniqueIPs(uniqueIPs)
                    .totalBandwidth(totalBandwidth)
                    .suspiciousCount(suspiciousCount)
                    .botCount(botCount)
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Get geographic distribution of requests by country
     *
     * @param date Date to analyze (defaults to today if null)
     * @return List of geographic distribution metrics
     */
    public List<GeographicDistributionDTO> getGeographicDistribution(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("Generating geographic distribution for date: {}", date);

        List<AccessLogDTO> logs = readAndParseLogs(date);

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        long totalRequests = logs.size();

        // Group by country code (placeholder - returns "Unknown" if GeoIP not enabled)
        Map<String, List<AccessLogDTO>> countryGroups = logs.stream()
            .collect(Collectors.groupingBy(dto -> {
                if (dto.getCountryCode() != null) {
                    return dto.getCountryCode();
                }
                return "Unknown";
            }));

        return countryGroups.entrySet().stream()
            .map(entry -> {
                String countryCode = entry.getKey();
                List<AccessLogDTO> countryLogs = entry.getValue();

                long requestCount = countryLogs.size();
                double percentage = (totalRequests > 0) ? (requestCount * 100.0 / totalRequests) : 0.0;

                long uniqueIPs = countryLogs.stream()
                    .map(AccessLogDTO::getRemoteAddress)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

                double avgResponseTime = countryLogs.stream()
                    .filter(dto -> dto.getTimeTakenMillis() != null)
                    .mapToLong(AccessLogDTO::getTimeTakenMillis)
                    .average()
                    .orElse(0.0);

                long suspiciousCount = countryLogs.stream()
                    .filter(dto -> Boolean.TRUE.equals(dto.getIsSuspicious()))
                    .count();

                String countryName = countryLogs.stream()
                    .map(AccessLogDTO::getCountryName)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(countryCode.equals("Unknown") ? "Unknown" : null);

                return GeographicDistributionDTO.builder()
                    .countryCode(countryCode)
                    .countryName(countryName)
                    .requestCount(requestCount)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .uniqueIPs(uniqueIPs)
                    .avgResponseTime(Math.round(avgResponseTime * 100.0) / 100.0)
                    .suspiciousCount(suspiciousCount)
                    .build();
            })
            .sorted(Comparator.comparingLong(GeographicDistributionDTO::getRequestCount).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get user agent statistics (browser and OS distribution)
     *
     * @param date Date to analyze (defaults to today if null)
     * @return List of user agent statistics
     */
    public List<UserAgentStatisticsDTO> getUserAgentStatistics(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("Generating user agent statistics for date: {}", date);

        List<AccessLogDTO> logs = readAndParseLogs(date);

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        long totalRequests = logs.size();
        List<UserAgentStatisticsDTO> results = new ArrayList<>();

        // Browser statistics
        Map<String, List<AccessLogDTO>> browserGroups = logs.stream()
            .filter(dto -> dto.getBrowserName() != null)
            .collect(Collectors.groupingBy(AccessLogDTO::getBrowserName));

        browserGroups.forEach((browser, browserLogs) -> {
            long requestCount = browserLogs.size();
            double percentage = (totalRequests > 0) ? (requestCount * 100.0 / totalRequests) : 0.0;

            long uniqueIPs = browserLogs.stream()
                .map(AccessLogDTO::getRemoteAddress)
                .filter(Objects::nonNull)
                .distinct()
                .count();

            String mostCommonDeviceType = browserLogs.stream()
                .filter(dto -> dto.getDeviceType() != null)
                .collect(Collectors.groupingBy(AccessLogDTO::getDeviceType, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            results.add(UserAgentStatisticsDTO.builder()
                .name(browser)
                .type("BROWSER")
                .requestCount(requestCount)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .deviceType(mostCommonDeviceType)
                .uniqueIPs(uniqueIPs)
                .build());
        });

        // Operating system statistics
        Map<String, List<AccessLogDTO>> osGroups = logs.stream()
            .filter(dto -> dto.getOperatingSystem() != null)
            .collect(Collectors.groupingBy(AccessLogDTO::getOperatingSystem));

        osGroups.forEach((os, osLogs) -> {
            long requestCount = osLogs.size();
            double percentage = (totalRequests > 0) ? (requestCount * 100.0 / totalRequests) : 0.0;

            long uniqueIPs = osLogs.stream()
                .map(AccessLogDTO::getRemoteAddress)
                .filter(Objects::nonNull)
                .distinct()
                .count();

            String mostCommonDeviceType = osLogs.stream()
                .filter(dto -> dto.getDeviceType() != null)
                .collect(Collectors.groupingBy(AccessLogDTO::getDeviceType, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            results.add(UserAgentStatisticsDTO.builder()
                .name(os)
                .type("OS")
                .requestCount(requestCount)
                .percentage(Math.round(percentage * 100.0) / 100.0)
                .deviceType(mostCommonDeviceType)
                .uniqueIPs(uniqueIPs)
                .build());
        });

        return results.stream()
            .sorted(Comparator.comparingLong(UserAgentStatisticsDTO::getRequestCount).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Get error analysis by status code and endpoint
     *
     * @param date Date to analyze (defaults to today if null)
     * @return List of error analysis metrics
     */
    public List<ErrorAnalysisDTO> getErrorAnalysis(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        log.info("Generating error analysis for date: {}", date);

        List<AccessLogDTO> logs = readAndParseLogs(date);

        // Filter only errors (4xx and 5xx)
        List<AccessLogDTO> errorLogs = logs.stream()
            .filter(dto -> Boolean.TRUE.equals(dto.getIsClientError()) || Boolean.TRUE.equals(dto.getIsServerError()))
            .collect(Collectors.toList());

        if (errorLogs.isEmpty()) {
            return Collections.emptyList();
        }

        long totalErrors = errorLogs.size();

        // Group by status code and endpoint
        Map<String, List<AccessLogDTO>> errorGroups = errorLogs.stream()
            .collect(Collectors.groupingBy(dto -> {
                Integer status = dto.getStatus();
                String endpoint = dto.getRequestUri();
                return (status != null ? status : 0) + "|" + (endpoint != null ? endpoint : "Unknown");
            }));

        return errorGroups.entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split("\\|", 2);
                Integer statusCode = Integer.parseInt(parts[0]);
                String endpoint = parts[1];
                List<AccessLogDTO> errorGroupLogs = entry.getValue();

                long errorCount = errorGroupLogs.size();
                double percentage = (totalErrors > 0) ? (errorCount * 100.0 / totalErrors) : 0.0;

                String statusCategory = statusCode >= 500 ? "5xx" : "4xx";

                long uniqueIPs = errorGroupLogs.stream()
                    .map(AccessLogDTO::getRemoteAddress)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

                return ErrorAnalysisDTO.builder()
                    .statusCode(statusCode)
                    .endpoint(endpoint)
                    .errorCount(errorCount)
                    .percentage(Math.round(percentage * 100.0) / 100.0)
                    .statusCategory(statusCategory)
                    .errorPattern(null) // Could be enhanced with pattern matching
                    .uniqueIPs(uniqueIPs)
                    .build();
            })
            .sorted(Comparator.comparingLong(ErrorAnalysisDTO::getErrorCount).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Read and parse logs for a specific date
     */
    private List<AccessLogDTO> readAndParseLogs(LocalDate date) {
        Path logFilePath = resolveLogFilePath(date);

        if (!Files.exists(logFilePath)) {
            log.warn("Log file not found: {}", logFilePath);
            return Collections.emptyList();
        }

        try (Stream<String> lines = Files.lines(logFilePath)) {
            AtomicLong counter = new AtomicLong(0);

            return lines
                .map(line -> {
                    long id = counter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    return parserService.parse(line, encodedId);
                })
                .collect(Collectors.toList());

        } catch (IOException ex) {
            log.error("Error reading log file: {}", logFilePath, ex);
            return Collections.emptyList();
        }
    }

    /**
     * Resolve log file path based on date
     */
    private Path resolveLogFilePath(LocalDate date) {
        String directory = settings.getLogDirectory();
        String prefix = settings.getLogPrefix();
        String suffix = settings.getLogSuffix();

        String filename = prefix + "." + date.format(DATE_FORMATTER) + suffix;
        return Paths.get(directory, filename);
    }

    /**
     * Create empty overview when no logs exist
     */
    private DashboardOverviewDTO createEmptyOverview(LocalDate date) {
        return DashboardOverviewDTO.builder()
            .totalRequests(0L)
            .uniqueIPs(0L)
            .errorRate(0.0)
            .avgResponseTime(0.0)
            .successfulRequests(0L)
            .clientErrors(0L)
            .serverErrors(0L)
            .suspiciousRequests(0L)
            .botRequests(0L)
            .slowRequests(0L)
            .avgResponseSize(0.0)
            .totalBandwidth(0L)
            .topEndpoint(null)
            .topEndpointCount(0L)
            .date(date.format(DATE_FORMATTER))
            .build();
    }
}
