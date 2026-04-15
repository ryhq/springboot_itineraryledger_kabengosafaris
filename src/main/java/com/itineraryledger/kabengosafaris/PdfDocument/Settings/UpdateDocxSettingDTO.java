package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDocxSettingDTO {
    private String settingValue;
    private Boolean active = true;
}
