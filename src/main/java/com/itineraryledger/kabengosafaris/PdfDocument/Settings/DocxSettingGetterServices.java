package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reads DOCX settings with fallback to application.properties.
 *
 * Consumers (WordGenerator and friends) call these getters per request — there
 * is no caching, so updates via {@link DocxSettingServices#updateDocxSetting}
 * are visible immediately without restart.
 *
 * For {@code docx.engine} this means a user can flip between docx4j and
 * libreoffice at runtime. For {@code jodconverter.local.*} values, the
 * getters return the authoritative current value, but the JODConverter
 * OfficeManager Spring bean was built from the values present at startup —
 * changes to those keys require an app restart (flagged via
 * {@code requiresRestart=true} on the setting row).
 */
@Service
public class DocxSettingGetterServices {

    // =====================================================================
    // Fallback values from application.properties — ENGINE
    // =====================================================================

    @Value("${docx.engine:docx4j}")
    private String docxEngine;

    // =====================================================================
    // Fallback values from application.properties — LIBREOFFICE (JODConverter)
    // =====================================================================

    @Value("${jodconverter.local.enabled:false}")
    private Boolean jodconverterEnabled;

    @Value("${jodconverter.local.office-home:}")
    private String jodconverterOfficeHome;

    @Value("${jodconverter.local.port-numbers:2002}")
    private String jodconverterPortNumbers;

    @Value("${jodconverter.local.max-tasks-per-process:100}")
    private Integer jodconverterMaxTasksPerProcess;

    @Value("${jodconverter.local.task-execution-timeout:120000}")
    private Long jodconverterTaskExecutionTimeout;

    @Value("${jodconverter.local.task-queue-timeout:30000}")
    private Long jodconverterTaskQueueTimeout;

    @Autowired
    private DocxSettingRepository docxSettingRepository;

    // =====================================================================
    // ENGINE getters
    // =====================================================================

    /** @return "docx4j" or "libreoffice" — the DOCX engine to use. */
    public String getDocxEngine() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("docx.engine").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return docxEngine;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : docxEngine;
    }

    // =====================================================================
    // LIBREOFFICE getters (read-only informational — actual runtime values
    // are controlled by Spring-Boot-level config at startup)
    // =====================================================================

    public Boolean isJodconverterEnabled() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("jodconverter.local.enabled").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return jodconverterEnabled;
        }
        try {
            return Boolean.parseBoolean(setting.getSettingValue());
        } catch (Exception e) {
            return jodconverterEnabled;
        }
    }

    public String getJodconverterOfficeHome() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("jodconverter.local.office-home").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return jodconverterOfficeHome;
        }
        return setting.getSettingValue() != null ? setting.getSettingValue() : "";
    }

    public String getJodconverterPortNumbers() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("jodconverter.local.port-numbers").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return jodconverterPortNumbers;
        }
        String value = setting.getSettingValue();
        return (value != null && !value.isBlank()) ? value : jodconverterPortNumbers;
    }

    public Integer getJodconverterMaxTasksPerProcess() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("jodconverter.local.max-tasks-per-process").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return jodconverterMaxTasksPerProcess;
        }
        try {
            return Integer.parseInt(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return jodconverterMaxTasksPerProcess;
        }
    }

    public Long getJodconverterTaskExecutionTimeout() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("jodconverter.local.task-execution-timeout").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return jodconverterTaskExecutionTimeout;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return jodconverterTaskExecutionTimeout;
        }
    }

    public Long getJodconverterTaskQueueTimeout() {
        DocxSetting setting = docxSettingRepository.findBySettingKey("jodconverter.local.task-queue-timeout").orElse(null);
        if (setting == null || !Boolean.TRUE.equals(setting.getActive())) {
            return jodconverterTaskQueueTimeout;
        }
        try {
            return Long.parseLong(setting.getSettingValue());
        } catch (NumberFormatException e) {
            return jodconverterTaskQueueTimeout;
        }
    }
}
