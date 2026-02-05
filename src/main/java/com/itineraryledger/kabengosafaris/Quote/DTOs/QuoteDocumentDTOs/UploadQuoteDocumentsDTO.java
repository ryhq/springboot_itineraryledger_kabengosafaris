package com.itineraryledger.kabengosafaris.Quote.DTOs.QuoteDocumentDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for uploading multiple quote documents at once
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadQuoteDocumentsDTO {
    private List<CreateQuoteDocumentDTO> documents;
}
