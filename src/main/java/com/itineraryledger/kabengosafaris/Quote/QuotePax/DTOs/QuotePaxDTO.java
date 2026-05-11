package com.itineraryledger.kabengosafaris.Quote.QuotePax.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotePaxDTO {
    private String id;
    private String quoteId;

    private String nationCategoryId;
    private String nationCategoryName;
    private String ageCategoryId;
    private String ageCategoryName;

    private Integer count;
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
