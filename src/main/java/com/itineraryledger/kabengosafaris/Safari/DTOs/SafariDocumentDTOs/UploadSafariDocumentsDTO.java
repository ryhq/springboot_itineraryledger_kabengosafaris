package com.itineraryledger.kabengosafaris.Safari.DTOs.SafariDocumentDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk uploading safari documents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadSafariDocumentsDTO {

    private List<CreateSafariDocumentDTO> documents;
}
