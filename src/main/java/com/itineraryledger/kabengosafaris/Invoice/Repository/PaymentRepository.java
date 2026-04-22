package com.itineraryledger.kabengosafaris.Invoice.Repository;

import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByInvoiceIdOrderByPaymentDateDesc(Long invoiceId);

    long countByInvoiceId(Long invoiceId);

    List<Payment> findByPaymentDateBetween(LocalDate from, LocalDate to);

    long countByPaymentDateBetween(LocalDate from, LocalDate to);

    List<Payment> findTop20ByOrderByPaymentDateDescIdDesc();

    /**
     * @deprecated Use {@link #sumBaseAmountByInvoiceIdAndInvoiceCurrency} instead.
     * This query only sees payments whose raw currency matches, missing cross-currency payments.
     */
    @Deprecated
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId AND p.currency = :currency")
    BigDecimal sumAmountByInvoiceIdAndCurrency(@Param("invoiceId") Long invoiceId, @Param("currency") String currency);

    /**
     * Sum the base (invoice-currency-equivalent) amounts of all payments that settle a given
     * invoice currency. This correctly accounts for cross-currency payments via their stored
     * exchangeRate: baseAmount = amount × exchangeRate.
     *
     * Falls back to raw amount for legacy rows where baseAmount is still null.
     */
    @Query("SELECT COALESCE(SUM(COALESCE(p.baseAmount, p.amount)), 0) " +
           "FROM Payment p " +
           "WHERE p.invoice.id = :invoiceId " +
           "  AND COALESCE(p.invoiceCurrency, p.currency) = :invoiceCurrency")
    BigDecimal sumBaseAmountByInvoiceIdAndInvoiceCurrency(
        @Param("invoiceId") Long invoiceId,
        @Param("invoiceCurrency") String invoiceCurrency);
}
