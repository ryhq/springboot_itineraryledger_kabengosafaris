package com.itineraryledger.kabengosafaris.ClientPortal.Services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteSnapshot;
import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteRevision;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteServices.QuoteRevisionService;

import lombok.RequiredArgsConstructor;

/**
 * Two photographs of a quotation, read out as sentences.
 *
 * <p>This is the one service behind both the client's change list and the office's, and that is
 * the entire point of it. A conversation where each side is looking at its own summary of what
 * the other side did ends with both of them certain, and one of them wrong, about what was
 * agreed. {@code TheClientAndTheOfficeSeeTheSameChangesTest} fails the build if a second path
 * into this ever appears.
 *
 * <p>It reports what somebody <em>booked</em>, not what somebody <em>typed</em>. A lodge, a board
 * basis, a room count, a park, a sector, the party and the price all appear; a reworded day
 * description does not. The distinction is whether the change costs money or changes where
 * somebody sleeps. A change list padded with prose edits is one nobody reads, and the one real
 * line in it goes past unnoticed.
 */
@Service
@RequiredArgsConstructor
public class PortalChangeLogService {

    private final QuoteRevisionService revisions;

    /**
     * One sentence, and what it is about.
     *
     * <p>The scope is carried separately rather than written into the sentence so that a surface
     * can group by it, and so the sentence reads the same whether it is shown under a heading or
     * in a flat list.
     */
    public record Change(String scope, String text) {}

    /**
     * One round of the conversation: what changed, who did it, and when they handed it back.
     *
     * <p>{@code revision} is null for the pending round, which is the work done since the last
     * handover and not yet submitted or returned. The portal shows it to a client as "your
     * changes so far"; without it a client who has edited and not yet submitted is shown a
     * history that does not contain anything they just did.
     */
    public record Round(
        Integer revision,
        String reason,
        String takenBy,
        String authorName,
        LocalDateTime at,
        boolean pending,
        List<Change> changes
    ) {}

    /**
     * The whole conversation, oldest first.
     *
     * <p>The first revision has no predecessor and therefore no changes; it is still listed,
     * because "sent to the customer on the 3rd" is the start of the story and a list that begins
     * at the first disagreement reads as though nothing happened before it.
     */
    @Transactional(readOnly = true)
    public List<Round> forQuote(Quote quote) {
        List<Round> rounds = new ArrayList<>();
        if (quote == null || quote.getId() == null) return rounds;

        List<QuoteRevision> history = revisions.history(quote.getId());
        QuoteSnapshot previous = null;

        for (QuoteRevision revision : history) {
            QuoteSnapshot snapshot = revisions.read(revision);
            rounds.add(new Round(
                revision.getRevisionNumber(),
                revision.getReason() == null ? null : revision.getReason().getDisplayName(),
                revision.getTakenBy() == null ? null : revision.getTakenBy().getDisplayName(),
                revision.getAuthorName(),
                revision.getCreatedAt(),
                false,
                between(previous, snapshot)));
            if (snapshot != null) previous = snapshot;
        }

        List<Change> sinceLastHandover = between(previous, revisions.current(quote));
        if (!sinceLastHandover.isEmpty()) {
            rounds.add(new Round(null, "Not yet sent", null, null, null, true, sinceLastHandover));
        }

        return rounds;
    }

    /**
     * What moved between two snapshots.
     *
     * <p>A null <em>before</em> means there is nothing to compare against, not that everything
     * was added: listing a whole trip as forty additions on the day it was sent is noise. A null
     * <em>after</em> means the snapshot could not be read, and an empty list is the honest answer
     * rather than a guess.
     */
    public List<Change> between(QuoteSnapshot before, QuoteSnapshot after) {
        List<Change> changes = new ArrayList<>();
        if (before == null || after == null) return changes;

        party(before, after, changes);
        vehicles(before, after, changes);
        days(before, after, changes);
        money(before, after, changes);
        return changes;
    }

    // ---------------------------------------------------------------- the party

