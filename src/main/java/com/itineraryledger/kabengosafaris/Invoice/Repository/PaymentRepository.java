package com.itineraryledger.kabengosafaris.Invoice.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByInvoiceIdOrderByPaymentDateDesc(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId AND p.currency = :currency")
    BigDecimal sumAmountByInvoiceIdAndCurrency(@Param("invoiceId") Long invoiceId, @Param("currency") String currency);
}
