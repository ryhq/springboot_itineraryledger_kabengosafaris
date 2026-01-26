package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityDocumentDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk uploading activity documents.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadActivityDocumentsDTO {

    private List<CreateActivityDocumentDTO> documents;
}
