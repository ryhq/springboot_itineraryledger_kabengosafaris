package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailLabelColor;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateEmailLabelDTO {

    @Size(max = 60)
    private String name;

    /*
     * Enums arrive as Strings so a blank can CLEAR the field; null still means
     * "leave unchanged". Bound as the enum itself, an empty value makes Jackson
     * reject the whole request body, which is how a set value became impossible
     * to unset (see the charging-basis fix).
     */
    private String color;
}
