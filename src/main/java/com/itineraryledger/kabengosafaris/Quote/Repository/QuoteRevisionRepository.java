package com.itineraryledger.kabengosafaris.Quote.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteRevision;

@Repository
public interface QuoteRevisionRepository extends JpaRepository<QuoteRevision, Long> {

    /** Oldest first, which is the order both the portal and the panel read them in. */
    List<QuoteRevision> findByQuoteIdOrderByRevisionNumberAsc(Long quoteId);

    Optional<QuoteRevision> findByQuoteIdAndRevisionNumber(Long quoteId, Integer revisionNumber);

    /**
     * The highest number used so far, or null when this quotation has none.
     *
     * <p>Max rather than count: a deleted revision would make count hand back a number already
     * taken, and the unique key would then refuse the insert.
     */
    @Query("select max(r.revisionNumber) from QuoteRevision r where r.quote.id = :quoteId")
    Integer highestNumberFor(@Param("quoteId") Long quoteId);

    /**
     * The most recent round.
     *
     * <p>A derived query rather than HQL with a {@code limit}, which is parsed at context start
     * and so would fail on the droplet rather than in this build.
     */
    Optional<QuoteRevision> findFirstByQuoteIdOrderByRevisionNumberDesc(Long quoteId);

    boolean existsByQuoteId(Long quoteId);
}
