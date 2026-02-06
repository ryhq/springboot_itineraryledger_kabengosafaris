package com.itineraryledger.kabengosafaris.Invoice.Services;

import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceStateTransitionDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceStatus;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices.InvoiceCreateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Invoice State Transition Service - Manages invoice workflow state transitions
 *
 * Implements the simplified 8-state Invoice workflow:
 *
 * Core Journey (6 states):
 *   DRAFT → SENT → VIEWED → PARTIALLY_PAID → PAID
 *                                ↓
 *                            OVERDUE
 *
 * Exception States (2 states):
 *   CANCELLED, REFUNDED
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceStateTransitionService {

    private final InvoiceRepository invoiceRepository;
    private final IdObfuscator idObfuscator;
    private final InvoiceCreateService invoiceCreateService; // For DTO conversion

    // ========================
    // CORE JOURNEY - SENDING PHASE
    // ========================

    /**
     * Send invoice to customer (DRAFT → SENT)
     */
    public ResponseEntity<ApiResponse<?>> sendInvoice(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Sending invoice: {}", idObfuscated);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Validate current state
            if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot send invoice in state %s. Invoice must be in DRAFT state.",
                            invoice.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Update state and sent date
            invoice.setStatus(InvoiceStatus.SENT);
            invoice.setSentDate(LocalDate.now());

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} sent successfully", invoice.getInvoiceCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice sent successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error sending invoice", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to send invoice", "SEND_INVOICE_FAILED")
            );
        }
    }

    /**
     * Mark invoice as viewed by customer (SENT → VIEWED)
     */
    public ResponseEntity<ApiResponse<?>> markAsViewed(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Marking invoice as viewed: {}", idObfuscated);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Validate current state
            if (invoice.getStatus() != InvoiceStatus.SENT) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot mark as viewed from state %s. Invoice must be SENT.",
                            invoice.getStatus().getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Update state
            invoice.setStatus(InvoiceStatus.VIEWED);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} marked as viewed", invoice.getInvoiceCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice marked as viewed", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error marking invoice as viewed", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to mark invoice as viewed", "MARK_VIEWED_FAILED")
            );
        }
    }

    // ========================
    // CORE JOURNEY - PAYMENT PHASE
    // ========================

    /**
     * Record invoice payment (SENT/VIEWED/OVERDUE → PARTIALLY_PAID or PAID)
     */
    public ResponseEntity<ApiResponse<?>> recordPayment(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Recording payment for invoice: {}", idObfuscated);

        try {
            if (dto == null || dto.getIsFullPayment() == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Payment details are required (isFullPayment field)", "PAYMENT_DETAILS_REQUIRED")
                );
            }

            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Validate current state - can record payment from SENT, VIEWED, PARTIALLY_PAID, or OVERDUE
            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus != InvoiceStatus.SENT &&
                currentStatus != InvoiceStatus.VIEWED &&
                currentStatus != InvoiceStatus.PARTIALLY_PAID &&
                currentStatus != InvoiceStatus.OVERDUE) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot record payment from state %s. Invoice must be SENT, VIEWED, PARTIALLY_PAID, or OVERDUE.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Update state based on payment type
            if (dto.getIsFullPayment()) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoice.setPaidDate(LocalDate.now());
                log.info("Invoice {} marked as PAID", invoice.getInvoiceCode());
            } else {
                invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
                log.info("Invoice {} marked as PARTIALLY_PAID", invoice.getInvoiceCode());
            }

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Payment recorded successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error recording payment", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to record payment", "RECORD_PAYMENT_FAILED")
            );
        }
    }

    /**
     * Mark invoice as overdue (any unpaid state → OVERDUE)
     */
    public ResponseEntity<ApiResponse<?>> markOverdue(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Marking invoice as overdue: {}", idObfuscated);

        try {
            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Can mark as overdue from SENT, VIEWED, or PARTIALLY_PAID
            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus != InvoiceStatus.SENT &&
                currentStatus != InvoiceStatus.VIEWED &&
                currentStatus != InvoiceStatus.PARTIALLY_PAID) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot mark as overdue from state %s. Invoice must be SENT, VIEWED, or PARTIALLY_PAID.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Verify due date has passed
            if (!invoice.getDueDate().isBefore(LocalDate.now())) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        "Cannot mark as overdue - due date has not passed yet",
                        "NOT_YET_OVERDUE")
                );
            }

            // Update state
            invoice.setStatus(InvoiceStatus.OVERDUE);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} marked as OVERDUE", invoice.getInvoiceCode());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice marked as overdue", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error marking invoice as overdue", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to mark invoice as overdue", "MARK_OVERDUE_FAILED")
            );
        }
    }

    // ========================
    // EXCEPTION STATES - CANCELLATION
    // ========================

    /**
     * Cancel invoice (multiple states → CANCELLED)
     */
    public ResponseEntity<ApiResponse<?>> cancelInvoice(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Cancelling invoice: {}", idObfuscated);

        try {
            if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Cancellation reason is required", "REASON_REQUIRED")
                );
            }

            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Cannot cancel PAID, REFUNDED, or already CANCELLED invoices
            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus == InvoiceStatus.PAID ||
                currentStatus == InvoiceStatus.REFUNDED ||
                currentStatus == InvoiceStatus.CANCELLED) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot cancel invoice in state %s. Use refund workflow for paid invoices.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Update state
            invoice.setStatus(InvoiceStatus.CANCELLED);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Invoice {} cancelled: {}", invoice.getInvoiceCode(), dto.getReason());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Invoice cancelled successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error cancelling invoice", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to cancel invoice", "CANCEL_INVOICE_FAILED")
            );
        }
    }

    // ========================
    // EXCEPTION STATES - REFUND
    // ========================

    /**
     * Initiate refund (PAID/PARTIALLY_PAID → REFUNDED)
     */
    public ResponseEntity<ApiResponse<?>> initiateRefund(String idObfuscated, InvoiceStateTransitionDTO dto) {
        log.info("Initiating refund for invoice: {}", idObfuscated);

        try {
            if (dto == null || dto.getReason() == null || dto.getReason().isBlank()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Refund reason is required", "REASON_REQUIRED")
                );
            }

            Invoice invoice = findInvoice(idObfuscated);
            if (invoice == null) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Can only refund PAID or PARTIALLY_PAID invoices
            InvoiceStatus currentStatus = invoice.getStatus();
            if (currentStatus != InvoiceStatus.PAID && currentStatus != InvoiceStatus.PARTIALLY_PAID) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400,
                        String.format("Cannot initiate refund from state %s. Only PAID or PARTIALLY_PAID invoices can be refunded.",
                            currentStatus.getDisplayName()),
                        "INVALID_STATE_TRANSITION")
                );
            }

            // Update state
            invoice.setStatus(InvoiceStatus.REFUNDED);

            invoice = invoiceRepository.save(invoice);
            InvoiceDTO invoiceDTO = invoiceCreateService.convertToDTO(invoice);

            log.info("Refund initiated for invoice {}: {}", invoice.getInvoiceCode(), dto.getReason());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Refund initiated successfully", invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error initiating refund", e);
            return ResponseEntity.internalServerError().body(
                ApiResponse.error(500, "Failed to initiate refund", "INITIATE_REFUND_FAILED")
            );
        }
    }

    // ========================
    // HELPER METHODS
    // ========================

    /**
     * Find invoice by obfuscated ID
     */
    private Invoice findInvoice(String idObfuscated) {
        try {
            Long id = idObfuscator.decodeId(idObfuscated);
            return invoiceRepository.findById(id).orElse(null);
        } catch (Exception e) {
            log.warn("Failed to decode invoice ID: {}", idObfuscated, e);
            return null;
        }
    }
}
