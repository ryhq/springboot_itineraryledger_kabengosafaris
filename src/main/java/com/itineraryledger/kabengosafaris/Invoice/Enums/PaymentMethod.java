package com.itineraryledger.kabengosafaris.Invoice.Enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    BANK_TRANSFER("Bank Transfer", "Wire transfer or bank deposit"),
    CASH("Cash", "Cash payment"),
    CREDIT_CARD("Credit Card", "Credit or debit card payment"),
    MOBILE_MONEY("Mobile Money", "Mobile money payment (M-Pesa, etc.)"),
    CHEQUE("Cheque", "Payment by cheque"),
    PAYPAL("PayPal", "PayPal payment"),
    OTHER("Other", "Other payment method");

    private final String displayName;
    private final String description;

    PaymentMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
}
