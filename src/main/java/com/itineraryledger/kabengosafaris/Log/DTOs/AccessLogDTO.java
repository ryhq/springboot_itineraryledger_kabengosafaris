package com.itineraryledger.kabengosafaris.Log.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Enhanced Access Log DTO with Security and Performance Analysis
 *
 * Extends the basic Tomcat access log with:
 * - Security threat detection
 * - Performance metrics
 * - Bot classification
 * - Geographic information (optional)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessLogDTO {

    // ==========================================
    // CORE TOMCAT ACCESS LOG FIELDS
    // ==========================================

    /**
     * Unique log ID (obfuscated)
     */
    private String logId;

    /**
     * Remote IP address (%a)
     */
    private String remoteAddress;

    /**
     * Local IP address (%A)
     */
    private String localAddress;

    /**
     * Local port (%p)
     */
    private Integer localPort;

    /**
     * Remote host (%h)
     */
    private String remoteHost;

    /**
     * Remote logical user (%l) - RFC 1413, usually "-"
     */
    private String remoteLogicalUser;

    /**
     * Remote authenticated user (%u)
     */
    private String remoteUser;

    /**
     * Timestamp (%t) - e.g., "09/Feb/2026:14:30:00 +0300"
     */
    private String timestamp;

    /**
     * First line of HTTP request (%r) - e.g., "GET /api/parks HTTP/1.1"
     */
    private String requestLine;

    /**
     * HTTP method extracted from requestLine
     */
    private String requestMethod;

    /**
     * Request URI extracted from requestLine
     */
    private String requestUri;

    /**
     * HTTP protocol extracted from requestLine
     */
    private String requestProtocol;

    /**
     * HTTP status code (%s)
     */
    private Integer status;

    /**
     * Response size in bytes (%b)
     */
    private Long responseSizeBytes;

    /**
     * Response size formatted (e.g., "2.4 MB")
     */
    private String responseSizeFormatted;

    /**
     * Time taken in microseconds (%D)
     */
    private Long timeTakenMicros;

    /**
     * Time taken in milliseconds (calculated)
     */
    private Long timeTakenMillis;

    /**
     * Time taken formatted (e.g., "1.23s")
     */
    private String timeTakenFormatted;

    /**
     * User-Agent header
     */
    private String userAgent;

    /**
     * Referer header
     */
    private String referer;

    /**
     * X-Forwarded-For header
     */
    private String xForwardedFor;

    /**
     * Cookie header
     */
    private String cookie;

    /**
     * Host header
     */
    private String host;

    /**
     * SSL session ID (%S)
     */
    private String sslSessionId;

    /**
     * Request thread name (%I)
     */
    private String requestThreadName;

    /**
     * Full raw log line (unparsed)
     */
    private String fullLog;

    // ==========================================
    // SECURITY ANALYSIS FIELDS
    // ==========================================

    /**
     * Whether this request is flagged as suspicious
     */
    private Boolean isSuspicious;

    /**
     * Type of threat detected (SQL_INJECTION, XSS, PATH_TRAVERSAL, etc.)
     */
    private String threatType;

    /**
     * Threat score (0-100, higher = more dangerous)
     */
    private Integer threatScore;

    /**
     * Security analysis description
     */
    private String securityAnalysis;

    /**
     * Matched security patterns (for debugging)
     */
    private String matchedPatterns;

    // ==========================================
    // PERFORMANCE ANALYSIS FIELDS
    // ==========================================

    /**
     * Whether this request is considered slow
     */
    private Boolean isSlowRequest;

    /**
     * Performance grade (A, B, C, D, F)
     */
    private String performanceGrade;

    // ==========================================
    // BOT DETECTION FIELDS
    // ==========================================

    /**
     * Whether this request is from a bot
     */
    private Boolean isBot;

    /**
     * Bot type classification (SEARCH_ENGINE, CRAWLER, SCRAPER, MALICIOUS, UNKNOWN)
     */
    private String botType;

    /**
     * Bot name if identified (e.g., "Googlebot", "Bingbot")
     */
    private String botName;

    // ==========================================
    // GEOGRAPHIC FIELDS (optional)
    // ==========================================

    /**
     * Country code (ISO 3166-1 alpha-2)
     */
    private String countryCode;

    /**
     * Country name
     */
    private String countryName;

    /**
     * City name
     */
    private String city;

    /**
     * Region/state name
     */
    private String region;

    /**
     * ISP name
     */
    private String isp;

    /**
     * Organization name
     */
    private String organization;

    /**
     * Whether this IP is from a known VPN/proxy
     */
    private Boolean isVpn;

    // ==========================================
    // ADDITIONAL METADATA
    // ==========================================

    /**
     * Request timestamp as epoch millis (for sorting)
     */
    private Long timestampEpoch;

    /**
     * Status category (2xx, 3xx, 4xx, 5xx)
     */
    private String statusCategory;

    /**
     * Whether this is a successful request (2xx, 3xx)
     */
    private Boolean isSuccess;

    /**
     * Whether this is a client error (4xx)
     */
    private Boolean isClientError;

    /**
     * Whether this is a server error (5xx)
     */
    private Boolean isServerError;

    /**
     * Browser name extracted from user agent
     */
    private String browserName;

    /**
     * Browser version
     */
    private String browserVersion;

    /**
     * Operating system extracted from user agent
     */
    private String operatingSystem;

    /**
     * Device type (DESKTOP, MOBILE, TABLET, BOT, UNKNOWN)
     */
    private String deviceType;
}
