package com.itineraryledger.kabengosafaris.BankAccount.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateBankAccountDTO {
    @NotBlank(message = "Account name is required")
    private String accountName;

    private String description;

    @NotBlank(message = "Bank name is required")
    private String bankName;

    private String bankBranch;
    private String branchAddress;
    private String branchCity;
    private String branchCountry;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code (e.g., USD, TZS, EUR)")
    private String currency;

    private String swiftBicCode;
    private String iban;
    private String routingNumber;
    private String sortCode;
    private String intermediaryBankName;
    private String intermediarySwiftCode;
    private Boolean isDefault;
    private Boolean isActive;
    private String internalNotes;
    private String invoiceDisplayNotes;
}
