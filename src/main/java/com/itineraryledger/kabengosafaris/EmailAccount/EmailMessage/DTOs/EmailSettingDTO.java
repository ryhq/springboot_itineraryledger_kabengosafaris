package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailSetting;
import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailSettingDTO {
    private String id;
    private String displayName;
    private String settingKey;
    private String settingValue;
    private SettingDataType dataType;
    private String description;
    private Boolean active = true;
    private Boolean isSystemDefault = false;
    private EmailSetting.Category category;
    private Boolean requiresRestart = false;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
