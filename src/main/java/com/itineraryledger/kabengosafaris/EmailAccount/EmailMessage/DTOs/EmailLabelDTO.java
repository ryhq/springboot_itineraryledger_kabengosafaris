package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabelColor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailLabelDTO {
    private String id;
    private String name;
    private EmailLabelColor color;
    /** the exact colour if one was chosen; null means the family's colour */
    private String colorHex;
    private Boolean isSystem;
    private Long count;
}
