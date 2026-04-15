package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DocxSettingDTO {
    private String id;
    private String displayName;
    private String settingKey;
    private String settingValue;
    private SettingDataType dataType;
    private String description;
    private Boolean active;
    private Boolean isSystemDefault;
    private DocxSetting.Category category;
    private Boolean requiresRestart;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
