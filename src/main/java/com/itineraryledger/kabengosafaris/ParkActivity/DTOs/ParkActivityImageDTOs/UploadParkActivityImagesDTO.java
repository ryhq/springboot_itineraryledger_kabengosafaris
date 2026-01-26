package com.itineraryledger.kabengosafaris.ParkActivity.DTOs.ParkActivityImageDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk uploading park activity images.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadParkActivityImagesDTO {

    private List<CreateParkActivityImageDTO> images;
}
