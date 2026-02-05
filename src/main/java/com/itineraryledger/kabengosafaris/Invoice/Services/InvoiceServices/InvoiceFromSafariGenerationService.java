package com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceServices;

import com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryCostEstimationDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceFromSafariDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.CreateInvoiceLineItemDTO;
import com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDTO;
import com.itineraryledger.kabengosafaris.Invoice.Enums.InvoiceItemType;
import com.itineraryledger.kabengosafaris.Invoice.Services.InvoiceLineItemServices.InvoiceLineItemCreateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Safari.Entity.Safari;
import com.itineraryledger.kabengosafaris.Safari.Repository.SafariRepository;
import com.itineraryledger.kabengosafaris.Safari.Services.SafariCostEstimationService;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * InvoiceFromSafariGenerationService - Generates an invoice from a Safari with cost estimation
 *
 * This service bridges the Safari and Invoice modules by:
 * 1. Using SafariCostEstimationService to calculate costs
 * 2. Creating a new Invoice entity linked to both Customer and Safari
 * 3. Converting cost line items to InvoiceLineItems
 * 4. Setting up proper relationships - Customer is derived from Safari
 *
 * IMPORTANT: Safari MUST have a Customer linked to it. If not, invoice creation is rejected.
 */
@Service
@Slf4j
@Transactional
public class InvoiceFromSafariGenerationService {

    private final SafariCostEstimationService costEstimationService;
    private final InvoiceCreateService invoiceCreateService;
    private final InvoiceLineItemCreateService invoiceLineItemCreateService;
    private final SafariRepository safariRepository;
    private final IdObfuscator idObfuscator;

    @Autowired
    public InvoiceFromSafariGenerationService(
            SafariCostEstimationService costEstimationService,
            InvoiceCreateService invoiceCreateService,
            InvoiceLineItemCreateService invoiceLineItemCreateService,
            SafariRepository safariRepository,
            IdObfuscator idObfuscator
    ) {
        this.costEstimationService = costEstimationService;
        this.invoiceCreateService = invoiceCreateService;
        this.invoiceLineItemCreateService = invoiceLineItemCreateService;
        this.safariRepository = safariRepository;
        this.idObfuscator = idObfuscator;
    }

    /**
     * Generate an invoice from a Safari
     *
     * Customer is automatically derived from the Safari. If the Safari has no Customer linked,
     * the invoice creation will be rejected.
     *
     * @param dto CreateInvoiceFromSafariDTO containing:
     *            - safariId: The obfuscated safari ID (required)
     *            - title: Invoice title (optional, defaults to safari name)
     *            - description: Invoice description (optional)
     *            - useStoRate: Whether to use STO rates (true) or RACK rates (false) - default: false
     *            - currency: Preferred currency for cost estimation (default: USD)
     *            - taxPercentage: Optional tax percentage to apply
     *            - discountPercentage: Optional discount percentage to apply
     *            - discountReason: Optional reason for discount
     *            - issueDate: Invoice issue date (required)
     *            - dueDate: Payment due date (required)
     *            - internalNotes: Optional internal notes
     *            - customerNotes: Optional customer-facing notes
     *            - paymentTerms: Optional payment terms description
     * @return ResponseEntity with the created invoice
     */
    public ResponseEntity<ApiResponse<?>> generateInvoiceFromSafari(CreateInvoiceFromSafariDTO dto) {
        log.info("Generating invoice for safari: {}", dto.getSafariId());

        // Default useStoRate to false if not provided (RACK rates)
        boolean useStoRates = dto.getUseStoRate() != null ? dto.getUseStoRate() : false;

        // Default currency to USD if not provided
        String currency = (dto.getCurrency() != null && !dto.getCurrency().isBlank()) ? dto.getCurrency() : "USD";

        try {
            // 1. Validate inputs and decode Safari ID
            Long safariId = idObfuscator.decodeId(dto.getSafariId());

            if (safariId == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400, "Invalid safari ID", "INVALID_SAFARI_ID")
                );
            }

            // 2. Verify Safari exists and has a Customer linked
            Safari safari = safariRepository.findById(safariId).orElse(null);
            if (safari == null) {
                return ResponseEntity.status(404).body(
                        ApiResponse.error(404, "Safari not found", "SAFARI_NOT_FOUND")
                );
            }

