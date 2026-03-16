package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.time.LocalDateTime;

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
public class EmailMessageListDTO {
    private String id;
    private String fromAddress;
    private String fromName;
    private String toAddresses;
    private String subject;
    private String snippet;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean hasAttachments;
    private Integer attachmentCount;
    private LocalDateTime sentAt;
}
