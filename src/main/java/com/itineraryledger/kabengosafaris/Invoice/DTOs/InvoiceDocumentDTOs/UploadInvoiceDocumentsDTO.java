package com.itineraryledger.kabengosafaris.Invoice.DTOs.InvoiceDocumentDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for uploading multiple invoice documents at once
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadInvoiceDocumentsDTO {
    private List<CreateInvoiceDocumentDTO> documents;
}
