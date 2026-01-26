package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityDocumentDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk uploading park activity documents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadParkActivityDocumentsDTO {

    private List<CreateParkActivityDocumentDTO> documents;
}