            // CRITICAL: Safari must have a customer linked
            if (safari.getCustomer() == null) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(400,
                                "Cannot create invoice: Safari has no customer linked. Please link a customer to the safari first.",
                                "SAFARI_NO_CUSTOMER")
                );
            }

            // 3. Verify safari has customer and log it
            log.info("Customer derived from safari: {}", safari.getCustomer().getDisplayName());

            // 4. Get cost estimation from safari
            ResponseEntity<ApiResponse<?>> costResponse =
                    costEstimationService.estimateCosts(dto.getSafariId(), useStoRates, currency);

            if (!costResponse.getStatusCode().is2xxSuccessful() || costResponse.getBody() == null) {
                log.error("Failed to get cost estimation for safari: {}", dto.getSafariId());
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to estimate costs", "COST_ESTIMATION_FAILED")
                );
            }

            ItineraryCostEstimationDTO costEstimation = (ItineraryCostEstimationDTO) costResponse.getBody().getData();
            if (costEstimation == null) {
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Cost estimation returned null", "NULL_ESTIMATION")
                );
            }

            // 5. Create the Invoice entity
            CreateInvoiceDTO createInvoiceDTO = buildCreateInvoiceDTO(
                    dto.getSafariId(),
                    safari,
                    costEstimation,
                    dto.getTitle(),
                    dto.getDescription(),
                    dto.getIssueDate(),
                    dto.getDueDate(),
                    dto.getTaxPercentage(),
                    dto.getDiscountPercentage(),
                    dto.getDiscountReason(),
                    dto.getInternalNotes(),
                    dto.getCustomerNotes(),
                    dto.getPaymentTerms()
            );

            ResponseEntity<ApiResponse<?>> invoiceResponse = invoiceCreateService.createInvoice(createInvoiceDTO);
            if (!invoiceResponse.getStatusCode().is2xxSuccessful() || invoiceResponse.getBody() == null) {
                log.error("Failed to create invoice");
                return ResponseEntity.status(500).body(
                        ApiResponse.error(500, "Failed to create invoice", "INVOICE_CREATION_FAILED")
                );
            }

            InvoiceDTO invoiceDTO = (InvoiceDTO) invoiceResponse.getBody().getData();
            String invoiceId = invoiceDTO.getId();

            // 6. Create InvoiceLineItems from cost estimation line items
            int itemsCreated = createInvoiceLineItemsFromEstimation(invoiceId, costEstimation);

            log.info("Successfully generated invoice: {} with {} items for safari: {}",
                    invoiceDTO.getInvoiceCode(), itemsCreated, dto.getSafariId());

            // 7. Return the created invoice (totals are automatically calculated)
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(201,
                            "Invoice generated successfully from safari",
                            invoiceDTO)
            );

        } catch (Exception e) {
            log.error("Error generating invoice from safari", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(500,
                            "Failed to generate invoice: " + e.getMessage(),
                            "INVOICE_GENERATION_FAILED")
            );
        }
    }

    /**
     * Build CreateInvoiceDTO from safari and cost estimation
     */
    private CreateInvoiceDTO buildCreateInvoiceDTO(
            String safariIdObfuscated,
            Safari safari,
            ItineraryCostEstimationDTO costEstimation,
            String titleOverride,
            String descriptionOverride,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal taxPercentage,
            BigDecimal discountPercentage,
            String discountReason,
            String internalNotes,
            String customerNotes,
            String paymentTerms
    ) {
        CreateInvoiceDTO dto = new CreateInvoiceDTO();

        // Basic information (with override support)
        String title = (titleOverride != null && !titleOverride.isBlank())
                ? titleOverride
                : (safari.getName() != null ? safari.getName() + " - Invoice" : "Safari Invoice");
        dto.setTitle(title);

        String description = (descriptionOverride != null && !descriptionOverride.isBlank())
                ? descriptionOverride
                : safari.getDescription();
        dto.setDescription(description);

        dto.setSafariId(safariIdObfuscated);

        // Invoice dates
        dto.setIssueDate(issueDate);
        dto.setDueDate(dueDate);

        // Pricing details
        dto.setTaxPercentage(taxPercentage); // Can be null

        if (discountPercentage != null) {
            dto.setDiscountPercentage(discountPercentage);
            dto.setDiscountReason(discountReason);
        }

        // Notes
        dto.setInternalNotes(internalNotes);
        dto.setCustomerNotes(customerNotes);
        dto.setPaymentTerms(paymentTerms);

        // Default: active
        dto.setIsActive(true);

        return dto;
    }

    /**
     * Create InvoiceLineItems from cost estimation breakdown
     *
     * @param invoiceId The obfuscated invoice ID
     * @param costEstimation The cost estimation DTO
     * @return Number of items created
     */
    private int createInvoiceLineItemsFromEstimation(String invoiceId, ItineraryCostEstimationDTO costEstimation) {
        int itemsCreated = 0;

        // Process accommodation costs
        if (costEstimation.getAccommodationCosts() != null &&
                costEstimation.getAccommodationCosts().getItems() != null) {
            for (ItineraryCostEstimationDTO.CostLineItem lineItem :
                    costEstimation.getAccommodationCosts().getItems()) {
                createInvoiceLineItemFromLineItem(invoiceId, lineItem, InvoiceItemType.ACCOMMODATION);
                itemsCreated++;
            }
        }

        // Process park fee costs
        if (costEstimation.getParkFeeCosts() != null &&
                costEstimation.getParkFeeCosts().getItems() != null) {
            for (ItineraryCostEstimationDTO.CostLineItem lineItem :
                    costEstimation.getParkFeeCosts().getItems()) {
                createInvoiceLineItemFromLineItem(invoiceId, lineItem, InvoiceItemType.PARK_FEE);
                itemsCreated++;
            }
        }

        // Process activity costs
        if (costEstimation.getActivityCosts() != null &&
                costEstimation.getActivityCosts().getItems() != null) {
            for (ItineraryCostEstimationDTO.CostLineItem lineItem :
                    costEstimation.getActivityCosts().getItems()) {
                createInvoiceLineItemFromLineItem(invoiceId, lineItem, InvoiceItemType.ACTIVITY);
                itemsCreated++;
            }
        }

        return itemsCreated;
    }

    /**
     * Create a single InvoiceLineItem from a cost estimation line item
     */
    private void createInvoiceLineItemFromLineItem(
            String invoiceId,
            ItineraryCostEstimationDTO.CostLineItem lineItem,
            InvoiceItemType itemType
    ) {
        CreateInvoiceLineItemDTO itemDTO = new CreateInvoiceLineItemDTO();
        itemDTO.setItemType(itemType);
        itemDTO.setItemName(lineItem.getItemName());

        // Build description from available information
        StringBuilder description = new StringBuilder();
        if (lineItem.getDayNumber() != null) {
            description.append("Day ").append(lineItem.getDayNumber());
        }
        if (lineItem.getPaxCategory() != null && !lineItem.getPaxCategory().isEmpty()) {
            if (description.length() > 0) description.append(" - ");
            description.append(lineItem.getPaxCategory());
        }
        if (description.length() > 0) {
            itemDTO.setDescription(description.toString());
        }

        // Convert price to InvoiceLineItem price format
        List<CreateInvoiceLineItemDTO.PriceInput> prices = new ArrayList<>();
        CreateInvoiceLineItemDTO.PriceInput price = new CreateInvoiceLineItemDTO.PriceInput();
        price.setCurrency(lineItem.getCurrency());
        price.setQuantity(lineItem.getQuantity());
        price.setUnitPrice(lineItem.getUnitPrice());

        // Add breakdown if rate was found or missing
        if (lineItem.getRateFound() != null && !lineItem.getRateFound()) {
            price.setBreakdown("Rate not found - estimated");
        }

        prices.add(price);
        itemDTO.setPrices(prices);
        itemDTO.setIsActive(true);

        // Create the item
        try {
            invoiceLineItemCreateService.createInvoiceLineItem(invoiceId, itemDTO);
        } catch (Exception e) {
            log.error("Failed to create invoice line item: {} - {}", lineItem.getItemName(), e.getMessage());
            // Continue creating other items even if one fails
        }
    }
}
