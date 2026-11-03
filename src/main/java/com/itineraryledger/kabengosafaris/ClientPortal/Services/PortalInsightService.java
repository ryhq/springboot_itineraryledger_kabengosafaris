package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.ClientPortal.DTOs.PortalAdminDTOs;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalJourney;
import com.itineraryledger.kabengosafaris.ClientPortal.Entity.PortalParticipant;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.PortalSection;
import com.itineraryledger.kabengosafaris.ClientPortal.Enums.ProposalState;
import com.itineraryledger.kabengosafaris.ClientPortal.Repository.PortalAccessEventRepository;

import lombok.RequiredArgsConstructor;

/**
 * What the access log is actually worth knowing.
 *
 * <p>The log itself has been accumulating since the portal shipped and nobody reads it, because
 * reading it means scrolling a list of page views looking for a pattern. The three questions it
 * can answer without guessing are worth answering directly:
 *
 * <ul>
 *   <li><b>Has anybody opened it?</b> A quotation sent and never opened looks exactly like one
 *       being thought about, and the two want opposite actions. This is the figure the whole
 *       file exists for.</li>
 *   <li><b>Did they get stuck getting in?</b> Somebody who asked for a code three times and
 *       never verified has it in a spam folder. Nothing else in the application can tell you
 *       that, and it is the most common support call a portal produces.</li>
 *   <li><b>What did they keep coming back to?</b> Four visits to the quotation and none to the
 *       invoice is a different conversation from the reverse.</li>
 * </ul>
 *
 * <p><b>Our own clicks are not their interest.</b> The same table holds a consultant freezing
 * the quotation, the office taking a proposal over and a link being withdrawn. Counting rows
 * would fold those into the customer's activity and produce a number that is confidently wrong,
 * which is worse than no number. Every count here is filtered by {@link PortalSection.Kind}, and
 * that is the reason the section is an enum rather than a string.
 *
 * <p><b>Nothing is inferred beyond what a row states.</b> There is no engagement score, no
 * likelihood to book, no "interest level". Those are invented numbers wearing a figure's
 * clothes, and somebody would price a trip by one. Every line this returns is a count, a
 * timestamp, or a sentence that names the count it came from.
 */
@Service
@RequiredArgsConstructor
public class PortalInsightService {

    private final PortalAccessEventRepository events;

    /** The sections that mean the customer, as stored keys. */
    private static final Set<String> THEIRS = keysOf(PortalSection::isTheirs);
    private static final Set<String> READING = keysOf(PortalSection::isTheClientLooking);

    private static Set<String> keysOf(java.util.function.Predicate<PortalSection> test) {
        Set<String> keys = new java.util.LinkedHashSet<>();
        for (PortalSection section : PortalSection.values()) {
            if (test.test(section)) keys.add(section.name());
        }
        return keys;
    }

    @Transactional(readOnly = true)
    public PortalAdminDTOs.Insight forJourney(PortalJourney journey) {
        Long id = journey.getId();

        Map<PortalSection, Tally> bySection = new LinkedHashMap<>();
        for (Object[] row : events.countBySection(id)) {
            PortalSection section = PortalSection.of((String) row[0]);
            Tally tally = bySection.computeIfAbsent(section, k -> new Tally());
            tally.add(count(row[1]), (LocalDateTime) row[2], (LocalDateTime) row[3]);
        }

        long reads = sum(bySection, PortalSection::isTheClientLooking);
        long actions = sum(bySection, PortalSection::isTheClientActing);
        LocalDateTime firstRead = earliest(bySection, PortalSection::isTheClientLooking);
        LocalDateTime lastTheirs = events.lastOf(id, THEIRS);

        List<PortalAdminDTOs.SectionCount> sections = bySection.entrySet().stream()
            .filter(entry -> entry.getKey().isTheirs())
            .sorted(Comparator.comparingLong((Map.Entry<PortalSection, Tally> e) -> e.getValue().count).reversed())
            .map(entry -> new PortalAdminDTOs.SectionCount(
                entry.getKey().name(),
                entry.getKey().getLabel(),
                entry.getKey().getKind().name(),
                entry.getValue().count,
                entry.getValue().last))
            .toList();

        return new PortalAdminDTOs.Insight(
            reads > 0,
            firstRead,
            lastTheirs,
            daysSince(lastTheirs),
            reads,
            actions,
            sections,
            people(journey),
            signals(journey, bySection, reads, lastTheirs));
    }

    // ------------------------------------------------------------------ per person

