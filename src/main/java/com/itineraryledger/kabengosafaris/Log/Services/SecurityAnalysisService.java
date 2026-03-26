package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
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
    // SQL INJECTION PATTERNS (applied to decoded URI path + param values, not raw query string)
    // ==========================================
    private static final List<Pattern> SQL_PATTERNS = Arrays.asList(
        // SQL keywords in URI path or param values (not in normal param names)
        Pattern.compile("\\b(SELECT|UNION|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|DECLARE)\\b", Pattern.CASE_INSENSITIVE),
        // SQL comments
        Pattern.compile("(--|/\\*|\\*/)", Pattern.CASE_INSENSITIVE),
        // Classic SQL injection: ' OR '1'='1, ' OR 1=1 --
        Pattern.compile("'\\s*OR\\s*'?1'?\\s*=\\s*'?1", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\bUNION\\b.*\\bSELECT\\b", Pattern.CASE_INSENSITIVE),
        // Tautology: OR 1=1, AND 1=1
        Pattern.compile("\\b(OR|AND)\\s+\\d+\\s*=\\s*\\d+", Pattern.CASE_INSENSITIVE),
        // URL-encoded single quote injection
        Pattern.compile("%27.*(%3D|=|%4F%52|OR|%41%4E%44|AND)", Pattern.CASE_INSENSITIVE)
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
        // Event handlers in HTML attributes
        Pattern.compile("\\bon(load|error|click|mouseover|keyup|focus|blur|change|submit)\\s*=", Pattern.CASE_INSENSITIVE),
        // Iframe and embed tags
        Pattern.compile("<(iframe|embed|object|frame)", Pattern.CASE_INSENSITIVE),
        // Data URIs
        Pattern.compile("data:text/html", Pattern.CASE_INSENSITIVE),
        // Encoded script tags
        Pattern.compile("%3Cscript", Pattern.CASE_INSENSITIVE)
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
    // COMMAND INJECTION PATTERNS (applied to URI path only, NOT query string)
    // ==========================================
    private static final List<Pattern> COMMAND_INJECTION_PATTERNS = Arrays.asList(
        // Shell command chaining in URI path
        Pattern.compile("(;|\\|\\||&&)\\s*\\w+", Pattern.CASE_INSENSITIVE),
        // Backtick command substitution
        Pattern.compile("`[^`]+`"),
        // $() command substitution
        Pattern.compile("\\$\\([^)]+\\)"),
        // URL-encoded shell operators
        Pattern.compile("(%3B|%7C%7C|%26%26)\\s*\\w+", Pattern.CASE_INSENSITIVE)
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

        Set<String> threats = new LinkedHashSet<>();
        int score = 0;
        List<String> matchedPatternDetails = new ArrayList<>();

        String uri = dto.getRequestUri();
        String userAgent = dto.getUserAgent();

        // Check URI for threats
        if (uri != null && !uri.isEmpty()) {
            // Split URI into path and query string
            String uriPath = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
            String queryString = uri.contains("?") ? uri.substring(uri.indexOf('?') + 1) : "";

            // Analyze path for all threat types
            // SQL Injection (check path + decoded query param values, not keys like sortBy=)
            String paramValues = extractQueryParamValues(queryString);
            String analysisTarget = uriPath + " " + paramValues;
            if (detectPattern(analysisTarget, SQL_PATTERNS)) {
                threats.add("SQL_INJECTION");
                score += 40;
                matchedPatternDetails.add("SQL injection pattern in URI");
            }

            // XSS (check path + param values)
            if (detectPattern(analysisTarget, XSS_PATTERNS)) {
                threats.add("XSS");
                score += 35;
                matchedPatternDetails.add("XSS pattern in URI");
            }

            // Path Traversal (check full URI — path traversal can appear anywhere)
            if (detectPattern(uri, PATH_TRAVERSAL_PATTERNS)) {
                threats.add("PATH_TRAVERSAL");
                score += 30;
                matchedPatternDetails.add("Path traversal pattern in URI");
            }

            // Command Injection (check path only — & in query strings is normal)
            if (detectPattern(uriPath, COMMAND_INJECTION_PATTERNS)) {
                threats.add("COMMAND_INJECTION");
                score += 35;
                matchedPatternDetails.add("Command injection pattern in URI path");
            }
        }

        // Check User Agent for suspicious patterns
        if (userAgent != null && !userAgent.isEmpty() && !userAgent.equals("-")) {
            if (detectPattern(userAgent, SUSPICIOUS_USER_AGENT_PATTERNS)) {
                threats.add("SUSPICIOUS_USER_AGENT");
                score += 25;
                matchedPatternDetails.add("Known attack tool user agent");
            } else if (userAgent.length() < 10 && !isKnownShortUserAgent(userAgent)) {
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
            List<String> threatList = new ArrayList<>(threats);
            dto.setIsSuspicious(true);
            dto.setThreatType(String.join(", ", threatList));
            dto.setThreatScore(Math.min(score, 100)); // Cap at 100
            dto.setMatchedPatterns(String.join("; ", matchedPatternDetails));
            dto.setSecurityAnalysis(generateAnalysisDescription(threatList, score));
        } else {
            dto.setIsSuspicious(false);
            dto.setThreatScore(0);
        }
    }

    /**
     * Extract only the values from query parameters (not keys) for analysis.
     * e.g., "page=0&sortBy=timestamp&q=SELECT+*+FROM" → "0 timestamp SELECT * FROM"
     */
    private String extractQueryParamValues(String queryString) {
        if (queryString == null || queryString.isEmpty()) return "";
        StringBuilder values = new StringBuilder();
        for (String param : queryString.split("&")) {
            int eq = param.indexOf('=');
            if (eq >= 0 && eq < param.length() - 1) {
                values.append(param.substring(eq + 1).replace('+', ' ')).append(' ');
            }
        }
        return values.toString().trim();
    }

    /**
     * Check if a short user agent is a known legitimate agent (e.g., Next.js SSR)
     */
    private boolean isKnownShortUserAgent(String userAgent) {
        String lower = userAgent.toLowerCase().trim();
        return lower.equals("node") || lower.equals("node.js") || lower.equals("dart")
            || lower.equals("python") || lower.equals("ruby") || lower.equals("go");
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
