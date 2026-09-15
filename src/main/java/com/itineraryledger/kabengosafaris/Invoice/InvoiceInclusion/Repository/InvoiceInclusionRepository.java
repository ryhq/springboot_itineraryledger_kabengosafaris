package com.itineraryledger.kabengosafaris.Invoice.InvoiceInclusion.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.Invoice.InvoiceInclusion.Entity.InvoiceInclusion;

@Repository
public interface InvoiceInclusionRepository extends JpaRepository<InvoiceInclusion, Long> {

    List<InvoiceInclusion> findByInvoiceIdOrderBySortOrderAscIdAsc(Long invoiceId);

    void deleteByInvoiceId(Long invoiceId);

    boolean existsByInvoiceId(Long invoiceId);
}
