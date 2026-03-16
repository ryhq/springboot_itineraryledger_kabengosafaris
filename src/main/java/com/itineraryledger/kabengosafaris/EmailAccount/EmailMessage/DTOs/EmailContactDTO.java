package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailContactDTO {

    private String id;
    private String emailAddress;
    private String displayName;
    private Integer frequency;
    private LocalDateTime lastContactedAt;
    private String source;
    private Boolean isStarred;
    private LocalDateTime createdAt;
}
