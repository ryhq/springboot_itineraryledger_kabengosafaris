package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailFolderDTO {
    private String id;
    private String name;
    private EmailFolderType type;
    private Boolean isSystem;
    private Integer messageCount;
    private Integer unreadCount;
    private String remoteFolderName;
}