    private List<PortalAdminDTOs.PersonActivity> people(PortalJourney journey) {
        Map<Long, Tally> byPerson = new HashMap<>();
        for (Object[] row : events.countByParticipant(journey.getId(), THEIRS)) {
            Tally tally = byPerson.computeIfAbsent((Long) row[0], k -> new Tally());
            tally.add(count(row[1]), (LocalDateTime) row[2], (LocalDateTime) row[3]);
        }

        List<PortalAdminDTOs.PersonActivity> rows = new ArrayList<>();
        for (PortalParticipant person : journey.getParticipants()) {
            Tally tally = byPerson.get(person.getId());
            rows.add(new PortalAdminDTOs.PersonActivity(
                person.getEmail(),
                person.getParticipantRole() == null ? null : person.getParticipantRole().name(),
                person.isVerified(),
                Boolean.TRUE.equals(person.getIsActive()),
                tally == null ? 0 : tally.count,
                tally == null ? null : tally.first,
                tally == null ? null : tally.last));
        }
        return rows;
    }

    // ------------------------------------------------------------------ the sentences

    /**
     * Things worth telling somebody, each one a fact the rows support exactly.
     *
     * <p>Order matters: the first is what a consultant should do about this booking today. None
     * of them is produced unless the count behind it is there to be quoted back.
     */
    private List<String> signals(PortalJourney journey, Map<PortalSection, Tally> bySection,
                                 long reads, LocalDateTime lastTheirs) {
        List<String> signals = new ArrayList<>();

        long codesSent = countOf(bySection, PortalSection.VERIFY_CODE_SENT);
        long verified = countOf(bySection, PortalSection.VERIFY_OK);
        long wrongCodes = countOf(bySection, PortalSection.VERIFY_FAILED);
        long strangers = countOf(bySection, PortalSection.VERIFY_UNKNOWN_ADDRESS);

        if (reads == 0) {
            Long days = daysSince(journey.getCreatedAt());
            signals.add(days == null
                ? "Nobody has opened this link yet."
                : "Nobody has opened this link in the " + plural(days, "day") + " since it was sent.");
        }

        /*
         * The code is in a spam folder, almost always. Worth saying plainly because the client
         * will not ring up about it: from their side the website simply did not work.
         */
        if (verified == 0 && codesSent > 0) {
            signals.add("A code has been sent " + plural(codesSent, "time")
                + " and nobody has got in. Check it is not in their spam folder, or read it to "
                + "them over the telephone.");
        }

        if (wrongCodes >= 3) {
            signals.add(plural(wrongCodes, "wrong code") + " entered. An old code from an earlier "
                + "email will do this: only the most recent one works.");
        }

        if (strangers > 0) {
            signals.add("Somebody tried " + plural(strangers, "address")
                + " that is not on this booking. If that was not the customer, withdraw the link.");
        }

        /*
         * We sent it back and they have not looked. Only stated when there is a moment to
         * measure from, because "unread" with no sent-back date is just "unopened" again.
         */
        if (journey.getProposalState() == ProposalState.RETURNED
                && journey.getProposalStateAt() != null) {
            long since = events.countSince(journey.getId(), READING, journey.getProposalStateAt());
            if (since == 0) {
                Long days = daysSince(journey.getProposalStateAt());
                signals.add("Sent back to them" + (days == null ? "" : " " + plural(days, "day") + " ago")
                    + " and not opened since.");
            }
        }

        if (reads > 0 && lastTheirs != null) {
            Long quiet = daysSince(lastTheirs);
            if (quiet != null && quiet >= 7) {
                signals.add("Nothing from them for " + plural(quiet, "day") + ".");
            }
        }

        return signals;
    }

    // ------------------------------------------------------------------ small things

    private static final class Tally {
        long count;
        LocalDateTime first;
        LocalDateTime last;

        void add(long n, LocalDateTime from, LocalDateTime to) {
            count += n;
            if (from != null && (first == null || from.isBefore(first))) first = from;
            if (to != null && (last == null || to.isAfter(last))) last = to;
        }
    }

    private long sum(Map<PortalSection, Tally> bySection,
                     java.util.function.Predicate<PortalSection> test) {
        return bySection.entrySet().stream()
            .filter(entry -> test.test(entry.getKey()))
            .mapToLong(entry -> entry.getValue().count)
            .sum();
    }

    private LocalDateTime earliest(Map<PortalSection, Tally> bySection,
                                   java.util.function.Predicate<PortalSection> test) {
        return bySection.entrySet().stream()
            .filter(entry -> test.test(entry.getKey()))
            .map(entry -> entry.getValue().first)
            .filter(java.util.Objects::nonNull)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }

    private long countOf(Map<PortalSection, Tally> bySection, PortalSection section) {
        Tally tally = bySection.get(section);
        return tally == null ? 0 : tally.count;
    }

    private static long count(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    /** Null rather than zero when there is nothing to measure from. */
    private static Long daysSince(LocalDateTime moment) {
        return moment == null ? null : Duration.between(moment, LocalDateTime.now()).toDays();
    }

    private static String plural(long n, String word) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    /** Used by the panel to label a log line it does not recognise. */
    public static Map<String, String> sectionLabels() {
        return java.util.Arrays.stream(PortalSection.values())
            .collect(Collectors.toMap(Enum::name, PortalSection::getLabel,
                (a, b) -> a, LinkedHashMap::new));
    }
}
