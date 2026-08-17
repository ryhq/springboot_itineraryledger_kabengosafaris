package com.itineraryledger.kabengosafaris.Quote.Controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.CreateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.ReorderQuoteItemsDTO;
import com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteItemDTOs.UpdateQuoteItemDTO;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteItemType;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemCreateService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemDeleteService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemGetService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemReorderService;
import com.itineraryledger.kabengosafaris.Quote.Services.QuoteItemServices.QuoteItemUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * QuoteItemController - REST controller for managing quote items
 */
@RestController
@RequestMapping("/api/quotes/{quoteId}/items")
@Slf4j
public class QuoteItemController {

    private final QuoteItemCreateService createService;
    private final QuoteItemUpdateService updateService;
    private final QuoteItemDeleteService deleteService;
    private final QuoteItemGetService getService;
    private final QuoteItemReorderService reorderService;

    @Autowired
    public QuoteItemController(
        QuoteItemCreateService createService,
        QuoteItemUpdateService updateService,
        QuoteItemDeleteService deleteService,
        QuoteItemGetService getService,
        QuoteItemReorderService reorderService
    ) {
        this.createService = createService;
        this.updateService = updateService;
        this.deleteService = deleteService;
        this.getService = getService;
        this.reorderService = reorderService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_QUOTE_ITEM')")
    public ResponseEntity<ApiResponse<?>> createItem(
        @PathVariable String quoteId,
        @Valid @RequestBody CreateQuoteItemDTO createDTO
    ) {
        log.info("POST /api/quotes/{}/items - Creating new item: {}", quoteId, createDTO.getItemName());
        // Ensure the quoteId from path matches the one in DTO
        createDTO.setQuoteId(quoteId);
        return createService.createQuoteItem(createDTO);
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_ITEM')")
    public ResponseEntity<ApiResponse<?>> updateItem(
        @PathVariable String quoteId,
        @PathVariable String itemId,
        @Valid @RequestBody UpdateQuoteItemDTO updateDTO
    ) {
        log.info("PUT /api/quotes/{}/items/{} - Updating item", quoteId, itemId);
        return updateService.updateQuoteItem(itemId, updateDTO);
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('PERM_DELETE_QUOTE_ITEM')")
    public ResponseEntity<ApiResponse<?>> deleteItems(
        @PathVariable String quoteId,
        @RequestBody List<String> itemIds
    ) {
        log.info("DELETE /api/quotes/{}/items - Deleting {} items", quoteId, itemIds.size());
        return deleteService.deleteQuoteItems(itemIds);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_ITEM')")
    public ResponseEntity<ApiResponse<?>> getItems(
        @PathVariable String quoteId,
        @RequestParam(required = false) QuoteItemType itemType,
        @RequestParam(required = false) String itemName,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String itemTypeGroup,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "10") Integer size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("GET /api/quotes/{}/items - Fetching items with filters", quoteId);
        return getService.getQuoteItemsByQuoteId(
            quoteId,
            itemType,
            itemName,
            description,
            isActive,
            itemTypeGroup,
            page,
            size,
            sortBy,
            sortDirection
        );
    }

    @GetMapping("/{itemId}")
    @PreAuthorize("hasAuthority('PERM_READ_QUOTE_ITEM')")
    public ResponseEntity<ApiResponse<?>> getItem(
        @PathVariable String quoteId,
        @PathVariable String itemId,
        /* the list's filters travel with the record so its arrows stay in that set */
        @RequestParam(required = false) QuoteItemType itemType,
        @RequestParam(required = false) String itemName,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) String itemTypeGroup,
        @RequestParam(required = false) String sortBy,
        @RequestParam(required = false) String sortDirection
    ) {
        log.info("GET /api/quotes/{}/items/{} - Fetching item", quoteId, itemId);
        return getService.getQuoteItemById(itemId, quoteId, itemType, itemName, description, isActive, itemTypeGroup, sortBy, sortDirection);
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_ITEM')")
    public ResponseEntity<ApiResponse<?>> reorderItems(
        @PathVariable String quoteId,
        @Valid @RequestBody ReorderQuoteItemsDTO reorderDTO
    ) {
        log.info("POST /api/quotes/{}/items/reorder - Reordering {} items", quoteId, reorderDTO.getItemOrder().size());
        return reorderService.reorderQuoteItems(quoteId, reorderDTO);
    }

    // shared bulk-flag endpoint (see Response/BulkFlags)
    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Response.BulkFlags bulkFlags;

    @org.springframework.beans.factory.annotation.Autowired
    private com.itineraryledger.kabengosafaris.Quote.Repository.QuoteItemRepository bulkFlagsRepository;

    /**
     * PATCH /bulk — take a whole selection of lines in or out of the totals.
     *
     * One request rather than one per line: leaving a selection half-applied,
     * with nobody told which half, is how a quote goes out with a line the office
     * thought it had removed.
     */
    @org.springframework.web.bind.annotation.PatchMapping("/bulk")
    @PreAuthorize("hasAuthority('PERM_UPDATE_QUOTE_ITEM')")
    public ResponseEntity<?> bulkFlags(
        @PathVariable String quoteId,
        @RequestBody com.itineraryledger.kabengosafaris.Response.BulkFlags.Request request
    ) {
        log.info("PATCH /api/quotes/{}/items/bulk", quoteId);
        return bulkFlags.apply("quote line", bulkFlagsRepository, request, entity -> {
            if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        });
    }
}
