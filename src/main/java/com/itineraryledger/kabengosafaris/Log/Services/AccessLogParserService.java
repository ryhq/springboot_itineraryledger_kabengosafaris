package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing Tomcat access logs with enhanced analysis
 *
 * Parses log lines following the pattern:
 * %a %A %p %h %l %u %t "%r" %s %b %D "%{User-Agent}i" "%{Referer}i" "%{X-Forwarded-For}i" "%{Cookie}i" "%{Host}i" %S %I
 *
 * Enriches logs with:
 * - Security threat detection
 * - Bot classification
 * - Performance analysis
 * - User agent parsing
 * - Geographic information (optional)
 */
@Service
@Slf4j
public class AccessLogParserService {

    @Autowired
    private SecurityAnalysisService securityAnalysisService;

    @Autowired
    private BotDetectionService botDetectionService;

    @Autowired
    private PerformanceAnalysisService performanceAnalysisService;

    @Autowired
    private UserAgentParserService userAgentParserService;

    @Autowired
    private AccessLogSettingGetterServices settings;

    // Comprehensive pattern for Tomcat access log
    // Pattern: %a %A %p %h %l %u %t "%r" %s %b %D "%{User-Agent}i" "%{Referer}i" "%{X-Forwarded-For}i" "%{Cookie}i" "%{Host}i" %S %I
    private static final Pattern LONG_PATTERN = Pattern.compile(
        "^(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\S+)\\s+\\[([^\\]]+)\\]\\s+\"([^\"]*)\"\\s+" +
        "(\\d+)\\s+(\\S+)\\s+(\\d+)\\s+\"([^\"]*)\"\\s+\"([^\"]*)\"\\s+\"([^\"]*)\"\\s+\"([^\"]*)\"\\s+" +
        "\"([^\"]*)\"\\s+(\\S+)\\s+(\\S+)$"
    );

    // Timestamp formatter for Tomcat logs
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    // Decimal formatters
    private static final DecimalFormat TIME_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat SIZE_FORMAT = new DecimalFormat("#,##0.00");

    /**
     * Parse a single access log line with full analysis
     *
     * @param logLine raw log line from Tomcat access log
     * @param logId   unique obfuscated ID for this log entry
     * @return parsed and enriched AccessLogDTO
     */
    public AccessLogDTO parse(String logLine, String logId) {
        Matcher matcher = LONG_PATTERN.matcher(logLine);

        if (!matcher.matches()) {
            // Pattern didn't match - return DTO with raw log only
            return createEmptyDTO(logLine, logId);
        }

        try {
            // Build basic DTO from matched groups
            AccessLogDTO dto = AccessLogDTO.builder()
                .logId(logId)
                .remoteAddress(safeGroup(matcher, 1))
                .localAddress(safeGroup(matcher, 2))
                .localPort(parseInteger(matcher.group(3)))
                .remoteHost(safeGroup(matcher, 4))
                .remoteLogicalUser(safeGroup(matcher, 5))
                .remoteUser(safeGroup(matcher, 6))
                .timestamp(safeGroup(matcher, 7))
                .requestLine(safeGroup(matcher, 8))
                .status(parseInteger(matcher.group(9)))
                .responseSizeBytes(parseLong(matcher.group(10)))
                .timeTakenMicros(parseLong(matcher.group(11)))
                .userAgent(safeGroup(matcher, 12))
                .referer(safeGroup(matcher, 13))
                .xForwardedFor(safeGroup(matcher, 14))
                .cookie(safeGroup(matcher, 15))
                .host(safeGroup(matcher, 16))
                .sslSessionId(safeGroup(matcher, 17))
                .requestThreadName(safeGroup(matcher, 18))
                .fullLog(logLine)
                .build();

            // Extract request method, URI, protocol from requestLine
            parseRequestLine(dto);

            // Resolve effective client IP from X-Forwarded-For (behind reverse proxy)
            resolveClientIp(dto);

            // Calculate derived time fields
            if (dto.getTimeTakenMicros() != null) {
                dto.setTimeTakenMillis(dto.getTimeTakenMicros() / 1000);
                dto.setTimeTakenFormatted(formatTime(dto.getTimeTakenMillis()));
            }

            // Format response size
            if (dto.getResponseSizeBytes() != null) {
                dto.setResponseSizeFormatted(formatBytes(dto.getResponseSizeBytes()));
            }

            // Set status category and flags
            enrichStatusMetadata(dto);

            // Parse timestamp to epoch
            parseTimestampToEpoch(dto);

            // Run analysis services (based on settings)
            runAnalysisServices(dto);

            return dto;

        } catch (Exception e) {
            log.error("Error parsing log line: {}", logLine, e);
            return createEmptyDTO(logLine, logId);
        }
    }

