package com.itineraryledger.kabengosafaris.Safari.Specifications;

import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariPhase;
import com.itineraryledger.kabengosafaris.Safari.Enums.SafariState;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * SafariSpecification - JPA Specifications for Safari filtering
 */
public class SafariSpecification {

    public static Specification<Safari> nameLike(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Safari> codeLike(String code) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("code")), "%" + code.toLowerCase() + "%");
    }

    public static Specification<Safari> hasState(SafariState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Safari> startLocationLike(String startLocation) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("startLocation")), "%" + startLocation.toLowerCase() + "%");
    }

    public static Specification<Safari> endLocationLike(String endLocation) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("endLocation")), "%" + endLocation.toLowerCase() + "%");
    }

    public static Specification<Safari> startDateAfter(LocalDate date) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Safari> startDateBefore(LocalDate date) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startDate"), date);
    }

    public static Specification<Safari> isActive(Boolean isActive) {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Safari> searchKeyword(String keyword) {
        String lowerKeyword = "%" + keyword.toLowerCase() + "%";
        // Note: description is @Lob — LOWER() on CLOB types causes errors in Hibernate 6
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), lowerKeyword),
                cb.like(cb.lower(root.get("code")), lowerKeyword),
                cb.like(cb.lower(root.get("startLocation")), lowerKeyword),
                cb.like(cb.lower(root.get("endLocation")), lowerKeyword)
        );
    }

    public static Specification<Safari> hasCustomer(Long customerId) {
        return (root, query, cb) -> customerId == null
            ? cb.conjunction()
            : cb.equal(root.get("customer").get("id"), customerId);
    }

    public static Specification<Safari> hasItinerary(Long itineraryId) {
        return (root, query, cb) -> cb.equal(root.get("itinerary").get("id"), itineraryId);
    }

    // ========================
    // PHASE — where a safari is in time
    // ========================

    /**
     * The phase, as a database predicate rather than an afterthought.
     *
     * It used to be applied in memory to whichever page had already been
     * fetched: asking for IN_PROGRESS filtered ten rows out of page one and
     * reported that count as the total, so the filter answered a different
     * question on every page and the number under it was never right.
     *
     * Every phase is a statement about start date, end date and today, so every
     * one of them can be asked of the database. The boundaries mirror
     * {@code Safari.getCurrentPhase()} exactly — the entity remains the
     * definition, this is the same definition in SQL.
     */
    public static Specification<Safari> inPhase(SafariPhase phase, LocalDate today) {
        return (root, query, cb) -> {
            if (phase == null) return cb.conjunction();

            var start = root.<LocalDate>get("startDate");
            var end = root.<LocalDate>get("endDate");

            // running: start <= today <= end
            var running = cb.and(cb.lessThanOrEqualTo(start, today), cb.greaterThanOrEqualTo(end, today));
            // finished
            var ended = cb.lessThan(end, today);

            return switch (phase) {
                case FAR_FUTURE -> cb.greaterThan(start, today.plusDays(30));
                case UPCOMING -> cb.and(
                    cb.greaterThanOrEqualTo(start, today.plusDays(8)),
                    cb.lessThanOrEqualTo(start, today.plusDays(30)));
                case STARTING_SOON -> cb.and(
                    cb.greaterThanOrEqualTo(start, today.plusDays(3)),
                    cb.lessThanOrEqualTo(start, today.plusDays(7)));
                case IMMINENT -> cb.and(
                    cb.greaterThanOrEqualTo(start, today.plusDays(1)),
                    cb.lessThanOrEqualTo(start, today.plusDays(2)));
                /*
                 * Unreachable in the entity — it needs a safari that has not
                 * started yet and starts in under a day, which cannot both hold.
                 * Answering with nothing is honest; answering with "starts
                 * today" would disagree with the badge on the row, which reads
                 * DAY_ONE.
                 */
                case TODAY -> cb.disjunction();
                case DAY_ONE -> cb.and(running, cb.equal(start, today));
                case LAST_DAY -> cb.and(running, cb.notEqual(start, today), cb.equal(end, today));
                /*
                 * Day arithmetic, turned around so the database compares a
                 * column to a constant rather than calling a date function:
                 * "today is within two days of the start" is the same statement
                 * as "the start is within two days of today".
                 */
                // days two and three — not the first, not the last
                case EARLY_DAYS -> cb.and(
                    running,
                    cb.notEqual(start, today),
                    cb.notEqual(end, today),
                    cb.greaterThanOrEqualTo(start, today.minusDays(2)));
                // the day before the last one
                case FINAL_DAYS -> cb.and(
                    running,
                    cb.notEqual(start, today),
                    cb.equal(end, today.plusDays(1)));
                case MID_SAFARI -> cb.and(
                    running,
                    cb.lessThan(start, today.minusDays(2)),
                    cb.greaterThan(end, today.plusDays(1)));
                case JUST_ENDED -> cb.and(ended, cb.greaterThanOrEqualTo(end, today.minusDays(7)));
                case RECENTLY_ENDED -> cb.and(
                    cb.lessThan(end, today.minusDays(7)),
                    cb.greaterThanOrEqualTo(end, today.minusDays(30)));
                case PAST -> cb.lessThan(end, today.minusDays(30));
                default -> cb.conjunction();
            };
        };
    }

    /** The three groups an office actually plans around. */
    public static Specification<Safari> notStarted(LocalDate today) {
        return (root, query, cb) -> cb.greaterThan(root.<LocalDate>get("startDate"), today);
    }

    public static Specification<Safari> running(LocalDate today) {
        return (root, query, cb) -> cb.and(
            cb.lessThanOrEqualTo(root.<LocalDate>get("startDate"), today),
            cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), today));
    }

    public static Specification<Safari> finished(LocalDate today) {
        return (root, query, cb) -> cb.lessThan(root.<LocalDate>get("endDate"), today);
    }

    public static Specification<Safari> byStates(java.util.List<SafariState> states) {
        return (root, query, cb) -> states == null || states.isEmpty()
            ? cb.conjunction()
            : root.get("state").in(states);
    }

    public static Specification<Safari> createdAfter(java.time.LocalDateTime since) {
        return (root, query, cb) -> since == null
            ? cb.conjunction()
            : cb.greaterThanOrEqualTo(root.get("createdAt"), since);
    }
}
