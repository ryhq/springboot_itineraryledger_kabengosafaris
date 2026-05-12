package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.MuteRule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MuteRuleDTO {
    private String id;
    private String name;
    private MuteRule.MatchField matchField;
    private MuteRule.MatchMode matchMode;
    private String matchPattern;
    private Boolean isActive;
}
