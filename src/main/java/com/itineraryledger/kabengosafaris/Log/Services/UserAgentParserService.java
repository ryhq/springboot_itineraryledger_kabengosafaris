package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing User-Agent strings
 *
 * Extracts:
 * - Browser name and version
 * - Operating system
 * - Device type (desktop, mobile, tablet)
 *
 * NOTE: This is a lightweight implementation. For production use,
 * consider adding the UserAgentUtils library or UAParser library.
 */
@Service
@Slf4j
public class UserAgentParserService {

    // Browser patterns
    private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/([\\d.]+)");
    private static final Pattern FIREFOX_PATTERN = Pattern.compile("Firefox/([\\d.]+)");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("Version/([\\d.]+).*Safari");
    private static final Pattern EDGE_PATTERN = Pattern.compile("Edg/([\\d.]+)");
    private static final Pattern OPERA_PATTERN = Pattern.compile("OPR/([\\d.]+)");
    private static final Pattern IE_PATTERN = Pattern.compile("MSIE ([\\d.]+)");
    private static final Pattern IE11_PATTERN = Pattern.compile("Trident/.*rv:([\\d.]+)");

    // OS patterns
    private static final Pattern WINDOWS_PATTERN = Pattern.compile("Windows NT ([\\d.]+)");
    private static final Pattern MAC_PATTERN = Pattern.compile("Mac OS X ([\\d_]+)");
    private static final Pattern LINUX_PATTERN = Pattern.compile("Linux");
    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android ([\\d.]+)");
    private static final Pattern IOS_PATTERN = Pattern.compile("iPhone OS ([\\d_]+)");
    private static final Pattern IPAD_PATTERN = Pattern.compile("iPad");

    // Device type patterns
    private static final Pattern MOBILE_PATTERN = Pattern.compile("Mobile|Android|iPhone");
    private static final Pattern TABLET_PATTERN = Pattern.compile("Tablet|iPad");

    /**
     * Parse user agent string and extract information
     * Enriches the DTO with browser, OS, and device info
     *
     * @param dto the access log DTO to enrich
     */
    public void parse(AccessLogDTO dto) {
        String userAgent = dto.getUserAgent();

        if (userAgent == null || userAgent.isEmpty() || userAgent.equals("-")) {
            dto.setDeviceType("UNKNOWN");
            return;
        }

        try {
            // Parse browser
            parseBrowser(dto, userAgent);

            // Parse OS
            parseOperatingSystem(dto, userAgent);

            // Parse device type
            parseDeviceType(dto, userAgent);

        } catch (Exception e) {
            log.debug("Failed to parse user agent: {}", userAgent, e);
            dto.setDeviceType("UNKNOWN");
        }
    }

    /**
     * Parse browser name and version from user agent
     */
    private void parseBrowser(AccessLogDTO dto, String userAgent) {
        Matcher matcher;

        // Check for Edge (must be before Chrome as it contains "Chrome")
        matcher = EDGE_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Microsoft Edge");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Check for Opera (must be before Chrome as it contains "Chrome")
        matcher = OPERA_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Opera");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Check for Chrome
        matcher = CHROME_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Chrome");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Check for Firefox
        matcher = FIREFOX_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Firefox");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Check for Safari
        matcher = SAFARI_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Safari");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Check for IE 11
        matcher = IE11_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Internet Explorer");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Check for older IE versions
        matcher = IE_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setBrowserName("Internet Explorer");
            dto.setBrowserVersion(matcher.group(1));
            return;
        }

        // Unknown browser
        dto.setBrowserName("Unknown");
    }

    /**
     * Parse operating system from user agent
     */
    private void parseOperatingSystem(AccessLogDTO dto, String userAgent) {
        Matcher matcher;

        // Check for Windows
        matcher = WINDOWS_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            String version = matcher.group(1);
            dto.setOperatingSystem("Windows " + mapWindowsVersion(version));
            return;
        }

        // Check for Android
        matcher = ANDROID_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            dto.setOperatingSystem("Android " + matcher.group(1));
            return;
        }

        // Check for iOS (iPhone)
        matcher = IOS_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            String version = matcher.group(1).replace("_", ".");
            dto.setOperatingSystem("iOS " + version);
            return;
        }

        // Check for iPad
        if (IPAD_PATTERN.matcher(userAgent).find()) {
            dto.setOperatingSystem("iPadOS");
            return;
        }

        // Check for Mac
        matcher = MAC_PATTERN.matcher(userAgent);
        if (matcher.find()) {
            String version = matcher.group(1).replace("_", ".");
            dto.setOperatingSystem("macOS " + version);
            return;
        }

        // Check for Linux
        if (LINUX_PATTERN.matcher(userAgent).find()) {
            dto.setOperatingSystem("Linux");
            return;
        }

        // Unknown OS
        dto.setOperatingSystem("Unknown");
    }

    /**
     * Parse device type from user agent
     */
    private void parseDeviceType(AccessLogDTO dto, String userAgent) {
        // Check for mobile
        if (MOBILE_PATTERN.matcher(userAgent).find()) {
            dto.setDeviceType("MOBILE");
            return;
        }

        // Check for tablet
        if (TABLET_PATTERN.matcher(userAgent).find()) {
            dto.setDeviceType("TABLET");
            return;
        }

        // Check if it's a bot (already detected)
        if (dto.getIsBot() != null && dto.getIsBot()) {
            dto.setDeviceType("BOT");
            return;
        }

        // Default to desktop
        dto.setDeviceType("DESKTOP");
    }

    /**
     * Map Windows NT version to friendly name
     */
    private String mapWindowsVersion(String ntVersion) {
        return switch (ntVersion) {
            case "10.0" -> "10/11";
            case "6.3" -> "8.1";
            case "6.2" -> "8";
            case "6.1" -> "7";
            case "6.0" -> "Vista";
            case "5.2" -> "XP 64-bit";
            case "5.1" -> "XP";
            case "5.0" -> "2000";
            default -> ntVersion;
        };
    }
}
