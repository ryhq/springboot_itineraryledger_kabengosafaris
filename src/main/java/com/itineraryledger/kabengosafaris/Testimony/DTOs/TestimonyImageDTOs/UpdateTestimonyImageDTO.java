package com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTestimonyImageDTO {

    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
}
