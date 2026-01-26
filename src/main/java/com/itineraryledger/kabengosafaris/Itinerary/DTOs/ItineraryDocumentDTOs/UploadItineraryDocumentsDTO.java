package com.itineraryledger.kabengosafaris.Itinerary.DTOs.ItineraryDocumentDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk uploading itinerary documents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadItineraryDocumentsDTO {

    private List<CreateItineraryDocumentDTO> documents;
}
