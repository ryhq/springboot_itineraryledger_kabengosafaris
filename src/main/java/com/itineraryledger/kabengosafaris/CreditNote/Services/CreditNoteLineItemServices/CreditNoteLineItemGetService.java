package com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteLineItemServices;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.CreditNote.DTOs.CreditNoteLineItemDTO;
import com.itineraryledger.kabengosafaris.CreditNote.Entity.CreditNoteLineItem;
import com.itineraryledger.kabengosafaris.CreditNote.Repository.CreditNoteLineItemRepository;
import com.itineraryledger.kabengosafaris.CreditNote.Services.CreditNoteServices.CreditNoteCreateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for retrieving credit note line items
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreditNoteLineItemGetService {

    private final CreditNoteLineItemRepository creditNoteLineItemRepository;
    private final IdObfuscator idObfuscator;
    private final CreditNoteCreateService creditNoteCreateService;

    /**
     * Get all line items for a specific credit note
     *
     * @param creditNoteIdObfuscated The obfuscated credit note ID
     * @return ResponseEntity with ApiResponse containing list of line item DTOs
     */
    public ResponseEntity<ApiResponse<?>> getLineItemsByCreditNote(String creditNoteIdObfuscated) {
        log.info("Fetching line items for credit note: {}", creditNoteIdObfuscated);

        try {
            Long creditNoteId;
            try {
                creditNoteId = idObfuscator.decodeId(creditNoteIdObfuscated);
            } catch (Exception e) {
                log.warn("Failed to decode credit note ID: {}", creditNoteIdObfuscated, e);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Invalid credit note ID", "INVALID_CREDIT_NOTE_ID")
                );
            }

            List<CreditNoteLineItem> lineItems = creditNoteLineItemRepository
                .findByCreditNoteIdOrderByDisplayOrderAsc(creditNoteId);

            List<CreditNoteLineItemDTO> lineItemDTOs = lineItems.stream()
                .map(creditNoteCreateService::convertLineItemToDTO)
                .collect(Collectors.toList());

            return ResponseEntity.ok().body(
                ApiResponse.success(200, "Credit note line items retrieved successfully", lineItemDTOs)
            );

        } catch (Exception e) {
            log.error("Error fetching credit note line items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to fetch credit note line items", "CREDIT_NOTE_LINE_ITEMS_FETCH_FAILED")
            );
        }
    }
}
