package com.itineraryledger.kabengosafaris.Attribution;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Turns the tags a browser collected into something the office can count.
 *
 * Two jobs, and they are separate on purpose:
 *
 *  1. Distrust. Every field arrived in a query string, which anybody can write.
 *     Each is stripped of control characters and angle brackets, capped to its
 *     column, and a URL that is not http(s) is dropped rather than stored.
 *  2. Derivation. The channel is worked out here and only here, so the rule lives
 *     in one place, can be corrected, and can be re-run over rows already stored.
 */
@Service
@Slf4j
public class AttributionService {

    /*
     * Answer engines. Checked FIRST, because their hosts sit underneath a search
     * brand: gemini.google.com and bard.google.com would otherwise be counted as
     * Google search, and the whole point is to tell those apart.
     */
    private static final Set<String> AI_BRANDS = brands(
        "chatgpt", "openai", "perplexity", "claude", "anthropic", "copilot",
        "gemini", "bard", "poe", "deepseek", "grok", "phind", "mistral",
        "monica", "iask", "komo", "andisearch", "you.com", "meta.ai");

    private static final Set<String> SEARCH_BRANDS = brands(
        "google", "bing", "duckduckgo", "yahoo", "yandex", "baidu", "ecosia",
        "brave", "startpage", "qwant", "naver", "seznam", "kagi", "ask", "aol",
        "searx", "mojeek", "lycos");

    private static final Set<String> SOCIAL_BRANDS = brands(
        "facebook", "fb", "instagram", "ig", "tiktok", "youtube", "twitter", "x",
        "linkedin", "pinterest", "reddit", "snapchat", "whatsapp", "telegram",
        "tumblr", "vk", "weibo", "wechat", "discord", "threads", "mastodon",
        "quora", "flipboard", "messenger", "line", "meta");

    /*
     * Click identifiers that only ever exist on a click that was paid for. These
     * outrank utm_medium: a mistagged medium is common, a forged gclid is not.
     */
    private static final Set<String> PAID_SEARCH_CLICK_IDS =
        brands("gclid", "gbraid", "wbraid", "msclkid", "yclid");
    private static final Set<String> PAID_DISPLAY_CLICK_IDS = brands("dclid");
    private static final Set<String> PAID_SOCIAL_CLICK_IDS =
        brands("ttclid", "twclid", "li_fat_id", "sccid", "epik", "rdt_cid", "obclid", "taboolaclickid");

    /*
     * fbclid is deliberately NOT in that set. Facebook and Instagram stamp it on
     * every outbound link, an organic post as readily as an ad, so treating it as
     * proof of spend would invent paid conversions that were never bought.
     */
    private static final Set<String> AMBIGUOUS_SOCIAL_CLICK_IDS = brands("fbclid", "igshid");

    private static final Set<String> PAID_SEARCH_MEDIUMS =
        brands("cpc", "ppc", "paidsearch", "paid_search", "paid-search", "sem", "adwords", "google_ads");
    private static final Set<String> PAID_SOCIAL_MEDIUMS =
        brands("paidsocial", "paid_social", "paid-social", "social_paid", "socialpaid", "cpv", "paid_influencer");
    private static final Set<String> PAID_DISPLAY_MEDIUMS =
        brands("display", "banner", "cpm", "programmatic", "retargeting", "remarketing", "native");
    private static final Set<String> EMAIL_MEDIUMS =
        brands("email", "e-mail", "mail", "newsletter", "mailing", "edm");
    private static final Set<String> ORGANIC_SEARCH_MEDIUMS = brands("organic", "seo");
    private static final Set<String> SOCIAL_MEDIUMS =
        brands("social", "social-organic", "social_organic", "organic-social", "organic_social", "sm");
    private static final Set<String> AFFILIATE_MEDIUMS =
        brands("affiliate", "partner", "referral_partner", "cpa");
    private static final Set<String> REFERRAL_MEDIUMS = brands("referral", "link");

