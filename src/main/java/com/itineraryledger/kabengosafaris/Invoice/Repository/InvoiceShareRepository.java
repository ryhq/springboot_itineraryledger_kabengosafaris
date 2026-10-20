package com.itineraryledger.kabengosafaris.Invoice.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceShare;

/**
 * The shares of one invoice, in the order somebody arranged them.
 *
 * <p>Ordered by {@code displayOrder} then {@code id} so a list of payers reads the same way every
 * time it is opened; a chase list that reshuffles itself is a chase list nobody trusts.
 */
@Repository
public interface InvoiceShareRepository
    extends JpaRepository<InvoiceShare, Long>,
            org.springframework.data.jpa.repository.JpaSpecificationExecutor<InvoiceShare> {

    List<InvoiceShare> findByInvoiceIdOrderByDisplayOrderAscIdAsc(Long invoiceId);

    List<InvoiceShare> findByInvoiceIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    boolean existsByInvoiceId(Long invoiceId);
}
