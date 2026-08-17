package com.itineraryledger.kabengosafaris.Blog.DTOs.BlogImageDTOs;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The multipart wrapper Spring binds the indexed `images[i]` fields into. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadBlogImagesDTO {

    @NotEmpty(message = "At least one image is required")
    @Valid
    private List<CreateBlogImageDTO> images;
}
