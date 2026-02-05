package com.itineraryledger.kabengosafaris.BankAccount.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BankAccountDTO {
    private String id;
    private String accountCode;
    private String accountName;
    private String description;
    private String bankName;
    private String bankBranch;
    private String branchAddress;
    private String branchCity;
    private String branchCountry;
    private String accountNumber;
    private String accountHolderName;
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
    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
