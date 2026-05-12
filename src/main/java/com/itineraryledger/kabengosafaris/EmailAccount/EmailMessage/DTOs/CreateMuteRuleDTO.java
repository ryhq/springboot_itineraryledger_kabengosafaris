package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.MuteRule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateMuteRuleDTO {

    @NotBlank
    @Size(max = 80)
    private String name;

    @NotNull
    private MuteRule.MatchField matchField;

    private MuteRule.MatchMode matchMode;

    @NotBlank
    @Size(max = 400)
    private String matchPattern;
}
