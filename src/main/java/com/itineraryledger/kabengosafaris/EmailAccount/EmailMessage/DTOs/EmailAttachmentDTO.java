package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailAttachmentDTO {
    private String id;
    private String fileName;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private Boolean isInline;
}