    private void party(QuoteSnapshot before, QuoteSnapshot after, List<Change> changes) {
        Map<String, Integer> was = partyCounts(before);
        Map<String, Integer> now = partyCounts(after);

        for (String key : union(was.keySet(), now.keySet())) {
            Integer wasCount = was.get(key);
            Integer nowCount = now.get(key);
            if (Objects.equals(wasCount, nowCount)) continue;

            if (wasCount == null) {
                changes.add(new Change("Travellers", key + ": " + nowCount + " added"));
            } else if (nowCount == null) {
                changes.add(new Change("Travellers", key + ": " + wasCount + " removed"));
            } else {
                changes.add(new Change("Travellers",
                    key + ": " + wasCount + " became " + nowCount));
            }
        }
    }

    private Map<String, Integer> partyCounts(QuoteSnapshot snapshot) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (snapshot.party() == null) return counts;
        for (QuoteSnapshot.Party p : snapshot.party()) {
            String key = join(p.nationality(), p.ageGroup());
            if (key.isBlank()) continue;
            counts.merge(key, p.count() == null ? 0 : p.count(), Integer::sum);
        }
        return counts;
    }

    private void vehicles(QuoteSnapshot before, QuoteSnapshot after, List<Change> changes) {
        if (Objects.equals(before.vehicles(), after.vehicles())) return;
        if (before.vehicles() == null || after.vehicles() == null) return;
        changes.add(new Change("Vehicles",
            "Vehicles: " + before.vehicles() + " became " + after.vehicles()));
    }

    // ---------------------------------------------------------------- the days

    private void days(QuoteSnapshot before, QuoteSnapshot after, List<Change> changes) {
        Map<Integer, QuoteSnapshot.Day> was = byNumber(before);
        Map<Integer, QuoteSnapshot.Day> now = byNumber(after);

        for (Integer number : union(was.keySet(), now.keySet())) {
            QuoteSnapshot.Day a = was.get(number);
            QuoteSnapshot.Day b = now.get(number);
            String scope = "Day " + number;

            if (a == null) {
                changes.add(new Change(scope, scope + " was added" + titleSuffix(b)));
                continue;
            }
            if (b == null) {
                changes.add(new Change(scope, scope + " was removed" + titleSuffix(a)));
                continue;
            }

            stays(scope, a, b, changes);
            activities(scope, a, b, changes);
            parks(scope, a, b, changes);
            flights(scope, a, b, changes);
            route(scope, a, b, changes);
        }
    }

    private Map<Integer, QuoteSnapshot.Day> byNumber(QuoteSnapshot snapshot) {
        Map<Integer, QuoteSnapshot.Day> days = new LinkedHashMap<>();
        if (snapshot.days() == null) return days;
        for (QuoteSnapshot.Day d : snapshot.days()) {
            if (d.number() != null) days.put(d.number(), d);
        }
        return days;
    }

    private String titleSuffix(QuoteSnapshot.Day day) {
        return day == null || day.title() == null || day.title().isBlank()
            ? "" : " (" + day.title() + ")";
    }

    /**
     * Where somebody is sleeping, and on what terms.
     *
     * <p>The alternatives are left out of the comparison entirely. At editing level
     * CHOOSE_OFFERED they are the menu, so a client swapping to one of them produces a swap in
     * the booked row and a mirror-image swap in the menu, and reporting both says the same thing
     * twice in opposite directions. What the client booked is the change; what we offered is the
     * context.
     *
     * <p>A single lodge becoming a single other lodge is reported as one sentence rather than a
     * removal and an addition, because that is what happened and it is the commonest change there
     * is.
     */
    private void stays(String scope, QuoteSnapshot.Day a, QuoteSnapshot.Day b, List<Change> changes) {
        List<QuoteSnapshot.Stay> was = booked(a.stays());
        List<QuoteSnapshot.Stay> now = booked(b.stays());

        if (was.size() == 1 && now.size() == 1
                && !Objects.equals(was.get(0).accommodation(), now.get(0).accommodation())) {
            changes.add(new Change(scope, scope + ": " + name(was.get(0).accommodation())
                + " became " + name(now.get(0).accommodation())));
            return;
        }

        Map<String, QuoteSnapshot.Stay> wasByName = byName(was);
        Map<String, QuoteSnapshot.Stay> nowByName = byName(now);

        for (String lodge : union(wasByName.keySet(), nowByName.keySet())) {
            QuoteSnapshot.Stay x = wasByName.get(lodge);
            QuoteSnapshot.Stay y = nowByName.get(lodge);

            if (x == null) {
                changes.add(new Change(scope, scope + ": " + lodge + " added"));
            } else if (y == null) {
                changes.add(new Change(scope, scope + ": " + lodge + " removed"));
            } else {
                detail(scope, lodge, "room", x.roomType(), y.roomType(), changes);
                detail(scope, lodge, "room standard", x.roomStandard(), y.roomStandard(), changes);
                detail(scope, lodge, "board", x.boardType(), y.boardType(), changes);
                if (!Objects.equals(x.rooms(), y.rooms()) && x.rooms() != null && y.rooms() != null) {
                    changes.add(new Change(scope, scope + ": " + lodge + ", "
                        + rooms(x.rooms()) + " became " + rooms(y.rooms())));
                }
            }
        }
    }

    private List<QuoteSnapshot.Stay> booked(List<QuoteSnapshot.Stay> stays) {
        List<QuoteSnapshot.Stay> booked = new ArrayList<>();
        if (stays == null) return booked;
        for (QuoteSnapshot.Stay s : stays) {
            if (!s.alternative()) booked.add(s);
        }
        return booked;
    }

    private Map<String, QuoteSnapshot.Stay> byName(List<QuoteSnapshot.Stay> stays) {
        Map<String, QuoteSnapshot.Stay> byName = new LinkedHashMap<>();
        for (QuoteSnapshot.Stay s : stays) byName.put(name(s.accommodation()), s);
        return byName;
    }

    private void detail(String scope, String lodge, String what,
                        String was, String now, List<Change> changes) {
        if (Objects.equals(was, now) || was == null || now == null) return;
        changes.add(new Change(scope,
            scope + ": " + lodge + ", " + what + " " + was + " became " + now));
    }

    private void activities(String scope, QuoteSnapshot.Day a, QuoteSnapshot.Day b,
                            List<Change> changes) {
        Map<String, QuoteSnapshot.Activity> was = new LinkedHashMap<>();
        Map<String, QuoteSnapshot.Activity> now = new LinkedHashMap<>();
        if (a.activities() != null) for (var x : a.activities()) was.put(name(x.name()), x);
        if (b.activities() != null) for (var y : b.activities()) now.put(name(y.name()), y);

        for (String activity : union(was.keySet(), now.keySet())) {
            QuoteSnapshot.Activity x = was.get(activity);
            QuoteSnapshot.Activity y = now.get(activity);

            if (x == null) {
                changes.add(new Change(scope, scope + ": " + activity + " added"));
            } else if (y == null) {
                changes.add(new Change(scope, scope + ": " + activity + " removed"));
            } else if (x.included() != y.included()) {
                changes.add(new Change(scope, scope + ": " + activity
                    + (y.included() ? " is now included in the price"
                                    : " is no longer included in the price")));
            }
        }
    }

    private void parks(String scope, QuoteSnapshot.Day a, QuoteSnapshot.Day b,
                       List<Change> changes) {
        Map<String, QuoteSnapshot.Park> was = new LinkedHashMap<>();
        Map<String, QuoteSnapshot.Park> now = new LinkedHashMap<>();
        if (a.parks() != null) for (var x : a.parks()) was.put(name(x.name()), x);
        if (b.parks() != null) for (var y : b.parks()) now.put(name(y.name()), y);

        for (String park : union(was.keySet(), now.keySet())) {
            QuoteSnapshot.Park x = was.get(park);
            QuoteSnapshot.Park y = now.get(park);

            if (x == null) {
                changes.add(new Change(scope, scope + ": " + park + " added"));
            } else if (y == null) {
                changes.add(new Change(scope, scope + ": " + park + " removed"));
            } else {
                inside(scope, park, x.activities(), y.activities(), changes);
            }
        }
    }

    private void inside(String scope, String park, List<String> was, List<String> now,
                        List<Change> changes) {
        Set<String> a = was == null ? Set.of() : new LinkedHashSet<>(was);
        Set<String> b = now == null ? Set.of() : new LinkedHashSet<>(now);

        for (String added : b) {
            if (!a.contains(added)) {
                changes.add(new Change(scope, scope + ": " + name(added) + " added in " + park));
            }
        }
        for (String removed : a) {
            if (!b.contains(removed)) {
                changes.add(new Change(scope, scope + ": " + name(removed) + " removed from " + park));
            }
        }
    }

    private void flights(String scope, QuoteSnapshot.Day a, QuoteSnapshot.Day b,
                         List<Change> changes) {
        Map<String, QuoteSnapshot.Flight> was = new LinkedHashMap<>();
        Map<String, QuoteSnapshot.Flight> now = new LinkedHashMap<>();
        if (a.flights() != null) for (var x : a.flights()) was.put(sector(x), x);
        if (b.flights() != null) for (var y : b.flights()) now.put(sector(y), y);

        for (String sector : union(was.keySet(), now.keySet())) {
            QuoteSnapshot.Flight x = was.get(sector);
            QuoteSnapshot.Flight y = now.get(sector);

            if (x == null) {
                changes.add(new Change(scope, scope + ": flight " + sector + " added"));
            } else if (y == null) {
                changes.add(new Change(scope, scope + ": flight " + sector + " removed"));
            } else if (!Objects.equals(x.seats(), y.seats())
                    && x.seats() != null && y.seats() != null) {
                changes.add(new Change(scope, scope + ": flight " + sector + ", "
                    + x.seats() + " seats became " + y.seats()));
            }
        }
    }

    private String sector(QuoteSnapshot.Flight flight) {
        String route = join(flight.from(), flight.to()).replace(", ", " to ");
        if (route.isBlank()) route = name(flight.airline());
        return route;
    }

    /** Where the day ends, which is the only part of the route a snapshot can see move. */
    private void route(String scope, QuoteSnapshot.Day a, QuoteSnapshot.Day b,
                       List<Change> changes) {
        if (Objects.equals(a.endLocation(), b.endLocation())) return;
        if (a.endLocation() == null || b.endLocation() == null) return;
        if (a.endLocation().isBlank() || b.endLocation().isBlank()) return;
        changes.add(new Change(scope,
            scope + ": ends in " + b.endLocation() + " instead of " + a.endLocation()));
    }

    // ---------------------------------------------------------------- the money

    /**
     * The figures, last, because they are the consequence of everything above.
     *
     * <p>Only the grand total. A client reading that the subtotal, the discount and the total all
     * moved learns nothing they did not learn from the total, and three lines of arithmetic at the
     * bottom of a change list is how the one sentence about their lodge gets lost.
     */
    private void money(QuoteSnapshot before, QuoteSnapshot after, List<Change> changes) {
        Map<String, BigDecimal> was = totals(before);
        Map<String, BigDecimal> now = totals(after);

        for (String currency : union(was.keySet(), now.keySet())) {
            BigDecimal x = was.get(currency);
            BigDecimal y = now.get(currency);
            if (x == null || y == null) continue;
            if (x.compareTo(y) == 0) continue;
            changes.add(new Change("Price", "Total: " + currency + " " + amount(x)
                + " became " + currency + " " + amount(y)));
        }
    }

    private Map<String, BigDecimal> totals(QuoteSnapshot snapshot) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        if (snapshot.money() == null) return totals;
        for (QuoteSnapshot.Money m : snapshot.money()) {
            if (m.currency() != null && m.total() != null) totals.put(m.currency(), m.total());
        }
        return totals;
    }

    // ---------------------------------------------------------------- odds and ends

    private String amount(BigDecimal value) {
        return String.format(Locale.UK, "%,.2f", value);
    }

    private String rooms(Integer count) {
        return count + (count == 1 ? " room" : " rooms");
    }

    private String name(String value) {
        return value == null || value.isBlank() ? "Not named" : value;
    }

    private String join(String a, String b) {
        if (a == null || a.isBlank()) return b == null ? "" : b.trim();
        if (b == null || b.isBlank()) return a.trim();
        return a.trim() + ", " + b.trim();
    }

    /** Both sides, in the order they first appear, so a change list reads in trip order. */
    private <T> Set<T> union(Set<T> a, Set<T> b) {
        Set<T> all = new LinkedHashSet<>(a);
        all.addAll(b);
        return all;
    }
}
