package com.itineraryledger.kabengosafaris.Quote.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Quote.Entity.QuoteProposalComment;

@Repository
public interface QuoteProposalCommentRepository extends JpaRepository<QuoteProposalComment, Long> {

    /** Everything on this quotation, oldest first. The office's view. */
    List<QuoteProposalComment> findByQuoteIdOrderByIdAsc(Long quoteId);

    /**
     * What the client may read.
     *
     * <p>Filtered in the query rather than in the caller, deliberately. A caller that forgets
     * is a caller that publishes our internal discussion of somebody's budget to them, and the
     * two call sites look identical at a glance; one of them being physically unable to return
     * a hidden row is worth more than a comment asking people to be careful.
     */
    @Query("select c from QuoteProposalComment c "
        + "where c.quote.id = :quoteId and c.visibleToClient = true order by c.id asc")
    List<QuoteProposalComment> findVisibleToClient(@Param("quoteId") Long quoteId);

    long countByQuoteIdAndResolvedAtIsNull(Long quoteId);
}
