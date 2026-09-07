package com.itineraryledger.kabengosafaris.Expense.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Who to advise, and which letter to use.
 *
 * Both optional. With neither, the advice goes to the vendor's own address on the default template,
 * which is the ordinary case. The override exists because a lodge's accounts desk is often not the
 * address reservations are booked through, and paying somebody is a conversation with a different
 * person than booking them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendPaymentAdviceDTO {

    /** Addresses to send to instead of the vendor's own. */
    private List<String> to;

    /** A specific template belonging to SEND_PAYMENT_ADVICE; the default is used when absent. */
    private Long emailTemplateId;
}
