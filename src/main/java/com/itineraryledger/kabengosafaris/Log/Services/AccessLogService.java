package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Main service for retrieving and filtering access logs
 *
 * Reads logs from disk (like ItineraryLedger) and enriches with analysis
 */
@Service
@Slf4j
public class AccessLogService {

    @Autowired
    private AccessLogParserService parserService;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private AccessLogSettingGetterServices settings;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Get access logs with filtering and pagination
     */
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLogs(
        int page,
        int size,
        LocalDate date,
        String remoteAddress,
        String localAddress,
        Integer localPort,
        String remoteHost,
        String requestMethod,
        String requestUri,
        Integer status,
        String statusCategory,
        Long responseSizeBytes,
        String responseSizeBytesArgument,
        Long timeTakenMillis,
        String timeTakenMillisArgument,
        String userAgent,
        String referer,
        String xForwardedFor,
        String host,
        Boolean isSuspicious,
        String threatType,
        Integer minThreatScore,
        Boolean isBot,
        String botType,
        Boolean isSlowRequest,
        String performanceGrade,
        String browserName,
        String operatingSystem,
        String deviceType
    ) {
        // Validate date
        if (date != null && date.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Future dates are not allowed", "INVALID_DATE")
            );
        }

        // Validate IP addresses
        Pattern ipPattern = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(?:\\.(?!$)|$)){4}$"
        );

        if (remoteAddress != null && !ipPattern.matcher(remoteAddress).matches()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid remote address format", "INVALID_IP")
            );
        }

        if (localAddress != null && !ipPattern.matcher(localAddress).matches()) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid local address format", "INVALID_IP")
            );
        }

        // Validate port
        if (localPort != null && (localPort < 0 || localPort > 65535)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid port: must be 0-65535", "INVALID_PORT")
            );
        }

        // Validate status
        if (status != null && (status < 100 || status > 599)) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(400, "Invalid HTTP status", "INVALID_STATUS")
            );
        }

        // Validate comparison arguments
        List<String> validArguments = List.of(
            "Equality", "Inequality", "GreaterThan", "LessThan",
            "GreaterThanOrEqualTo", "LessThanOrEqualTo"
        );

        if (responseSizeBytes != null && responseSizeBytesArgument != null) {
            if (validArguments.stream().noneMatch(op -> op.equalsIgnoreCase(responseSizeBytesArgument))) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid responseSizeBytesArgument", "INVALID_ARGUMENT")
                );
            }
        }

        if (timeTakenMillis != null && timeTakenMillisArgument != null) {
            if (validArguments.stream().noneMatch(op -> op.equalsIgnoreCase(timeTakenMillisArgument))) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid timeTakenMillisArgument", "INVALID_ARGUMENT")
                );
            }
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
            // Parse and filter logs
            AtomicLong counter = new AtomicLong(0);

            List<AccessLogDTO> filteredLogs = lines
                .map(line -> {
                    long id = counter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    return parserService.parse(line, encodedId);
                })
                .filter(dto -> remoteAddress == null || remoteAddress.equals(dto.getRemoteAddress()))
                .filter(dto -> localAddress == null || localAddress.equals(dto.getLocalAddress()))
                .filter(dto -> localPort == null || localPort.equals(dto.getLocalPort()))
                .filter(dto -> remoteHost == null || remoteHost.equals(dto.getRemoteHost()))
                .filter(dto -> requestMethod == null || requestMethod.equalsIgnoreCase(dto.getRequestMethod()))
                .filter(dto -> requestUri == null || (dto.getRequestUri() != null && dto.getRequestUri().contains(requestUri)))
                .filter(dto -> status == null || status.equals(dto.getStatus()))
                .filter(dto -> statusCategory == null || statusCategory.equals(dto.getStatusCategory()))
                .filter(dto -> responseSizeBytes == null || compare(dto.getResponseSizeBytes(), responseSizeBytes, responseSizeBytesArgument))
                .filter(dto -> timeTakenMillis == null || compare(dto.getTimeTakenMillis(), timeTakenMillis, timeTakenMillisArgument))
                .filter(dto -> userAgent == null || (dto.getUserAgent() != null && dto.getUserAgent().contains(userAgent)))
                .filter(dto -> referer == null || (dto.getReferer() != null && dto.getReferer().contains(referer)))
                .filter(dto -> xForwardedFor == null || (dto.getXForwardedFor() != null && dto.getXForwardedFor().contains(xForwardedFor)))
                .filter(dto -> host == null || (dto.getHost() != null && dto.getHost().contains(host)))
                .filter(dto -> isSuspicious == null || (dto.getIsSuspicious() != null && dto.getIsSuspicious().equals(isSuspicious)))
                .filter(dto -> threatType == null || (dto.getThreatType() != null && dto.getThreatType().contains(threatType)))
                .filter(dto -> minThreatScore == null || (dto.getThreatScore() != null && dto.getThreatScore() >= minThreatScore))
                .filter(dto -> isBot == null || (dto.getIsBot() != null && dto.getIsBot().equals(isBot)))
                .filter(dto -> botType == null || (dto.getBotType() != null && dto.getBotType().equals(botType)))
                .filter(dto -> isSlowRequest == null || (dto.getIsSlowRequest() != null && dto.getIsSlowRequest().equals(isSlowRequest)))
                .filter(dto -> performanceGrade == null || performanceGrade.equals(dto.getPerformanceGrade()))
                .filter(dto -> browserName == null || (dto.getBrowserName() != null && dto.getBrowserName().contains(browserName)))
                .filter(dto -> operatingSystem == null || (dto.getOperatingSystem() != null && dto.getOperatingSystem().contains(operatingSystem)))
                .filter(dto -> deviceType == null || deviceType.equals(dto.getDeviceType()))
                .collect(Collectors.toList());

            // Reverse for latest first
            Collections.reverse(filteredLogs);

            // Paginate
            PageRequest pageRequest = PageRequest.of(page, size);
            int start = (int) pageRequest.getOffset();
            int end = Math.min(start + pageRequest.getPageSize(), filteredLogs.size());

            List<AccessLogDTO> pageContent = (start > filteredLogs.size())
                ? Collections.emptyList()
                : filteredLogs.subList(start, end);

            Page<AccessLogDTO> pageLogs = new PageImpl<>(pageContent, pageRequest, filteredLogs.size());

            if (pageLogs.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(
                    ApiResponse.success(204, "No logs found matching the criteria", null)
                );
            }

            // Prepare response with summary
            Map<String, Object> response = new HashMap<>();
            response.put("logs", pageLogs.getContent());
            response.put("currentPage", pageLogs.getNumber());
            response.put("totalItems", pageLogs.getTotalElements());
            response.put("totalPages", pageLogs.getTotalPages());
            response.put("summary", generateSummary(pageLogs.getContent()));

            return ResponseEntity.ok(
                ApiResponse.success(200, "Logs retrieved successfully", response)
            );

        } catch (IOException ex) {
            log.error("Error reading log file: {}", logFilePath, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Error reading log file: " + ex.getMessage(), "INTERNAL_ERROR")
            );
        }
    }

    /**
     * Get summary statistics for filtered logs
     */
    private Map<String, Object> generateSummary(List<AccessLogDTO> logs) {
        Map<String, Object> summary = new HashMap<>();

        long totalRequests = logs.size();
        long successRequests = logs.stream().filter(dto -> dto.getIsSuccess() != null && dto.getIsSuccess()).count();
        long clientErrors = logs.stream().filter(dto -> dto.getIsClientError() != null && dto.getIsClientError()).count();
        long serverErrors = logs.stream().filter(dto -> dto.getIsServerError() != null && dto.getIsServerError()).count();
        long suspiciousRequests = logs.stream().filter(dto -> dto.getIsSuspicious() != null && dto.getIsSuspicious()).count();
        long botRequests = logs.stream().filter(dto -> dto.getIsBot() != null && dto.getIsBot()).count();
        long slowRequests = logs.stream().filter(dto -> dto.getIsSlowRequest() != null && dto.getIsSlowRequest()).count();

        // Calculate averages
        OptionalDouble avgResponseTime = logs.stream()
            .filter(dto -> dto.getTimeTakenMillis() != null)
            .mapToLong(AccessLogDTO::getTimeTakenMillis)
            .average();

        OptionalDouble avgResponseSize = logs.stream()
            .filter(dto -> dto.getResponseSizeBytes() != null)
            .mapToLong(AccessLogDTO::getResponseSizeBytes)
            .average();

        // Count unique IPs
        long uniqueIPs = logs.stream()
            .map(AccessLogDTO::getRemoteAddress)
            .filter(Objects::nonNull)
            .distinct()
            .count();

        // Status code distribution
        Map<Integer, Long> statusDistribution = logs.stream()
            .filter(dto -> dto.getStatus() != null)
            .collect(Collectors.groupingBy(AccessLogDTO::getStatus, Collectors.counting()));

        // Performance grade distribution
        Map<String, Long> performanceDistribution = logs.stream()
            .filter(dto -> dto.getPerformanceGrade() != null)
            .collect(Collectors.groupingBy(AccessLogDTO::getPerformanceGrade, Collectors.counting()));

        summary.put("totalRequests", totalRequests);
        summary.put("successRequests", successRequests);
        summary.put("clientErrors", clientErrors);
        summary.put("serverErrors", serverErrors);
        summary.put("suspiciousRequests", suspiciousRequests);
        summary.put("botRequests", botRequests);
        summary.put("slowRequests", slowRequests);
        summary.put("uniqueIPs", uniqueIPs);
        summary.put("avgResponseTimeMs", avgResponseTime.isPresent() ? avgResponseTime.getAsDouble() : 0);
        summary.put("avgResponseSizeBytes", avgResponseSize.isPresent() ? avgResponseSize.getAsDouble() : 0);
        summary.put("statusDistribution", statusDistribution);
        summary.put("performanceDistribution", performanceDistribution);

        return summary;
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

    /**
     * Compare numeric values with operator
     */
    private boolean compare(Long actual, Long target, String argument) {
        if (actual == null || target == null || argument == null) return true;

        return switch (argument.toLowerCase()) {
            case "equality" -> actual.equals(target);
            case "inequality" -> !actual.equals(target);
            case "greaterthan" -> actual > target;
            case "lessthan" -> actual < target;
            case "greaterthanorequalto" -> actual >= target;
            case "lessthanorequalto" -> actual <= target;
            default -> false;
        };
    }
}
