package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerRepository;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.UpdateInvoiceShareDTO;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceShare;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceShareRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.PaymentRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Who is paying which part of an invoice that several people are splitting.
 *
 * <p>The invoice is left entirely alone: its total, its status and its own balance still count
 * every payment against it, share or no share. Shares sit underneath and answer a question the
 * invoice never could, which is who still owes what.
 *
 * <p>Shares are allowed not to add up to the invoice. Half a group is often settled before the
 * rest have been asked, and a booking can be part-allocated for weeks. The rule is that the gap
 * is always reported, never silently closed and never silently ignored.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class InvoiceShareService {

    private final InvoiceShareRepository shareRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final InvoicePaymentAggregationService aggregationService;
    private final IdObfuscator idObfuscator;

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> list(String obfuscatedInvoiceId) {
        Invoice invoice = findInvoice(obfuscatedInvoiceId);
        if (invoice == null) {
            return notFound("Invoice not found", "INVOICE_NOT_FOUND");
        }

        List<InvoiceShare> shares =
            shareRepository.findByInvoiceIdOrderByDisplayOrderAscIdAsc(invoice.getId());

        Map<String, Object> body = new HashMap<>();
        body.put("invoiceShares", shares.stream().map(this::toDTO).toList());
        body.put("totalItems", shares.size());
        body.put("allocated", toPriceList(aggregationService.computeAllocatedByCurrency(shares)));
        /*
         * What nobody has been made responsible for. Positive means part of the bill has no payer
         * yet; negative means the shares promise more than the invoice asks for. Both are shown,
         * because which share is wrong is a judgement only the person who set them up can make.
         */
        body.put("unallocated",
            toPriceList(aggregationService.computeUnallocatedByCurrency(invoice, shares)));
        body.put("fullyAllocated",
            aggregationService.computeUnallocatedByCurrency(invoice, shares).values().stream()
                .allMatch(remaining -> remaining.compareTo(BigDecimal.ZERO) == 0));

        return ResponseEntity.ok(
            ApiResponse.success(200, "Invoice shares retrieved successfully", body));
    }

    /**
     * Every share across every invoice, paged and searchable.
     *
     * <p>The house pattern: a child collection is reachable from its parent AND on its own, so a
     * picker that has to find one row among hundreds does not have to know which invoice it
     * belongs to first. The template editor's record picker is exactly that caller.
     */
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> listAll(
        String keyword, int page, int size, String sortBy, String sortDirection) {

        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        String safeSortBy = List.of("createdAt", "updatedAt", "amount", "payerName")
            .contains(sortBy) ? sortBy : "updatedAt";

        Specification<InvoiceShare> spec = (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + keyword.trim().toLowerCase() + "%";
            query.distinct(true);
            return cb.or(
                cb.like(cb.lower(root.get("payerName")), like),
                cb.like(cb.lower(cb.coalesce(root.get("payerEmail"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("shareReference"), "")), like),
                cb.like(cb.lower(cb.coalesce(root.get("invoice").get("invoiceCode"), "")), like));
        };

        Page<InvoiceShare> found = shareRepository.findAll(spec,
            PageRequest.of(Math.max(page, 0), safeSize, Sort.by(direction, safeSortBy)));

        Map<String, Object> body = new HashMap<>();
        body.put("invoiceShares", found.getContent().stream().map(this::toDTO).toList());
        body.put("currentPage", found.getNumber());
        body.put("totalItems", found.getTotalElements());
        body.put("totalPages", found.getTotalPages());
        body.put("pageSize", found.getSize());
        body.put("currentSortBy", safeSortBy);
        body.put("currentSortDirection", direction.name().toLowerCase());
        body.put("validSortFields", List.of("createdAt", "updatedAt", "amount", "payerName"));

        return ResponseEntity.ok(
            ApiResponse.success(200, "Invoice shares retrieved successfully", body));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<?>> getOne(String obfuscatedShareId) {
        InvoiceShare share = findShare(obfuscatedShareId);
        if (share == null) {
            return notFound("Invoice share not found", "INVOICE_SHARE_NOT_FOUND");
        }
        return ResponseEntity.ok(
            ApiResponse.success(200, "Invoice share retrieved successfully", toDTO(share)));
    }

    @AuditLogAnnotation(action = "CREATE_INVOICE_SHARE",
        description = "Making someone responsible for part of an invoice", entityType = "Invoice")
    public ResponseEntity<ApiResponse<?>> create(String obfuscatedInvoiceId,
                                                 CreateInvoiceShareDTO dto) {
        Invoice invoice = findInvoice(obfuscatedInvoiceId);
        if (invoice == null) {
            return notFound("Invoice not found", "INVOICE_NOT_FOUND");
        }

        String currency = resolveCurrency(dto.getCurrency(), invoice);
        if (currency == null) {
            return badRequest(
                "This invoice has no total yet, so there is nothing to divide. Give the share a "
                + "currency explicitly, or calculate the invoice first.",
                "SHARE_CURRENCY_UNKNOWN");
        }
        if (!invoiceHasCurrency(invoice, currency)) {
            return badRequest(
                "This invoice is not billed in " + currency
                + ", so a share in that currency would never settle against it.",
                "SHARE_CURRENCY_MISMATCH");
        }

        Customer customer = null;
        if (dto.getCustomerId() != null && !dto.getCustomerId().isBlank()) {
            customer = findCustomer(dto.getCustomerId());
            if (customer == null) {
                return badRequest("No such customer: " + dto.getCustomerId(), "CUSTOMER_NOT_FOUND");
            }
        }

        InvoiceShare share = InvoiceShare.builder()
            .invoice(invoice)
            .payerName(dto.getPayerName().trim())
            .payerEmail(blankToNull(dto.getPayerEmail()))
            .customer(customer)
            .amount(dto.getAmount())
            .currency(currency)
            .shareReference(blankToNull(dto.getShareReference()))
            .notes(blankToNull(dto.getNotes()))
            .displayOrder(dto.getDisplayOrder() != null
                ? dto.getDisplayOrder()
                : (int) shareRepository.countByInvoiceId(invoice.getId()) + 1)
            .isActive(true)
            .build();

        share = shareRepository.save(share);
        log.info("Created invoice share {} on invoice {} for {}",
            share.getId(), invoice.getId(), share.getPayerName());

        return ResponseEntity.status(HttpStatus.CREATED).body(
            ApiResponse.success(201, "Invoice share created successfully", toDTO(share)));
    }

    @AuditLogAnnotation(action = "UPDATE_INVOICE_SHARE",
        description = "Changing who pays what on an invoice", entityType = "Invoice")
    public ResponseEntity<ApiResponse<?>> update(String obfuscatedShareId,
                                                 UpdateInvoiceShareDTO dto) {
        InvoiceShare share = findShare(obfuscatedShareId);
        if (share == null) {
            return notFound("Invoice share not found", "INVOICE_SHARE_NOT_FOUND");
        }

        if (dto.getCurrency() != null && !dto.getCurrency().equalsIgnoreCase(share.getCurrency())) {
            String currency = dto.getCurrency().toUpperCase();
            if (!invoiceHasCurrency(share.getInvoice(), currency)) {
                return badRequest(
                    "This invoice is not billed in " + currency + ".", "SHARE_CURRENCY_MISMATCH");
            }
            /*
             * Re-denominating a share that has already taken money would leave payments recorded
             * in one currency sitting under a share demanding another, and the balance would read
             * as though nothing had been paid.
             */
            if (paymentRepository.countByShareId(share.getId()) > 0) {
                return badRequest(
                    "Payments have already been recorded against this share, so its currency "
                    + "cannot change. Record a correction against the invoice instead.",
                    "SHARE_HAS_PAYMENTS");
            }
            share.setCurrency(currency);
        }

        if (dto.getCustomerId() != null) {
            if (dto.getCustomerId().isBlank()) {
                share.setCustomer(null);
            } else {
                Customer customer = findCustomer(dto.getCustomerId());
                if (customer == null) {
                    return badRequest("No such customer: " + dto.getCustomerId(),
                        "CUSTOMER_NOT_FOUND");
                }
                share.setCustomer(customer);
            }
        }

        if (dto.getPayerName() != null && !dto.getPayerName().isBlank()) {
            share.setPayerName(dto.getPayerName().trim());
        }
        if (dto.getPayerEmail() != null) {
            share.setPayerEmail(blankToNull(dto.getPayerEmail()));
        }
        if (dto.getAmount() != null) {
            share.setAmount(dto.getAmount());
        }
        if (dto.getShareReference() != null) {
            share.setShareReference(blankToNull(dto.getShareReference()));
        }
        if (dto.getNotes() != null) {
            share.setNotes(blankToNull(dto.getNotes()));
        }
        if (dto.getDisplayOrder() != null) {
            share.setDisplayOrder(dto.getDisplayOrder());
        }
        if (dto.getIsActive() != null) {
            share.setIsActive(dto.getIsActive());
        }

        share = shareRepository.save(share);
        return ResponseEntity.ok(
            ApiResponse.success(200, "Invoice share updated successfully", toDTO(share)));
    }

    /**
     * Remove shares, reporting per id what happened.
     *
     * <p>A share that has taken money is refused rather than deleted. The payment would survive
     * it, because the foreign key is {@code on delete set null} and the record of money actually
     * received must never disappear, but the trail of who sent it would not, and that trail is
     * the only reason this feature exists.
     */
    @AuditLogAnnotation(action = "DELETE_INVOICE_SHARE",
        description = "Removing a payer from an invoice", entityType = "Invoice")
    public ResponseEntity<ApiResponse<?>> delete(List<String> obfuscatedShareIds) {
        List<String> deletedIds = new ArrayList<>();
        List<Map<String, String>> skipped = new ArrayList<>();

        for (String obfuscatedId : obfuscatedShareIds == null ? List.<String>of() : obfuscatedShareIds) {
            InvoiceShare share = findShare(obfuscatedId);
            if (share == null) {
                skipped.add(Map.of("id", String.valueOf(obfuscatedId),
                    "reason", "No such share"));
                continue;
            }
            long payments = paymentRepository.countByShareId(share.getId());
            if (payments > 0) {
                skipped.add(Map.of(
                    "id", obfuscatedId,
                    "code", share.getPayerName(),
                    "reason", payments + " payment(s) are recorded against this share. Deactivate "
                        + "it instead, so the record of who paid survives."));
                continue;
            }
            shareRepository.delete(share);
            deletedIds.add(obfuscatedId);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("deletedCount", deletedIds.size());
        report.put("deletedIds", deletedIds);
        report.put("skipped", skipped);

        return ResponseEntity.ok(ApiResponse.success(200,
            deletedIds.size() + " share(s) deleted, " + skipped.size() + " skipped", report));
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private InvoiceShareDTO toDTO(InvoiceShare share) {
        Invoice invoice = share.getInvoice();
        Customer customer = share.getCustomer();
        return InvoiceShareDTO.builder()
            .id(idObfuscator.encodeId(share.getId()))
            .invoiceId(invoice != null ? idObfuscator.encodeId(invoice.getId()) : null)
            .invoiceCode(invoice != null ? invoice.getInvoiceCode() : null)
            .payerName(share.resolvedPayerName())
            .payerEmail(share.resolvedPayerEmail())
            .customerId(customer != null ? idObfuscator.encodeId(customer.getId()) : null)
            .customerName(customer != null ? customer.getDisplayName() : null)
            .amount(share.getAmount())
            .currency(share.getCurrency())
            .shareReference(share.getShareReference())
            .notes(share.getNotes())
            .displayOrder(share.getDisplayOrder())
            .isActive(share.getIsActive())
            .amountPaid(aggregationService.computeSharePaid(share))
            .balance(aggregationService.computeShareBalance(share))
            .status(aggregationService.computeShareStatus(share))
            .paymentCount((int) paymentRepository.countByShareId(share.getId()))
            .createdAt(share.getCreatedAt())
            .updatedAt(share.getUpdatedAt())
            .build();
    }

    private List<Price> toPriceList(Map<String, BigDecimal> byCurrency) {
        List<Price> prices = new ArrayList<>();
        byCurrency.forEach((currency, amount) -> prices.add(Price.builder()
            .currency(currency)
            .quantity(1)
            .unitPrice(amount)
            .totalPrice(amount)
            .build()));
        return prices;
    }

    private String resolveCurrency(String requested, Invoice invoice) {
        if (requested != null && !requested.isBlank()) {
            return requested.toUpperCase();
        }
        if (invoice.getGrandTotals() == null || invoice.getGrandTotals().isEmpty()) {
            return null;
        }
        return invoice.getGrandTotals().get(0).getCurrency();
    }

    private boolean invoiceHasCurrency(Invoice invoice, String currency) {
        if (invoice == null || invoice.getGrandTotals() == null
            || invoice.getGrandTotals().isEmpty()) {
            return true; // nothing to contradict yet
        }
        return invoice.getGrandTotals().stream()
            .anyMatch(total -> currency.equalsIgnoreCase(total.getCurrency()));
    }

    private Invoice findInvoice(String obfuscatedId) {
        try {
            return invoiceRepository.findById(idObfuscator.decodeId(obfuscatedId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private InvoiceShare findShare(String obfuscatedId) {
        try {
            return shareRepository.findById(idObfuscator.decodeId(obfuscatedId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private Customer findCustomer(String obfuscatedId) {
        try {
            return customerRepository.findById(idObfuscator.decodeId(obfuscatedId)).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseEntity<ApiResponse<?>> notFound(String message, String code) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, message, code));
    }

    private ResponseEntity<ApiResponse<?>> badRequest(String message, String code) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message, code));
    }
}
