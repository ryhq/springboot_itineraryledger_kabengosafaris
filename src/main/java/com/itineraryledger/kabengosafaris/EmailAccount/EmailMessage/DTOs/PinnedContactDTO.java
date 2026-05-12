package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PinnedContactDTO {
    private String id;
    private String email;
    private String name;
    private String role;
}