    /**
     * Parse multiple log lines in batch
     *
     * @param logLines list of raw log lines
     * @param startId  starting ID for log entries
     * @return list of parsed DTOs
     */
    public List<AccessLogDTO> parseMultiple(List<String> logLines, long startId) {
        AtomicLong counter = new AtomicLong(startId);
        return logLines.stream()
            .map(line -> parse(line, String.valueOf(counter.getAndIncrement())))
            .collect(Collectors.toList());
    }

    /**
     * Extract method, URI, protocol from request line
     * Example: "GET /api/parks HTTP/1.1" -> method=GET, uri=/api/parks, protocol=HTTP/1.1
     */
    private void parseRequestLine(AccessLogDTO dto) {
        String requestLine = dto.getRequestLine();
        if (requestLine != null && !requestLine.isEmpty() && !requestLine.equals("-")) {
            String[] parts = requestLine.split(" ", 3);
            if (parts.length >= 1) {
                dto.setRequestMethod(parts[0]);
            }
            if (parts.length >= 2) {
                dto.setRequestUri(parts[1]);
            }
            if (parts.length >= 3) {
                dto.setRequestProtocol(parts[2]);
            }
        }
    }

    /**
     * Resolve the effective client IP from X-Forwarded-For header.
     * Behind Nginx, remoteAddress is always 127.0.0.1 or ::1.
     * The real client IP is in X-Forwarded-For (first IP in the chain).
     */
    private void resolveClientIp(AccessLogDTO dto) {
        String xff = dto.getXForwardedFor();
        if (xff != null && !xff.isEmpty()) {
            // X-Forwarded-For can be comma-separated: "client, proxy1, proxy2"
            String clientIp = xff.split(",")[0].trim();
            if (!clientIp.isEmpty()) {
                dto.setRemoteAddress(clientIp);
                dto.setRemoteHost(clientIp);
            }
        }
    }

    /**
     * Enrich DTO with status-based metadata
     */
    private void enrichStatusMetadata(AccessLogDTO dto) {
        Integer status = dto.getStatus();
        if (status != null) {
            // Set status category (2xx, 3xx, 4xx, 5xx)
            dto.setStatusCategory((status / 100) + "xx");

            // Set status flags
            dto.setIsSuccess(status >= 200 && status < 400);
            dto.setIsClientError(status >= 400 && status < 500);
            dto.setIsServerError(status >= 500);
        }
    }

    /**
     * Parse timestamp string to epoch milliseconds
     */
    private void parseTimestampToEpoch(AccessLogDTO dto) {
        String timestamp = dto.getTimestamp();
        if (timestamp != null && !timestamp.isEmpty()) {
            try {
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(timestamp, TIMESTAMP_FORMATTER);
                dto.setTimestampEpoch(zonedDateTime.toInstant().toEpochMilli());
            } catch (Exception e) {
                log.debug("Failed to parse timestamp: {}", timestamp);
            }
        }
    }

    /**
     * Run all enabled analysis services
     */
    private void runAnalysisServices(AccessLogDTO dto) {
        // Security analysis
        if (settings.isSecurityAnalysisEnabled()) {
            securityAnalysisService.analyze(dto);
        }

        // Bot detection
        if (settings.isBotDetectionEnabled()) {
            botDetectionService.detect(dto);
        }

        // Performance analysis
        if (settings.isPerformanceMonitoringEnabled()) {
            performanceAnalysisService.analyze(dto);
        }

        // User agent parsing (always run - lightweight)
        userAgentParserService.parse(dto);
    }

    /**
     * Format time in milliseconds to human-readable format
     */
    private String formatTime(Long millis) {
        if (millis == null) return null;

        if (millis < 1000) {
            return millis + "ms";
        } else {
            double seconds = millis / 1000.0;
            return TIME_FORMAT.format(seconds) + "s";
        }
    }

    /**
     * Format bytes to human-readable format (KB, MB, GB)
     */
    private String formatBytes(Long bytes) {
        if (bytes == null) return null;

        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / 1024.0) + " KB";
        } else if (bytes < 1024 * 1024 * 1024) {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0)) + " MB";
        } else {
            return SIZE_FORMAT.format(bytes / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
    }

    /**
     * Create empty DTO when parsing fails
     */
    private AccessLogDTO createEmptyDTO(String logLine, String logId) {
        return AccessLogDTO.builder()
            .logId(logId)
            .fullLog(logLine)
            .build();
    }

    /**
     * Safely extract group from matcher (returns "-" if group is null/empty)
     */
    private String safeGroup(Matcher matcher, int group) {
        try {
            String value = matcher.group(group);
            return (value == null || value.isEmpty() || value.equals("-")) ? null : value;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse string to Integer, return null on failure
     */
    private Integer parseInteger(String value) {
        try {
            return (value == null || value.equals("-")) ? null : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parse string to Long, return null on failure
     */
    private Long parseLong(String value) {
        try {
            return (value == null || value.equals("-")) ? null : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
