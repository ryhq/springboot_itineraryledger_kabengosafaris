package com.itineraryledger.kabengosafaris.Log.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for detecting and classifying bots
 *
 * Identifies:
 * - Search engine bots (Google, Bing, etc.)
 * - Social media bots (Facebook, Twitter, etc.)
 * - SEO crawlers (Ahrefs, Semrush, etc.)
 * - Malicious scrapers
 * - Automated scripts
 */
@Service
@Slf4j
public class BotDetectionService {

    @Autowired
    private AccessLogSettingGetterServices settings;

    // ==========================================
    // KNOWN BOT SIGNATURES
    // ==========================================
    private static final Map<String, BotInfo> KNOWN_BOTS = Map.ofEntries(
        // Search Engines
        Map.entry("Googlebot", new BotInfo("SEARCH_ENGINE", "Googlebot")),
        Map.entry("Bingbot", new BotInfo("SEARCH_ENGINE", "Bingbot")),
        Map.entry("Slurp", new BotInfo("SEARCH_ENGINE", "Yahoo Slurp")),
        Map.entry("DuckDuckBot", new BotInfo("SEARCH_ENGINE", "DuckDuckGo Bot")),
        Map.entry("Baiduspider", new BotInfo("SEARCH_ENGINE", "Baidu Spider")),
        Map.entry("YandexBot", new BotInfo("SEARCH_ENGINE", "Yandex Bot")),
        Map.entry("Sogou", new BotInfo("SEARCH_ENGINE", "Sogou Spider")),
        Map.entry("Exabot", new BotInfo("SEARCH_ENGINE", "Exabot")),
        Map.entry("archive.org_bot", new BotInfo("SEARCH_ENGINE", "Internet Archive Bot")),

        // Social Media Bots
        Map.entry("facebookexternalhit", new BotInfo("SOCIAL_MEDIA_BOT", "Facebook Bot")),
        Map.entry("Facebot", new BotInfo("SOCIAL_MEDIA_BOT", "Facebook Bot")),
        Map.entry("Twitterbot", new BotInfo("SOCIAL_MEDIA_BOT", "Twitter Bot")),
        Map.entry("LinkedInBot", new BotInfo("SOCIAL_MEDIA_BOT", "LinkedIn Bot")),
        Map.entry("Slackbot", new BotInfo("SOCIAL_MEDIA_BOT", "Slack Bot")),
        Map.entry("TelegramBot", new BotInfo("SOCIAL_MEDIA_BOT", "Telegram Bot")),
        Map.entry("WhatsApp", new BotInfo("SOCIAL_MEDIA_BOT", "WhatsApp Bot")),
        Map.entry("Discordbot", new BotInfo("SOCIAL_MEDIA_BOT", "Discord Bot")),
        Map.entry("Pinterestbot", new BotInfo("SOCIAL_MEDIA_BOT", "Pinterest Bot")),
        Map.entry("Applebot", new BotInfo("SOCIAL_MEDIA_BOT", "Apple Bot")),

        // SEO & Marketing Crawlers
        Map.entry("AhrefsBot", new BotInfo("SEO_CRAWLER", "Ahrefs Bot")),
        Map.entry("SemrushBot", new BotInfo("SEO_CRAWLER", "Semrush Bot")),
        Map.entry("MJ12bot", new BotInfo("SEO_CRAWLER", "Majestic Bot")),
        Map.entry("DotBot", new BotInfo("SEO_CRAWLER", "Moz DotBot")),
        Map.entry("PetalBot", new BotInfo("SEO_CRAWLER", "Petal Bot")),
        Map.entry("BLEXBot", new BotInfo("SEO_CRAWLER", "BLEXBot")),
        Map.entry("SeznamBot", new BotInfo("SEO_CRAWLER", "Seznam Bot")),

        // Monitoring & Uptime Bots
        Map.entry("UptimeRobot", new BotInfo("MONITORING", "Uptime Robot")),
        Map.entry("Pingdom", new BotInfo("MONITORING", "Pingdom")),
        Map.entry("StatusCake", new BotInfo("MONITORING", "StatusCake")),
        Map.entry("Site24x7", new BotInfo("MONITORING", "Site24x7")),

        // Scrapers (potentially malicious)
        Map.entry("Scrapy", new BotInfo("SCRAPER", "Scrapy")),
        Map.entry("HTTrack", new BotInfo("SCRAPER", "HTTrack")),
        Map.entry("WebCopier", new BotInfo("SCRAPER", "WebCopier")),
        Map.entry("WebZIP", new BotInfo("SCRAPER", "WebZIP")),
        Map.entry("WebReaper", new BotInfo("SCRAPER", "WebReaper")),

        // Server-Side Rendering / Internal
        Map.entry("node", new BotInfo("SSR", "Next.js SSR")),
        Map.entry("undici", new BotInfo("SSR", "Node.js Undici")),

        // Automated Scripts
        Map.entry("curl", new BotInfo("SCRIPT", "cURL")),
        Map.entry("wget", new BotInfo("SCRIPT", "Wget")),
        Map.entry("python-requests", new BotInfo("SCRIPT", "Python Requests")),
        Map.entry("Apache-HttpClient", new BotInfo("SCRIPT", "Apache HttpClient")),
        Map.entry("Java/", new BotInfo("SCRIPT", "Java HTTP Client")),
        Map.entry("Go-http-client", new BotInfo("SCRIPT", "Go HTTP Client")),
        Map.entry("libwww-perl", new BotInfo("SCRIPT", "Perl LWP")),
        Map.entry("node-fetch", new BotInfo("SCRIPT", "Node.js Fetch")),
        Map.entry("axios", new BotInfo("SCRIPT", "Axios")),

        // Security Scanners (malicious)
        Map.entry("sqlmap", new BotInfo("MALICIOUS", "SQLMap")),
        Map.entry("Nikto", new BotInfo("MALICIOUS", "Nikto Scanner")),
        Map.entry("Nmap", new BotInfo("MALICIOUS", "Nmap")),
        Map.entry("Masscan", new BotInfo("MALICIOUS", "Masscan")),
        Map.entry("ZmEu", new BotInfo("MALICIOUS", "ZmEu Scanner")),
        Map.entry("w3af", new BotInfo("MALICIOUS", "W3AF Scanner")),
        Map.entry("Acunetix", new BotInfo("MALICIOUS", "Acunetix Scanner")),
        Map.entry("Netsparker", new BotInfo("MALICIOUS", "Netsparker Scanner"))
    );

