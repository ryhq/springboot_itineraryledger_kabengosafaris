package com.itineraryledger.kabengosafaris.Activity.DTOs.ActivityImageDTOs;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bulk uploading activity images.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadActivityImagesDTO {

    private List<CreateActivityImageDTO> images;
}
