package com.itineraryledger.kabengosafaris.Invoice.Services.PaymentServices;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.PaymentDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Payment;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving payments
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentGetService {

    private final PaymentRepository paymentRepository;
    private final IdObfuscator idObfuscator;
    private final PaymentCreateService paymentCreateService;

    /**
     * Get all payments for a specific invoice
     *
     * @param invoiceIdObfuscated The obfuscated invoice ID
     * @return ResponseEntity with ApiResponse containing list of PaymentDTOs
     */
    public ResponseEntity<ApiResponse<?>> getPaymentsByInvoice(String invoiceIdObfuscated) {
        log.info("Fetching payments for invoice: {}", invoiceIdObfuscated);

        try {
            Long invoiceId;
            try {
                invoiceId = idObfuscator.decodeId(invoiceIdObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", invoiceIdObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            List<Payment> payments = paymentRepository.findByInvoiceIdOrderByPaymentDateDesc(invoiceId);

            List<PaymentDTO> paymentDTOs = payments.stream()
                .map(paymentCreateService::convertToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payments retrieved successfully", paymentDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching payments for invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch payments", "PAYMENTS_FETCH_FAILED")
            );
        }
    }

    /**
     * Get a single payment by obfuscated ID
     *
     * @param idObfuscated The obfuscated payment ID
     * @return ResponseEntity with ApiResponse containing the PaymentDTO
     */
    public ResponseEntity<ApiResponse<?>> getPaymentById(String idObfuscated) {
        log.info("Fetching payment with ID: {}", idObfuscated);

        try {
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode payment ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid payment ID", "INVALID_PAYMENT_ID")
                );
            }

            Payment payment = paymentRepository.findById(id).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Payment not found", "PAYMENT_NOT_FOUND")
                );
            }

            PaymentDTO paymentDTO = paymentCreateService.convertToDTO(payment);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payment retrieved successfully", paymentDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch payment", "PAYMENT_FETCH_FAILED")
            );
        }
    }
}
