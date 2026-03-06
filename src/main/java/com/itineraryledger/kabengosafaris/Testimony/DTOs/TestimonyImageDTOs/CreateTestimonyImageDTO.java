package com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTestimonyImageDTO {

    @NotNull(message = "Testimony ID is required")
    private String testimonyId;

    @NotNull(message = "Image file is required")
    private MultipartFile image;

    private String altText;
    private String caption;
    private String description;
}
