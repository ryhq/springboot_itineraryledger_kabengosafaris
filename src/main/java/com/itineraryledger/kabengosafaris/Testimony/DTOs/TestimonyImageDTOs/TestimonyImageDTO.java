package com.itineraryledger.kabengosafaris.Testimony.DTOs.TestimonyImageDTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestimonyImageDTO {

    private String id;
    private String testimonyId;
    private String imageUrl;
    private String fileImageUrl;
    private String fileName;
    private String originalFileName;
    private String altText;
    private String caption;
    private String description;
    private Boolean isPrimary;
    private Boolean isActive;
    private Integer displayOrder;
    private Long fileSize;
    private String fileSizeFormatted;
    private String mimeType;
    private Integer width;
    private Integer height;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
