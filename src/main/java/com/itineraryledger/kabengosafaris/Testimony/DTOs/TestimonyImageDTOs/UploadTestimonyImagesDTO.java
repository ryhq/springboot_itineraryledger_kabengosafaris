package com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadTestimonyImagesDTO {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<CreateTestimonyImageDTO> images;
}
