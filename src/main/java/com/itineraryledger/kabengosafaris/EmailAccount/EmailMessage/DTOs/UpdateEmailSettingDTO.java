package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmailSettingDTO {
    private String settingValue;
    private Boolean active;
}
