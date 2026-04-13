package com.itineraryledger.kabengosafaris.CreditNote.Repository;

import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreditNoteLineItemRepository extends JpaRepository<CreditNoteLineItem, Long>, JpaSpecificationExecutor<CreditNoteLineItem> {

    List<CreditNoteLineItem> findByCreditNoteIdOrderByDisplayOrderAsc(Long creditNoteId);

    long countByCreditNoteId(Long creditNoteId);

    void deleteByCreditNoteId(Long creditNoteId);
}
