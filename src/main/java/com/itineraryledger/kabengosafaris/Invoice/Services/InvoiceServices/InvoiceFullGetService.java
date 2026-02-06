package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.BankAccount.Entity.BankAccount;
import com.itineraryledger.kabengosafaris.BankAccount.Repository.BankAccountRepository;
import com.itineraryledger.kabengosafaris.Customer.Entity.Customer;
import com.itineraryledger.kabengosafaris.Customer.Enums.CustomerType;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.FullInvoiceDTO.*;
import com.itineraryledger.kabengosafaris.Invoice.Entity.Invoice;
import com.itineraryledger.kabengosafaris.Invoice.Entity.InvoiceLineItem;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceLineItemRepository;
import com.itineraryledger.kabengosafaris.Invoice.Repository.InvoiceRepository;
import com.itineraryledger.kabengosafaris.Quote.Embeddables.Price;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * InvoiceFullGetService - Service for retrieving complete invoice with all nested data
 *
 * Returns the full invoice structure including:
 * - Invoice base data
 * - Customer information (nullable)
 * - Safari summary (nullable)
 * - All line items ordered by displayOrder
 * - All totals by currency
 * - Active bank accounts matching invoice currencies
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class InvoiceFullGetService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineItemRepository invoiceLineItemRepository;
    private final BankAccountRepository bankAccountRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public InvoiceFullGetService(
        InvoiceRepository invoiceRepository,
        InvoiceLineItemRepository invoiceLineItemRepository,
        BankAccountRepository bankAccountRepository,
        IdObfuscator idObfuscator
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceLineItemRepository = invoiceLineItemRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Get complete invoice with all nested data by obfuscated ID
     *
     * @param idObfuscated The obfuscated invoice ID
     * @return ResponseEntity with ApiResponse containing the full invoice
     */
    public ResponseEntity<ApiResponse<?>> getFullInvoice(String idObfuscated) {
        log.info("Fetching full invoice with ID: {}", idObfuscated);

        try {
            // Decode ID
            Long id;
            try {
                id = idObfuscator.decodeId(idObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode invoice ID: {}", idObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid invoice ID", "INVALID_INVOICE_ID")
                );
            }

            // Find invoice
            Invoice invoice = invoiceRepository.findById(id).orElse(null);
            if (invoice == null) {
                return ResponseEntity.status(404).body(
                    ApiResponse.error(404, "Invoice not found", "INVOICE_NOT_FOUND")
                );
            }

            // Build full DTO
            FullInvoiceDTO fullDTO = buildFullInvoiceDTO(invoice);

            log.info("Full invoice retrieved successfully: {} with {} line items",
                invoice.getInvoiceCode(),
                fullDTO.getTotalLineItemsCount());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Full invoice retrieved successfully", fullDTO)
            );

        } catch (Exception e) {
            log.error("Error fetching full invoice", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch full invoice", "FULL_INVOICE_FETCH_FAILED")
            );
        }
    }

    /**
     * Build the complete FullInvoiceDTO with all nested data
     */
    private FullInvoiceDTO buildFullInvoiceDTO(Invoice invoice) {
        FullInvoiceDTO dto = new FullInvoiceDTO();

        // ========================
        // INVOICE BASE FIELDS
        // ========================
        dto.setId(idObfuscator.encodeId(invoice.getId()));
        dto.setInvoiceCode(invoice.getInvoiceCode());
        dto.setTitle(invoice.getTitle());
        dto.setDescription(invoice.getDescription());
        dto.setStatus(invoice.getStatus());
        dto.setStatusDisplayName(invoice.getStatus() != null ? invoice.getStatus().getDisplayName() : null);

        // ========================
        // PRICING DETAILS
        // ========================
        dto.setTaxPercentage(invoice.getTaxPercentage());
        dto.setDiscountPercentage(invoice.getDiscountPercentage());
        dto.setDiscountReason(invoice.getDiscountReason());

        // ========================
        // DATES
        // ========================
        dto.setIssueDate(invoice.getIssueDate());
        dto.setDueDate(invoice.getDueDate());
        dto.setSentDate(invoice.getSentDate());
        dto.setPaidDate(invoice.getPaidDate());
        dto.setIsOverdue(invoice.isOverdue());

        // ========================
        // NOTES
        // ========================
        dto.setCustomerNotes(invoice.getCustomerNotes());
        dto.setInternalNotes(invoice.getInternalNotes());
        dto.setPaymentTerms(invoice.getPaymentTerms());

        // ========================
        // AUDIT
        // ========================
        dto.setIsActive(invoice.getIsActive());
        if (invoice.getCreatedBy() != null) {
            dto.setCreatedByName(invoice.getCreatedBy().getUsername());
        }
        if (invoice.getUpdatedBy() != null) {
            dto.setUpdatedByName(invoice.getUpdatedBy().getUsername());
        }
        dto.setCreatedAt(invoice.getCreatedAt());
        dto.setUpdatedAt(invoice.getUpdatedAt());

        // ========================
        // CUSTOMER INFORMATION (nullable)
        // ========================
        if (invoice.getCustomer() != null) {
            String customerName = getCustomerDisplayName(invoice.getCustomer());
            CustomerDTO customerDTO = CustomerDTO.builder()
                .id(idObfuscator.encodeId(invoice.getCustomer().getId()))
                .customerCode(invoice.getCustomer().getCode())
                .customerName(customerName)
                .email(invoice.getCustomer().getPrimaryEmail())
                .phone(invoice.getCustomer().getPrimaryPhone())
                .nationality(invoice.getCustomer().getNationality())
                .address(invoice.getCustomer().getAddress())
                .city(invoice.getCustomer().getCity())
                .country(invoice.getCustomer().getCountry())
                .build();
            dto.setCustomer(customerDTO);
        }

        // ========================
        // SAFARI SUMMARY (nullable)
        // ========================
        if (invoice.getSafari() != null) {
            SafariDTO safariDTO = SafariDTO.builder()
                .id(idObfuscator.encodeId(invoice.getSafari().getId()))
                .name(invoice.getSafari().getName())
                .code(invoice.getSafari().getCode())
                .state(invoice.getSafari().getState() != null ? invoice.getSafari().getState().name() : null)
                .stateDisplayName(invoice.getSafari().getState() != null ? invoice.getSafari().getState().getDisplayName() : null)
                .totalDays(invoice.getSafari().getTotalDays())
                .totalNights(invoice.getSafari().getTotalNights())
                .startDate(invoice.getSafari().getStartDate())
                .endDate(invoice.getSafari().getEndDate())
                .description(invoice.getSafari().getDescription())
                .startLocation(invoice.getSafari().getStartLocation())
                .endLocation(invoice.getSafari().getEndLocation())
                .build();
            dto.setSafari(safariDTO);
        }

        // ========================
        // LINE ITEMS
        // ========================
        List<InvoiceLineItem> lineItems = invoiceLineItemRepository.findByInvoiceIdOrderByDisplayOrderAsc(invoice.getId());
        List<LineItemDTO> lineItemDTOs = lineItems.stream()
            .map(this::convertLineItemToDTO)
            .collect(Collectors.toList());
        dto.setLineItems(lineItemDTOs);

        // ========================
        // TOTALS (SUBTOTALS, TAXES, DISCOUNTS, GRAND TOTALS, AMOUNTS PAID, BALANCES)
        // ========================
        if (invoice.getSubtotals() != null) {
            List<PriceDTO> subtotalDTOs = invoice.getSubtotals().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setSubtotals(subtotalDTOs);
        }

        if (invoice.getTaxes() != null) {
            List<PriceDTO> taxDTOs = invoice.getTaxes().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setTaxes(taxDTOs);
        }

        if (invoice.getDiscounts() != null) {
            List<PriceDTO> discountDTOs = invoice.getDiscounts().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setDiscounts(discountDTOs);
        }

        if (invoice.getGrandTotals() != null) {
            List<PriceDTO> grandTotalDTOs = invoice.getGrandTotals().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setGrandTotals(grandTotalDTOs);
        }

        if (invoice.getAmountsPaid() != null) {
            List<PriceDTO> amountsPaidDTOs = invoice.getAmountsPaid().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setAmountsPaid(amountsPaidDTOs);
        }

        if (invoice.getBalances() != null) {
            List<PriceDTO> balanceDTOs = invoice.getBalances().stream()
                .map(this::convertPriceToDTO)
                .collect(Collectors.toList());
            dto.setBalances(balanceDTOs);
        }

        // ========================
        // BANK ACCOUNTS
        // ========================
        // Fetch active bank accounts for currencies in the invoice
        if (invoice.getGrandTotals() != null && !invoice.getGrandTotals().isEmpty()) {
            List<String> currencies = invoice.getGrandTotals().stream()
                .map(Price::getCurrency)
                .distinct()
                .collect(Collectors.toList());

            List<BankAccount> bankAccounts = bankAccountRepository.findByCurrencyInAndIsActive(currencies, true);
            List<BankAccountDTO> bankAccountDTOs = bankAccounts.stream()
                .map(this::convertBankAccountToDTO)
                .collect(Collectors.toList());
            dto.setBankAccounts(bankAccountDTOs);
        }

        // ========================
        // SUMMARY STATISTICS
        // ========================
        dto.setTotalLineItemsCount(lineItems.size());
        dto.setTotalCurrenciesCount(invoice.getGrandTotals() != null ? invoice.getGrandTotals().size() : 0);

        // Extract unique currencies from grand totals
        if (invoice.getGrandTotals() != null && !invoice.getGrandTotals().isEmpty()) {
            List<String> currencies = invoice.getGrandTotals().stream()
                .map(Price::getCurrency)
                .distinct()
                .collect(Collectors.toList());
            dto.setCurrencies(currencies);
        }

        return dto;
    }

    /**
     * Convert InvoiceLineItem entity to LineItemDTO
     */
    private LineItemDTO convertLineItemToDTO(InvoiceLineItem item) {
        List<PriceDTO> priceDTOs = item.getPrices().stream()
            .map(this::convertPriceToDTO)
            .collect(Collectors.toList());

        return LineItemDTO.builder()
            .id(idObfuscator.encodeId(item.getId()))
            .itemType(item.getItemType().name())
            .itemTypeDisplayName(item.getItemType().getDisplayName())
            .itemName(item.getItemName())
            .description(item.getDescription())
            .displayOrder(item.getDisplayOrder())
            .prices(priceDTOs)
            .isActive(item.getIsActive())
            .build();
    }

    /**
     * Convert Price embeddable to PriceDTO with formatted values
     */
    private PriceDTO convertPriceToDTO(Price price) {
        String formattedUnitPrice = formatPrice(price.getCurrency(), price.getUnitPrice());
        String formattedTotalPrice = formatPrice(price.getCurrency(), price.getTotalPrice());

        return PriceDTO.builder()
            .currency(price.getCurrency())
            .quantity(price.getQuantity())
            .unitPrice(price.getUnitPrice())
            .totalPrice(price.getTotalPrice())
            .breakdown(price.getBreakdown())
            .formattedUnitPrice(formattedUnitPrice)
            .formattedTotalPrice(formattedTotalPrice)
            .build();
    }

    /**
     * Format price with currency symbol
     */
    private String formatPrice(String currencyCode, BigDecimal amount) {
        if (amount == null || currencyCode == null) {
            return null;
        }

        try {
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(Locale.US);
            formatter.setCurrency(currency);
            return formatter.format(amount);
        } catch (Exception e) {
            // Fallback: simple format
            return currencyCode + " " + amount.toString();
        }
    }

    /**
     * Get customer display name based on customer type
     */
    private String getCustomerDisplayName(Customer customer) {
        if (customer == null) {
            return null;
        }

        // For corporate/travel agent, use company name
        if (customer.getCustomerType() == CustomerType.CORPORATE ||
            customer.getCustomerType() == CustomerType.TRAVEL_AGENT) {
            return customer.getCompanyName();
        }

        // For individual, combine first and last name
        StringBuilder name = new StringBuilder();
        if (customer.getTitle() != null && !customer.getTitle().isEmpty()) {
            name.append(customer.getTitle()).append(" ");
        }
        if (customer.getFirstName() != null && !customer.getFirstName().isEmpty()) {
            name.append(customer.getFirstName());
        }
        if (customer.getLastName() != null && !customer.getLastName().isEmpty()) {
            if (name.length() > 0) {
                name.append(" ");
            }
            name.append(customer.getLastName());
        }

        return name.length() > 0 ? name.toString() : customer.getCode();
    }

    /**
     * Convert BankAccount entity to BankAccountDTO
     */
    private BankAccountDTO convertBankAccountToDTO(BankAccount bankAccount) {
        return BankAccountDTO.builder()
            .accountName(bankAccount.getAccountName())
            .accountHolderName(bankAccount.getAccountHolderName())
            .bankName(bankAccount.getBankName())
            .bankBranch(bankAccount.getBankBranch())
            .branchAddress(bankAccount.getBranchAddress())
            .branchCity(bankAccount.getBranchCity())
            .branchCountry(bankAccount.getBranchCountry())
            .accountNumber(bankAccount.getAccountNumber())
            .currency(bankAccount.getCurrency())
            .swiftBicCode(bankAccount.getSwiftBicCode())
            .iban(bankAccount.getIban())
            .routingNumber(bankAccount.getRoutingNumber())
            .sortCode(bankAccount.getSortCode())
            .intermediaryBankName(bankAccount.getIntermediaryBankName())
            .intermediarySwiftCode(bankAccount.getIntermediarySwiftCode())
            .invoiceDisplayNotes(bankAccount.getInvoiceDisplayNotes())
            .build();
    }
}
