package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * §10 — body for POST /messages/{id}/quick-reply. The endpoint reuses
 * the original message's subject (with Re: prefix if missing) and the
 * sender as the To.
 */
@Data
public class QuickReplyDTO {

    public enum ReplyMode { REPLY, REPLY_ALL }

    @NotBlank
    private String body;

    private ReplyMode replyMode = ReplyMode.REPLY;
}
