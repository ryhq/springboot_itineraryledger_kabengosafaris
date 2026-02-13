package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for exporting access logs in various formats
 *
 * Supports:
 * - TEXT: Raw log format
 * - CSV: Comma-separated values
 * - JSON: JSON array format
 * - EXCEL: Excel workbook (.xlsx) with all fields
 */
@Service
@Slf4j
public class LogExportService {

    @Autowired
    private AccessLogParserService parserService;

    @Autowired
    private com.itineraryledger.kabengosafaris.Security.IdObfuscator idObfuscator;

    @Autowired
    private AccessLogSettingGetterServices settings;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Export logs in specified format with comprehensive filtering
     * Uses the same filters as AccessLogService.getLogs() for consistency
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
    public ResponseEntity<Resource> exportLogs(
        String format,
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
        // Validate format
        if (format == null || !List.of("TEXT", "CSV", "JSON", "EXCEL").contains(format.toUpperCase())) {
            return ResponseEntity.badRequest().build();
        }

        // Validate date
        if (date != null && date.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().build();
        }

        // Resolve log file path
        Path logFilePath = resolveLogFilePath(date);

        if (!Files.exists(logFilePath)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] exportBytes;
            String filename;
            MediaType mediaType;

            switch (format.toUpperCase()) {
                case "TEXT":
                    String textContent = exportAsText(logFilePath,
                        remoteAddress, localAddress, localPort, remoteHost,
                        requestMethod, requestUri, status, statusCategory,
                        responseSizeBytes, responseSizeBytesArgument,
                        timeTakenMillis, timeTakenMillisArgument,
                        userAgent, referer, xForwardedFor, host,
                        isSuspicious, threatType, minThreatScore,
                        isBot, botType,
                        isSlowRequest, performanceGrade,
                        browserName, operatingSystem, deviceType);
                    exportBytes = textContent.getBytes();
                    filename = "access_logs_" + (date != null ? date : LocalDate.now()).format(DATE_FORMATTER) + ".txt";
                    mediaType = MediaType.TEXT_PLAIN;
                    break;

                case "CSV":
                    String csvContent = exportAsCsv(logFilePath,
                        remoteAddress, localAddress, localPort, remoteHost,
                        requestMethod, requestUri, status, statusCategory,
                        responseSizeBytes, responseSizeBytesArgument,
                        timeTakenMillis, timeTakenMillisArgument,
                        userAgent, referer, xForwardedFor, host,
                        isSuspicious, threatType, minThreatScore,
                        isBot, botType,
                        isSlowRequest, performanceGrade,
                        browserName, operatingSystem, deviceType);
                    exportBytes = csvContent.getBytes();
                    filename = "access_logs_" + (date != null ? date : LocalDate.now()).format(DATE_FORMATTER) + ".csv";
                    mediaType = MediaType.parseMediaType("text/csv");
                    break;

                case "JSON":
                    String jsonContent = exportAsJson(logFilePath,
                        remoteAddress, localAddress, localPort, remoteHost,
                        requestMethod, requestUri, status, statusCategory,
                        responseSizeBytes, responseSizeBytesArgument,
                        timeTakenMillis, timeTakenMillisArgument,
                        userAgent, referer, xForwardedFor, host,
                        isSuspicious, threatType, minThreatScore,
                        isBot, botType,
                        isSlowRequest, performanceGrade,
                        browserName, operatingSystem, deviceType);
                    exportBytes = jsonContent.getBytes();
                    filename = "access_logs_" + (date != null ? date : LocalDate.now()).format(DATE_FORMATTER) + ".json";
                    mediaType = MediaType.APPLICATION_JSON;
                    break;

                case "EXCEL":
                    exportBytes = exportAsExcel(logFilePath,
                        remoteAddress, localAddress, localPort, remoteHost,
                        requestMethod, requestUri, status, statusCategory,
                        responseSizeBytes, responseSizeBytesArgument,
                        timeTakenMillis, timeTakenMillisArgument,
                        userAgent, referer, xForwardedFor, host,
                        isSuspicious, threatType, minThreatScore,
                        isBot, botType,
                        isSlowRequest, performanceGrade,
                        browserName, operatingSystem, deviceType);
                    filename = "access_logs_" + (date != null ? date : LocalDate.now()).format(DATE_FORMATTER) + ".xlsx";
                    mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
                    break;

                default:
                    return ResponseEntity.badRequest().build();
            }

            ByteArrayResource resource = new ByteArrayResource(exportBytes);

            // Properly format Content-Disposition header using Spring's ContentDisposition builder
            // This ensures RFC 6266 compliance and correct filename handling across all browsers
            ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename(filename)
                .build();

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(mediaType)
                .contentLength(resource.contentLength())
                .body(resource);

        } catch (IOException ex) {
            log.error("Error exporting logs: {}", logFilePath, ex);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export logs as plain text (raw log format)
     * Applies the same comprehensive filters as AccessLogService.getLogs()
     */
    private String exportAsText(
        Path logFilePath,
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
    ) throws IOException {
        try (Stream<String> lines = Files.lines(logFilePath)) {
            AtomicLong counter = new AtomicLong(0);

            List<String> filteredLines = lines
                .map(line -> {
                    long id = counter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    AccessLogDTO dto = parserService.parse(line, encodedId);
                    return new AbstractMap.SimpleEntry<>(line, dto);
                })
                .filter(entry -> remoteAddress == null || remoteAddress.equals(entry.getValue().getRemoteAddress()))
                .filter(entry -> localAddress == null || localAddress.equals(entry.getValue().getLocalAddress()))
                .filter(entry -> localPort == null || localPort.equals(entry.getValue().getLocalPort()))
                .filter(entry -> remoteHost == null || remoteHost.equals(entry.getValue().getRemoteHost()))
                .filter(entry -> requestMethod == null || requestMethod.equalsIgnoreCase(entry.getValue().getRequestMethod()))
                .filter(entry -> requestUri == null || (entry.getValue().getRequestUri() != null && entry.getValue().getRequestUri().contains(requestUri)))
                .filter(entry -> status == null || status.equals(entry.getValue().getStatus()))
                .filter(entry -> statusCategory == null || statusCategory.equals(entry.getValue().getStatusCategory()))
                .filter(entry -> responseSizeBytes == null || compare(entry.getValue().getResponseSizeBytes(), responseSizeBytes, responseSizeBytesArgument))
                .filter(entry -> timeTakenMillis == null || compare(entry.getValue().getTimeTakenMillis(), timeTakenMillis, timeTakenMillisArgument))
                .filter(entry -> userAgent == null || (entry.getValue().getUserAgent() != null && entry.getValue().getUserAgent().contains(userAgent)))
                .filter(entry -> referer == null || (entry.getValue().getReferer() != null && entry.getValue().getReferer().contains(referer)))
                .filter(entry -> xForwardedFor == null || (entry.getValue().getXForwardedFor() != null && entry.getValue().getXForwardedFor().contains(xForwardedFor)))
                .filter(entry -> host == null || (entry.getValue().getHost() != null && entry.getValue().getHost().contains(host)))
                .filter(entry -> isSuspicious == null || (entry.getValue().getIsSuspicious() != null && entry.getValue().getIsSuspicious().equals(isSuspicious)))
                .filter(entry -> threatType == null || (entry.getValue().getThreatType() != null && entry.getValue().getThreatType().contains(threatType)))
                .filter(entry -> minThreatScore == null || (entry.getValue().getThreatScore() != null && entry.getValue().getThreatScore() >= minThreatScore))
                .filter(entry -> isBot == null || (entry.getValue().getIsBot() != null && entry.getValue().getIsBot().equals(isBot)))
                .filter(entry -> botType == null || (entry.getValue().getBotType() != null && entry.getValue().getBotType().equals(botType)))
                .filter(entry -> isSlowRequest == null || (entry.getValue().getIsSlowRequest() != null && entry.getValue().getIsSlowRequest().equals(isSlowRequest)))
                .filter(entry -> performanceGrade == null || performanceGrade.equals(entry.getValue().getPerformanceGrade()))
                .filter(entry -> browserName == null || (entry.getValue().getBrowserName() != null && entry.getValue().getBrowserName().contains(browserName)))
                .filter(entry -> operatingSystem == null || (entry.getValue().getOperatingSystem() != null && entry.getValue().getOperatingSystem().contains(operatingSystem)))
                .filter(entry -> deviceType == null || deviceType.equals(entry.getValue().getDeviceType()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

            return String.join("\n", filteredLines);
        }
    }

    /**
     * Export logs as CSV
     * Applies the same comprehensive filters as AccessLogService.getLogs()
     */
    private String exportAsCsv(
        Path logFilePath,
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
    ) throws IOException {
        StringBuilder csv = new StringBuilder();

        // CSV Header (Log ID excluded from export)
        csv.append("Remote Address,Local Address,Local Port,Remote Host,Timestamp,")
           .append("Request Method,Request URI,Status,Response Size,Time Taken (ms),")
           .append("User Agent,Referer,Is Suspicious,Threat Type,Threat Score,")
           .append("Is Bot,Bot Type,Bot Name,Is Slow Request,Performance Grade,")
           .append("Browser Name,Operating System,Device Type\n");

        try (Stream<String> lines = Files.lines(logFilePath)) {
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

            for (AccessLogDTO dto : filteredLogs) {
                // Log ID excluded from export
                csv.append(escapeCsv(dto.getRemoteAddress())).append(",")
                   .append(escapeCsv(dto.getLocalAddress())).append(",")
                   .append(dto.getLocalPort() != null ? dto.getLocalPort() : "").append(",")
                   .append(escapeCsv(dto.getRemoteHost())).append(",")
                   .append(escapeCsv(dto.getTimestamp())).append(",")
                   .append(escapeCsv(dto.getRequestMethod())).append(",")
                   .append(escapeCsv(dto.getRequestUri())).append(",")
                   .append(dto.getStatus() != null ? dto.getStatus() : "").append(",")
                   .append(dto.getResponseSizeBytes() != null ? dto.getResponseSizeBytes() : "").append(",")
                   .append(dto.getTimeTakenMillis() != null ? dto.getTimeTakenMillis() : "").append(",")
                   .append(escapeCsv(dto.getUserAgent())).append(",")
                   .append(escapeCsv(dto.getReferer())).append(",")
                   .append(dto.getIsSuspicious() != null ? dto.getIsSuspicious() : "").append(",")
                   .append(escapeCsv(dto.getThreatType())).append(",")
                   .append(dto.getThreatScore() != null ? dto.getThreatScore() : "").append(",")
                   .append(dto.getIsBot() != null ? dto.getIsBot() : "").append(",")
                   .append(escapeCsv(dto.getBotType())).append(",")
                   .append(escapeCsv(dto.getBotName())).append(",")
                   .append(dto.getIsSlowRequest() != null ? dto.getIsSlowRequest() : "").append(",")
                   .append(escapeCsv(dto.getPerformanceGrade())).append(",")
                   .append(escapeCsv(dto.getBrowserName())).append(",")
                   .append(escapeCsv(dto.getOperatingSystem())).append(",")
                   .append(escapeCsv(dto.getDeviceType())).append("\n");
            }
        }

        return csv.toString();
    }

    /**
     * Export logs as JSON
     * Applies the same comprehensive filters as AccessLogService.getLogs()
     */
    private String exportAsJson(
        Path logFilePath,
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
    ) throws IOException {
        try (Stream<String> lines = Files.lines(logFilePath)) {
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

            // Configure ObjectMapper to exclude logId field from JSON export
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.addMixIn(AccessLogDTO.class, LogIdExclusionMixIn.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(filteredLogs);
        }
    }

    /**
     * Jackson MixIn to exclude logId field from JSON serialization during export
     */
    @JsonIgnoreProperties({"logId"})
    private abstract static class LogIdExclusionMixIn {
    }

    /**
     * Export logs as Excel workbook (.xlsx)
     * Applies the same comprehensive filters as AccessLogService.getLogs()
     * Includes ALL AccessLogDTO fields with auto-sized columns
     */
    private byte[] exportAsExcel(
        Path logFilePath,
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
    ) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Access Logs");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row with all AccessLogDTO fields (Log ID excluded from export)
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Remote Address", "Local Address", "Local Port", "Remote Host",
                "Remote Logical User", "Remote User", "Timestamp", "Request Line",
                "Request Method", "Request URI", "Request Protocol", "Status",
                "Response Size (Bytes)", "Response Size (Formatted)", "Time Taken (Micros)",
                "Time Taken (Millis)", "Time Taken (Formatted)", "User Agent", "Referer",
                "X-Forwarded-For", "Cookie", "Host", "SSL Session ID", "Request Thread Name",
                "Is Suspicious", "Threat Type", "Threat Score", "Security Analysis",
                "Matched Patterns", "Is Slow Request", "Performance Grade", "Is Bot",
                "Bot Type", "Bot Name", "Country Code", "Country Name", "City",
                "Region", "ISP", "Organization", "Is VPN", "Timestamp Epoch",
                "Status Category", "Is Success", "Is Client Error", "Is Server Error",
                "Browser Name", "Browser Version", "Operating System", "Device Type"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Get filtered logs
            try (Stream<String> lines = Files.lines(logFilePath)) {
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

                // Add data rows (Log ID excluded from export)
                int rowNum = 1;
                for (AccessLogDTO dto : filteredLogs) {
                    Row row = sheet.createRow(rowNum++);
                    int colNum = 0;

                    // Log ID excluded - start with remoteAddress
                    setCellValue(row, colNum++, dto.getRemoteAddress());
                    setCellValue(row, colNum++, dto.getLocalAddress());
                    setCellValue(row, colNum++, dto.getLocalPort());
                    setCellValue(row, colNum++, dto.getRemoteHost());
                    setCellValue(row, colNum++, dto.getRemoteLogicalUser());
                    setCellValue(row, colNum++, dto.getRemoteUser());
                    setCellValue(row, colNum++, dto.getTimestamp());
                    setCellValue(row, colNum++, dto.getRequestLine());
                    setCellValue(row, colNum++, dto.getRequestMethod());
                    setCellValue(row, colNum++, dto.getRequestUri());
                    setCellValue(row, colNum++, dto.getRequestProtocol());
                    setCellValue(row, colNum++, dto.getStatus());
                    setCellValue(row, colNum++, dto.getResponseSizeBytes());
                    setCellValue(row, colNum++, dto.getResponseSizeFormatted());
                    setCellValue(row, colNum++, dto.getTimeTakenMicros());
                    setCellValue(row, colNum++, dto.getTimeTakenMillis());
                    setCellValue(row, colNum++, dto.getTimeTakenFormatted());
                    setCellValue(row, colNum++, dto.getUserAgent());
                    setCellValue(row, colNum++, dto.getReferer());
                    setCellValue(row, colNum++, dto.getXForwardedFor());
                    setCellValue(row, colNum++, dto.getCookie());
                    setCellValue(row, colNum++, dto.getHost());
                    setCellValue(row, colNum++, dto.getSslSessionId());
                    setCellValue(row, colNum++, dto.getRequestThreadName());
                    setCellValue(row, colNum++, dto.getIsSuspicious());
                    setCellValue(row, colNum++, dto.getThreatType());
                    setCellValue(row, colNum++, dto.getThreatScore());
                    setCellValue(row, colNum++, dto.getSecurityAnalysis());
                    setCellValue(row, colNum++, dto.getMatchedPatterns());
                    setCellValue(row, colNum++, dto.getIsSlowRequest());
                    setCellValue(row, colNum++, dto.getPerformanceGrade());
                    setCellValue(row, colNum++, dto.getIsBot());
                    setCellValue(row, colNum++, dto.getBotType());
                    setCellValue(row, colNum++, dto.getBotName());
                    setCellValue(row, colNum++, dto.getCountryCode());
                    setCellValue(row, colNum++, dto.getCountryName());
                    setCellValue(row, colNum++, dto.getCity());
                    setCellValue(row, colNum++, dto.getRegion());
                    setCellValue(row, colNum++, dto.getIsp());
                    setCellValue(row, colNum++, dto.getOrganization());
                    setCellValue(row, colNum++, dto.getIsVpn());
                    setCellValue(row, colNum++, dto.getTimestampEpoch());
                    setCellValue(row, colNum++, dto.getStatusCategory());
                    setCellValue(row, colNum++, dto.getIsSuccess());
                    setCellValue(row, colNum++, dto.getIsClientError());
                    setCellValue(row, colNum++, dto.getIsServerError());
                    setCellValue(row, colNum++, dto.getBrowserName());
                    setCellValue(row, colNum++, dto.getBrowserVersion());
                    setCellValue(row, colNum++, dto.getOperatingSystem());
                    setCellValue(row, colNum++, dto.getDeviceType());
                }
            }

            // Auto-size all columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }


    /**
     * Helper method to set cell values in Excel with proper type handling
     */
    private void setCellValue(Row row, int colNum, Object value) {
        Cell cell = row.createCell(colNum);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Integer) {
            cell.setCellValue((Integer) value);
        } else if (value instanceof Long) {
            cell.setCellValue((Long) value);
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }



    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
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
     * Same logic as AccessLogService.compare() for consistency
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
