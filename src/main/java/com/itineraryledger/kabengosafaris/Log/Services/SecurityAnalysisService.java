package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for detecting security threats in access logs
 *
 * Detects:
 * - SQL Injection attempts
 * - XSS (Cross-Site Scripting) attempts
 * - Path Traversal attempts
 * - Command Injection attempts
 * - Suspicious user agents
 * - Brute force patterns
 */
@Service
@Slf4j
public class SecurityAnalysisService {

    @Autowired
    private AccessLogSettingGetterServices settings;

    // ==========================================
    // SQL INJECTION PATTERNS
    // ==========================================
    private static final List<Pattern> SQL_PATTERNS = Arrays.asList(
        // SQL keywords
        Pattern.compile("\\b(SELECT|UNION|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|DECLARE)\\b", Pattern.CASE_INSENSITIVE),
        // SQL comments
        Pattern.compile("(--|#|/\\*|\\*/)", Pattern.CASE_INSENSITIVE),
        // SQL operators and functions
        Pattern.compile("\\b(OR|AND)\\s+[\\w\\d]+\\s*=", Pattern.CASE_INSENSITIVE),
        Pattern.compile("'\\s*(OR|AND)\\s*'", Pattern.CASE_INSENSITIVE),
        // SQL injection attempts
        Pattern.compile("'\\s*OR\\s*'?1'?\\s*=\\s*'?1", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bUNION\\b.*\\bSELECT\\b", Pattern.CASE_INSENSITIVE),
        // SQL encoding attempts
        Pattern.compile("(%27)|(')", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(\\%3D)|(=)[^\\n]*(\\%27)|(')[^\\n]*(\\%3D)|(=)", Pattern.CASE_INSENSITIVE)
    );

    // ==========================================
    // XSS (Cross-Site Scripting) PATTERNS
    // ==========================================
    private static final List<Pattern> XSS_PATTERNS = Arrays.asList(
        // Script tags
        Pattern.compile("<script[^>]*>.*?</script>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL),
        Pattern.compile("<script", Pattern.CASE_INSENSITIVE),
        // JavaScript protocols
        Pattern.compile("javascript:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("vbscript:", Pattern.CASE_INSENSITIVE),
        // Event handlers
        Pattern.compile("on(load|error|click|mouse|key|focus|blur|change|submit)", Pattern.CASE_INSENSITIVE),
        // Iframe and embed tags
        Pattern.compile("<(iframe|embed|object|frame)", Pattern.CASE_INSENSITIVE),
        // Data URIs
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
        // Encoded XSS
        Pattern.compile("(%3C)|(<)[^\\n]+((%3E)|(>))", Pattern.CASE_INSENSITIVE)
    );

    // ==========================================
    // PATH TRAVERSAL PATTERNS
    // ==========================================
    private static final List<Pattern> PATH_TRAVERSAL_PATTERNS = Arrays.asList(
        // Basic path traversal
        Pattern.compile("\\.\\.[\\\\/]"),
        Pattern.compile("\\.\\.%2f", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.\\.%5c", Pattern.CASE_INSENSITIVE),
        // URL encoded
        Pattern.compile("%2e%2e[\\\\/]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%2e%2e%2f", Pattern.CASE_INSENSITIVE),
        Pattern.compile("%2e%2e%5c", Pattern.CASE_INSENSITIVE),
        // Double encoded
        Pattern.compile("%252e%252e", Pattern.CASE_INSENSITIVE),
        // System files
        Pattern.compile("(/etc/passwd|/etc/shadow|/windows/system32)", Pattern.CASE_INSENSITIVE)
    );

    // ==========================================
    // COMMAND INJECTION PATTERNS
    // ==========================================
    private static final List<Pattern> COMMAND_INJECTION_PATTERNS = Arrays.asList(
        // Shell operators
        Pattern.compile("[;|&`$()]"),
        // Command chaining
        Pattern.compile("(&&|\\|\\||;)"),
        // Backticks
        Pattern.compile("`.*`"),
        // URL encoded
        Pattern.compile("(%3B|%7C|%26|%60)", Pattern.CASE_INSENSITIVE)
    );

    // ==========================================
    // SUSPICIOUS USER AGENTS
    // ==========================================
    private static final List<Pattern> SUSPICIOUS_USER_AGENT_PATTERNS = Arrays.asList(
        Pattern.compile("(sqlmap|havij|acunetix|netsparker|nikto|w3af|metasploit)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(nmap|masscan|zmap)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(exploit|scanner|penetration)", Pattern.CASE_INSENSITIVE)
    );

    /**
     * Analyze access log for security threats
     * Enriches the DTO with security analysis results
     *
     * @param dto the access log DTO to analyze
     */
    public void analyze(AccessLogDTO dto) {
        if (!settings.isSecurityAnalysisEnabled()) {
            return;
        }

        List<String> threats = new ArrayList<>();
        int score = 0;
        List<String> matchedPatternDetails = new ArrayList<>();

        String uri = dto.getRequestUri();
        String userAgent = dto.getUserAgent();

        // Check URI for threats
        if (uri != null && !uri.isEmpty()) {
            // SQL Injection
            if (detectPattern(uri, SQL_PATTERNS)) {
                threats.add("SQL_INJECTION");
                score += 40;
                matchedPatternDetails.add("SQL injection pattern in URI");
                // log.warn("SQL injection attempt detected from {}: {}", dto.getRemoteAddress(), uri);
            }

            // XSS
            if (detectPattern(uri, XSS_PATTERNS)) {
                threats.add("XSS");
                score += 35;
                matchedPatternDetails.add("XSS pattern in URI");
                // log.warn("XSS attempt detected from {}: {}", dto.getRemoteAddress(), uri);
            }

            // Path Traversal
            if (detectPattern(uri, PATH_TRAVERSAL_PATTERNS)) {
                threats.add("PATH_TRAVERSAL");
                score += 30;
                matchedPatternDetails.add("Path traversal pattern in URI");
                // log.warn("Path traversal attempt detected from {}: {}", dto.getRemoteAddress(), uri);
            }

            // Command Injection
            if (detectPattern(uri, COMMAND_INJECTION_PATTERNS)) {
                threats.add("COMMAND_INJECTION");
                score += 35;
                matchedPatternDetails.add("Command injection pattern in URI");
                // log.warn("Command injection attempt detected from {}: {}", dto.getRemoteAddress(), uri);
            }
        }

        // Check User Agent for suspicious patterns
        if (userAgent != null && !userAgent.isEmpty() && !userAgent.equals("-")) {
            if (detectPattern(userAgent, SUSPICIOUS_USER_AGENT_PATTERNS)) {
                threats.add("SUSPICIOUS_USER_AGENT");
                score += 25;
                matchedPatternDetails.add("Known attack tool user agent");
                // log.warn("Suspicious user agent detected from {}: {}", dto.getRemoteAddress(), userAgent);
            }

            // Empty or very short user agent (suspicious)
            if (userAgent.length() < 10 && !userAgent.equals("-")) {
                threats.add("SUSPICIOUS_USER_AGENT");
                score += 15;
                matchedPatternDetails.add("Unusually short user agent");
            }
        }

        // Check for authentication failures (brute force indicator)
        if (dto.getStatus() != null && dto.getStatus() == 401) {
            score += 10;
            matchedPatternDetails.add("Authentication failure (401)");
        }

        // Check for repeated 403 errors (enumeration attempt)
        if (dto.getStatus() != null && dto.getStatus() == 403) {
            score += 5;
            matchedPatternDetails.add("Access forbidden (403)");
        }

        // Check for scanning patterns (rapid requests to non-existent resources)
        if (dto.getStatus() != null && dto.getStatus() == 404) {
            if (uri != null && (uri.contains("admin") || uri.contains("wp-") || uri.contains("phpmyadmin"))) {
                threats.add("SCANNING");
                score += 20;
                matchedPatternDetails.add("Scanning for common admin paths");
            }
        }

        // Set results if threats detected
        if (!threats.isEmpty()) {
            dto.setIsSuspicious(true);
            dto.setThreatType(String.join(", ", threats));
            dto.setThreatScore(Math.min(score, 100)); // Cap at 100
            dto.setMatchedPatterns(String.join("; ", matchedPatternDetails));
            dto.setSecurityAnalysis(generateAnalysisDescription(threats, score));
        } else {
            dto.setIsSuspicious(false);
            dto.setThreatScore(0);
        }
    }

    /**
     * Detect if input matches any pattern in the list
     */
    private boolean detectPattern(String input, List<Pattern> patterns) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(input).find());
    }

    /**
     * Generate human-readable security analysis description
     */
    private String generateAnalysisDescription(List<String> threats, int score) {
        StringBuilder sb = new StringBuilder();

        if (score >= 80) {
            sb.append("CRITICAL: ");
        } else if (score >= 50) {
            sb.append("HIGH: ");
        } else if (score >= 30) {
            sb.append("MEDIUM: ");
        } else {
            sb.append("LOW: ");
        }

        sb.append("Detected ");
        sb.append(threats.size());
        sb.append(" threat");
        if (threats.size() > 1) {
            sb.append("s");
        }
        sb.append(": ");
        sb.append(String.join(", ", threats));
        sb.append(" (Score: ").append(score).append("/100)");

        return sb.toString();
    }
}
