package com.itineraryledger.kabengosafaris.CreditNote.Repository;

import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNote;
import com.itineraryledger.kabengosafaris.CreditNote.Enums.CreditNoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditNoteRepository extends JpaRepository<CreditNote, Long>, JpaSpecificationExecutor<CreditNote> {

    Optional<CreditNote> findByCreditNoteCode(String creditNoteCode);

    Optional<CreditNote> findByCreditNoteCodeIgnoreCase(String creditNoteCode);

    boolean existsByCreditNoteCode(String creditNoteCode);

    long countByStatus(CreditNoteStatus status);

    long countByIsActiveTrue();

    long countByInvoiceId(Long invoiceId);

    long countByCustomerId(Long customerId);

    List<CreditNote> findByInvoiceId(Long invoiceId);

    List<CreditNote> findByCustomerId(Long customerId);

    List<CreditNote> findByStatus(CreditNoteStatus status);

    @Query("SELECT e.id FROM CreditNote e WHERE e.id > :currentId ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findNextId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM CreditNote e WHERE e.id < :currentId ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findPreviousId(@Param("currentId") Long currentId);

    @Query("SELECT e.id FROM CreditNote e ORDER BY e.id ASC LIMIT 1")
    Optional<Long> findFirstId();

    @Query("SELECT e.id FROM CreditNote e ORDER BY e.id DESC LIMIT 1")
    Optional<Long> findLastId();
}
