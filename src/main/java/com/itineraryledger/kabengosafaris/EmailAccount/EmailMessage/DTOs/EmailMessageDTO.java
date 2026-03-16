package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.time.LocalDateTime;
import java.util.List;

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
public class EmailMessageDTO {
    private String id;
    private String folderId;
    private String folderName;
    private String messageId;
    private String inReplyTo;
    private String threadId;
    private String fromAddress;
    private String fromName;
    private String toAddresses;
    private String ccAddresses;
    private String bccAddresses;
    private String subject;
    private String snippet;
    private String htmlBody;
    private Boolean isRead;
    private Boolean isStarred;
    private Boolean isDraft;
    private Boolean hasAttachments;
    private Integer attachmentCount;
    private Long fileSize;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;
    private List<EmailAttachmentDTO> attachments;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
