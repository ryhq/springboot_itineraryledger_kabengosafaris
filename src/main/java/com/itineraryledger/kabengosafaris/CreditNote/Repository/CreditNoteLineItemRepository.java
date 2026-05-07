package com.itineraryledger.kabengosafaris.CreditNote.Repository;

import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditNoteLineItemRepository extends JpaRepository<CreditNoteLineItem, Long>, JpaSpecificationExecutor<CreditNoteLineItem> {

    List<CreditNoteLineItem> findByCreditNoteIdOrderByDisplayOrderAsc(Long creditNoteId);

    long countByCreditNoteId(Long creditNoteId);

    void deleteByCreditNoteId(Long creditNoteId);

    /**
     * Returns the subset of given InvoiceLineItem ids that are still referenced
     * by any CreditNoteLineItem. Used to pre-check before a hard delete since
     * the FK has no ON DELETE rule and MySQL would otherwise raise a generic
     * constraint-violation 500.
     */
    @Query("SELECT DISTINCT c.invoiceLineItem.id FROM CreditNoteLineItem c " +
           "WHERE c.invoiceLineItem.id IN :ids")
    List<Long> findReferencedInvoiceLineItemIds(@Param("ids") List<Long> ids);
}
