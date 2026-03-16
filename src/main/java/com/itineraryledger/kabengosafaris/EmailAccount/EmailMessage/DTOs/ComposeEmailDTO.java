package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComposeEmailDTO {

    @NotEmpty(message = "At least one recipient is required")
    private List<String> toAddresses;

    private List<String> ccAddresses;
    private List<String> bccAddresses;

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Email body is required")
    private String htmlBody;

    private Boolean isDraft;

    /**
     * Obfuscated message ID of the message being replied to (for reply/reply-all)
     */
    private String inReplyToMessageId;
}