    private static Set<String> brands(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    /** Null in, null out — a form posted without tags stores nothing rather than a row of blanks. */
    public Attribution from(AttributionRequest request) {
        if (request == null) return null;

        Attribution a = new Attribution();
        a.setSource(clean(request.getSource(), 120));
        a.setMedium(clean(request.getMedium(), 120));
        a.setCampaign(clean(request.getCampaign(), 180));
        a.setContent(clean(request.getContent(), 180));
        a.setTerm(clean(request.getTerm(), 180));
        a.setClickId(clean(request.getClickId(), 255));
        a.setClickIdType(clean(request.getClickIdType(), 20));
        a.setLandingPage(cleanUrl(request.getLandingPage(), 500));
        a.setReferrer(cleanUrl(request.getReferrer(), 500));

        a.setFirstSource(clean(request.getFirstSource(), 120));
        a.setFirstMedium(clean(request.getFirstMedium(), 120));
        a.setFirstCampaign(clean(request.getFirstCampaign(), 180));
        a.setFirstSeenAt(parseInstant(request.getFirstSeenAt()));
        a.setTouchCount(saneCount(request.getTouchCount()));

        /*
         * Emptiness is decided BEFORE the channel is derived, not after.
         *
         * resolveChannel answers DIRECT when it is given nothing, which is right for a
         * visitor who really did type the address — the page still records the page they
         * landed on, so a real direct visit is never empty here. But a form posted with
         * no attribution at all (a browser with storage blocked, an older page, a caller
         * that omits the field) would otherwise be stamped DIRECT, and that is a claim
         * about where somebody came from made on the strength of no evidence. It has to
         * stay distinguishable from "not recorded", or the channel figures quietly
         * absorb every lead nobody measured.
         */
        boolean nothingCaptured = a.getSource() == null
            && a.getMedium() == null
            && a.getCampaign() == null
            && a.getContent() == null
            && a.getTerm() == null
            && a.getClickId() == null
            && a.getLandingPage() == null
            && a.getReferrer() == null
            && a.getFirstSource() == null
            && a.getFirstMedium() == null
            && a.getFirstCampaign() == null;
        if (nothingCaptured) return null;

        a.setChannel(resolveChannel(a.getSource(), a.getMedium(), a.getClickIdType(), a.getReferrer()));

        /*
         * The first touch carries no click id or referrer of its own — only the
         * three tags worth keeping for months — so it is resolved from those alone.
         * When nothing was kept, it is the same visit as the last touch.
         */
        if (a.getFirstSource() == null && a.getFirstMedium() == null) {
            a.setFirstChannel(a.getChannel());
            a.setFirstSource(a.getSource());
            a.setFirstMedium(a.getMedium());
            a.setFirstCampaign(a.getCampaign());
        } else {
            a.setFirstChannel(resolveChannel(a.getFirstSource(), a.getFirstMedium(), null, null));
        }

        return a.isEmpty() ? null : a;
    }

    /**
     * A second visit from the same person, without losing where they first came from.
     *
     * Somebody who subscribed months ago and comes back through an ad has converted for
     * that ad, so the last touch is replaced. What introduced them has not changed, so
     * the first touch is kept whenever it was already recorded. Overwriting it would
     * quietly rewrite history in favour of whatever campaign ran most recently, which is
     * the exact bias attribution is supposed to correct for.
     */
    public Attribution merge(Attribution existing, Attribution incoming) {
        if (existing == null) return incoming;
        if (incoming == null) return existing;

        if (existing.getFirstChannel() != null || existing.getFirstSource() != null) {
            incoming.setFirstChannel(existing.getFirstChannel());
            incoming.setFirstSource(existing.getFirstSource());
            incoming.setFirstMedium(existing.getFirstMedium());
            incoming.setFirstCampaign(existing.getFirstCampaign());
        }
        if (existing.getFirstSeenAt() != null
                && (incoming.getFirstSeenAt() == null || existing.getFirstSeenAt().isBefore(incoming.getFirstSeenAt()))) {
            incoming.setFirstSeenAt(existing.getFirstSeenAt());
        }
        int existingTouches = existing.getTouchCount() != null ? existing.getTouchCount() : 0;
        int incomingTouches = incoming.getTouchCount() != null ? incoming.getTouchCount() : 0;
        int touches = Math.max(existingTouches, incomingTouches);
        if (touches > 0) incoming.setTouchCount(touches);

        return incoming;
    }

    /**
     * The channel rule, in the order the signals deserve to be trusted.
     *
     * A paid click id first because it cannot be tagged by accident; the declared
     * medium next because somebody meant it; the source's identity after that; and
     * the referrer only when nothing was tagged at all.
     */
    public AcquisitionChannel resolveChannel(String source, String medium, String clickIdType, String referrer) {
        String kind = lower(clickIdType);
        if (PAID_SEARCH_CLICK_IDS.contains(kind)) return AcquisitionChannel.PAID_SEARCH;
        if (PAID_DISPLAY_CLICK_IDS.contains(kind)) return AcquisitionChannel.PAID_DISPLAY;
        if (PAID_SOCIAL_CLICK_IDS.contains(kind)) return AcquisitionChannel.PAID_SOCIAL;

        String med = lower(medium);
        if (!med.isEmpty()) {
            if (PAID_SEARCH_MEDIUMS.contains(med)) return AcquisitionChannel.PAID_SEARCH;
            if (PAID_SOCIAL_MEDIUMS.contains(med)) return AcquisitionChannel.PAID_SOCIAL;
            if (PAID_DISPLAY_MEDIUMS.contains(med)) return AcquisitionChannel.PAID_DISPLAY;
            if (EMAIL_MEDIUMS.contains(med)) return AcquisitionChannel.EMAIL;
            if (AFFILIATE_MEDIUMS.contains(med)) return AcquisitionChannel.AFFILIATE;
        }

        Set<String> sourceTokens = tokensOf(source);
        if (matches(sourceTokens, AI_BRANDS)) return AcquisitionChannel.AI_ASSISTANT;

        if (!med.isEmpty()) {
            if (SOCIAL_MEDIUMS.contains(med)) return AcquisitionChannel.ORGANIC_SOCIAL;
            if (ORGANIC_SEARCH_MEDIUMS.contains(med)) {
                return matches(sourceTokens, SOCIAL_BRANDS)
                    ? AcquisitionChannel.ORGANIC_SOCIAL
                    : AcquisitionChannel.ORGANIC_SEARCH;
            }
            if (REFERRAL_MEDIUMS.contains(med)) return AcquisitionChannel.REFERRAL;
        }

        if (matches(sourceTokens, SEARCH_BRANDS)) return AcquisitionChannel.ORGANIC_SEARCH;
        if (matches(sourceTokens, SOCIAL_BRANDS)) return AcquisitionChannel.ORGANIC_SOCIAL;

        if (AMBIGUOUS_SOCIAL_CLICK_IDS.contains(kind)) return AcquisitionChannel.ORGANIC_SOCIAL;

        if (!sourceTokens.isEmpty()) return AcquisitionChannel.OTHER;

        Set<String> referrerTokens = tokensOf(referrer);
        if (matches(referrerTokens, AI_BRANDS)) return AcquisitionChannel.AI_ASSISTANT;
        if (matches(referrerTokens, SEARCH_BRANDS)) return AcquisitionChannel.ORGANIC_SEARCH;
        if (matches(referrerTokens, SOCIAL_BRANDS)) return AcquisitionChannel.ORGANIC_SOCIAL;
        if (!referrerTokens.isEmpty()) return AcquisitionChannel.REFERRAL;

        return AcquisitionChannel.DIRECT;
    }

    /** For the panel. Null in, null out, so "not recorded" stays distinguishable from "direct". */
    public AttributionDTO toDto(Attribution a) {
        if (a == null || a.isEmpty()) return null;

        boolean multiTouch = a.getFirstChannel() != null
            && a.getChannel() != null
            && a.getFirstChannel() != a.getChannel();

        return AttributionDTO.builder()
            .channel(a.getChannel())
            .channelDisplayName(a.getChannel() != null ? a.getChannel().getDisplayName() : null)
            .source(a.getSource())
            .medium(a.getMedium())
            .campaign(a.getCampaign())
            .content(a.getContent())
            .term(a.getTerm())
            .clickId(a.getClickId())
            .clickIdType(a.getClickIdType())
            .landingPage(a.getLandingPage())
            .referrer(a.getReferrer())
            .firstChannel(a.getFirstChannel())
            .firstChannelDisplayName(a.getFirstChannel() != null ? a.getFirstChannel().getDisplayName() : null)
            .firstSource(a.getFirstSource())
            .firstMedium(a.getFirstMedium())
            .firstCampaign(a.getFirstCampaign())
            .firstSeenAt(a.getFirstSeenAt())
            .touchCount(a.getTouchCount())
            .multiTouch(multiTouch)
            .paid(a.getChannel() != null && a.getChannel().isPaid())
            .build();
    }

    /* ---------- distrust ---------- */

    /**
     * Angle brackets and control characters go, because this text is written by a
     * stranger and read back in the panel and in the notification mail. Escaping at
     * every render point is a promise nobody keeps; not storing them is one edit.
     */
    static String clean(String value, int maxLength) {
        if (value == null) return null;
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isISOControl(c) || c == '<' || c == '>') continue;
            out.append(c);
        }
        String cleaned = out.toString().replaceAll("\\s+", " ").trim();
        if (cleaned.isEmpty()) return null;
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }

    /** Only http(s) survives, so a {@code javascript:} URL can never reach a rendered href. */
    static String cleanUrl(String value, int maxLength) {
        String cleaned = clean(value, maxLength);
        if (cleaned == null) return null;
        String lower = cleaned.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) return cleaned;
        /*
         * A bare path is what a landing page usually is and carries no scheme to
         * abuse, but "//host" is protocol-relative and would navigate off-site from
         * any href it reached, so only a single leading slash is accepted.
         */
        if (cleaned.startsWith("/") && !cleaned.startsWith("//")) return cleaned;
        return null;
    }

    /** A count out of range says the page is wrong, so it is dropped rather than believed. */
    static Integer saneCount(Integer value) {
        if (value == null || value < 1 || value > 10_000) return null;
        return value;
    }

    static LocalDateTime parseInstant(String value) {
        String cleaned = clean(value, 40);
        if (cleaned == null) return null;
        try {
            return OffsetDateTime.parse(cleaned).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(cleaned);
            } catch (DateTimeParseException e) {
                log.debug("Unparseable first-seen timestamp on an inbound lead: {}", cleaned);
                return null;
            }
        }
    }

    /* ---------- identity ---------- */

    private static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    /**
     * Every name a value could be recognised by: the whole string, its host, and
     * each label of that host. "www.google.de" therefore answers to "google", and
     * a source written plainly as "google" answers to the same token.
     */
    static Set<String> tokensOf(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        String v = lower(value);
        if (v.isEmpty()) return tokens;

        tokens.add(v);

        String host = v;
        if (host.contains("://")) {
            try {
                String parsed = new URI(v).getHost();
                if (parsed != null) host = parsed.toLowerCase();
            } catch (Exception ignored) {
                host = v.substring(v.indexOf("://") + 3);
            }
        }
        int slash = host.indexOf('/');
        if (slash >= 0) host = host.substring(0, slash);
        if (host.startsWith("www.")) host = host.substring(4);
        if (!host.isEmpty()) tokens.add(host);

        for (String label : host.split("\\.")) {
            if (!label.isBlank()) tokens.add(label);
        }
        return tokens;
    }

    private static boolean matches(Set<String> tokens, Set<String> brands) {
        for (String token : tokens) {
            if (brands.contains(token)) return true;
        }
        return false;
    }
}