    // Patterns for heuristic bot detection
    private static final Pattern BOT_PATTERN = Pattern.compile(
        "\\b(bot|crawler|spider|scraper|fetch|scan|check)\\b",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Detect if request is from a bot and classify it
     * Enriches the DTO with bot detection results
     *
     * @param dto the access log DTO to analyze
     */
    public void detect(AccessLogDTO dto) {
        if (!settings.isBotDetectionEnabled()) {
            return;
        }

        String userAgent = dto.getUserAgent();

        // No user agent or empty = suspicious but not classified as bot
        if (userAgent == null || userAgent.isEmpty() || userAgent.equals("-")) {
            return;
        }

        // Check against known bots (prefer longest key match to avoid "node" matching "node-fetch")
        BotInfo bestMatch = null;
        String bestKey = null;
        for (Map.Entry<String, BotInfo> entry : KNOWN_BOTS.entrySet()) {
            if (containsIgnoreCase(userAgent, entry.getKey())) {
                if (bestKey == null || entry.getKey().length() > bestKey.length()) {
                    bestMatch = entry.getValue();
                    bestKey = entry.getKey();
                }
            }
        }
        if (bestMatch != null) {
            dto.setIsBot(true);
            dto.setBotType(bestMatch.type);
            dto.setBotName(bestMatch.name);

            if ("MALICIOUS".equals(bestMatch.type)) {
                log.warn("Malicious bot detected from {}: {} - {}",
                    dto.getRemoteAddress(), bestMatch.name, userAgent);
            } else {
                log.debug("Bot detected from {}: {} ({})",
                    dto.getRemoteAddress(), bestMatch.name, bestMatch.type);
            }
            return;
        }

        // Heuristic detection for unknown bots
        if (BOT_PATTERN.matcher(userAgent).find()) {
            dto.setIsBot(true);
            dto.setBotType("UNKNOWN");
            dto.setBotName("Unknown Bot");
            log.debug("Unknown bot detected from {}: {}", dto.getRemoteAddress(), userAgent);
            return;
        }

        // Additional heuristics
        if (isLikelyBotByBehavior(userAgent)) {
            dto.setIsBot(true);
            dto.setBotType("UNKNOWN");
            dto.setBotName("Likely Bot");
            log.debug("Likely bot detected by behavior from {}: {}", dto.getRemoteAddress(), userAgent);
        }
    }

    /**
     * Case-insensitive contains check
     */
    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }

    /**
     * Detect bots by behavioral patterns
     */
    private boolean isLikelyBotByBehavior(String userAgent) {
        String lower = userAgent.toLowerCase();

        // Very short user agent (< 10 chars excluding "-")
        if (lower.length() < 10 && !lower.equals("-")) {
            return true;
        }

        // Contains common bot keywords
        String[] botKeywords = {
            "crawl", "spider", "scrape", "index", "fetch",
            "monitor", "check", "scan", "test", "probe",
            "validator", "checker", "analyzer", "audit"
        };

        for (String keyword : botKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }

        // Unusual patterns (no browser name or version)
        if (!lower.contains("mozilla") &&
            !lower.contains("chrome") &&
            !lower.contains("safari") &&
            !lower.contains("firefox") &&
            !lower.contains("edge") &&
            !lower.contains("opera")) {
            // Could be a bot or script
            return true;
        }

        return false;
    }

    /**
     * Helper class to store bot information
     */
    private static class BotInfo {
        final String type;
        final String name;

        BotInfo(String type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}
