package com.itineraryledger.kabengosafaris.Initializers;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import com.itineraryledger.kabengosafaris.PdfDocument.Settings.DocxSetting;
import com.itineraryledger.kabengosafaris.PdfDocument.Settings.DocxSettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds DOCX generation settings from application.properties into the DB
 * on first boot. Idempotent — existing rows are preserved so user edits
 * survive restarts.
 *
 * Order: HIGHEST_PRECEDENCE + 16 — runs right after TranslationSettingsInitializer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocxSettingsInitializer implements ApplicationRunner, Ordered {

    private final DocxSettingRepository docxSettingRepository;

    // =====================================================================
    // ENGINE defaults
    // =====================================================================

    @Value("${docx.engine:docx4j}")
    private String docxEngine;

    // =====================================================================
    // LIBREOFFICE / JODConverter defaults
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

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 16;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        printStartBanner();
        try {
            initializeDocxSettings();
            printEndBanner(true);
        } catch (Exception e) {
            log.error("Error during DOCX Settings initialization", e);
            printEndBanner(false);
        }
    }

    private void printStartBanner() {
        log.info("");
        log.info("╔════════════════════════════════════════════════════════════════════╗");
        log.info("║                                                                    ║");
        log.info("║              DOCX SETTINGS INITIALIZER - START                     ║");
        log.info("║                                                                    ║");
        log.info("╚════════════════════════════════════════════════════════════════════╝");
        log.info("");
    }

    private void printEndBanner(boolean success) {
        log.info("");
        if (success) {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✓ DOCX SETTINGS INITIALIZER - COMPLETED                    ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        } else {
            log.info("╔════════════════════════════════════════════════════════════════════╗");
            log.info("║                                                                    ║");
            log.info("║         ✗ DOCX SETTINGS INITIALIZER - FAILED                       ║");
            log.info("║                                                                    ║");
            log.info("╚════════════════════════════════════════════════════════════════════╝");
        }
        log.info("");
    }

    private void initializeDocxSettings() {
        // ENGINE — takes effect immediately (WordGenerator re-reads per request)
        createIfMissing(
            "docx.engine",
            docxEngine,
            SettingDataType.STRING,
            "DOCX generation engine: 'docx4j' (in-JVM, no host deps, drops @page/running footers) " +
                "or 'libreoffice' (headless LibreOffice via JODConverter — higher fidelity, " +
                "requires LibreOffice installed AND jodconverter.local.enabled=true).",
            DocxSetting.Category.ENGINE,
            false
        );

        // LIBREOFFICE / JODConverter — these are baked into the OfficeManager
        // Spring bean at startup. Changes require a restart (flagged).
        createIfMissing(
            "jodconverter.local.enabled",
            String.valueOf(jodconverterEnabled),
            SettingDataType.BOOLEAN,
            "Enable the JODConverter local OfficeManager. Required to use docx.engine=libreoffice. " +
                "Requires LibreOffice installed on the host (apt install libreoffice-core libreoffice-writer). " +
                "⚠ Requires application restart.",
            DocxSetting.Category.LIBREOFFICE,
            true
        );

        createIfMissing(
            "jodconverter.local.office-home",
            jodconverterOfficeHome != null ? jodconverterOfficeHome : "",
            SettingDataType.STRING,
            "Explicit path to the LibreOffice install (e.g. /usr/lib/libreoffice). " +
                "Leave blank on Debian/Ubuntu to let JODConverter autodiscover. " +
                "⚠ Requires application restart.",
            DocxSetting.Category.LIBREOFFICE,
            true
        );

        createIfMissing(
            "jodconverter.local.port-numbers",
            jodconverterPortNumbers,
            SettingDataType.STRING,
            "Comma-separated ports for the soffice pool. Multiple ports = multiple parallel " +
                "office instances (e.g. '2002,2003,2004'). ⚠ Requires application restart.",
            DocxSetting.Category.LIBREOFFICE,
            true
        );

        createIfMissing(
            "jodconverter.local.max-tasks-per-process",
            String.valueOf(jodconverterMaxTasksPerProcess),
            SettingDataType.INTEGER,
            "Recycle each soffice process after this many conversions (defends against memory leaks). " +
                "⚠ Requires application restart.",
            DocxSetting.Category.LIBREOFFICE,
            true
        );

        createIfMissing(
            "jodconverter.local.task-execution-timeout",
            String.valueOf(jodconverterTaskExecutionTimeout),
            SettingDataType.LONG,
            "Per-conversion timeout in milliseconds. Raise for very large documents. " +
                "⚠ Requires application restart.",
            DocxSetting.Category.LIBREOFFICE,
            true
        );

        createIfMissing(
            "jodconverter.local.task-queue-timeout",
            String.valueOf(jodconverterTaskQueueTimeout),
            SettingDataType.LONG,
            "How long a task may wait in the queue before failing (milliseconds). " +
                "⚠ Requires application restart.",
            DocxSetting.Category.LIBREOFFICE,
            true
        );

        log.info("All DOCX settings have been initialized");
    }

    private void createIfMissing(
            String settingKey, String settingValue,
            SettingDataType dataType, String description,
            DocxSetting.Category category, Boolean requiresRestart
    ) {
        try {
            if (docxSettingRepository.existsBySettingKey(settingKey)) {
                log.debug("⊘ DOCX setting already exists, skipping: {}", settingKey);
                return;
            }
            DocxSetting setting = DocxSetting.builder()
                .settingKey(settingKey)
                .settingValue(settingValue)
                .dataType(dataType)
                .description(description)
                .category(category)
                .active(true)
                .isSystemDefault(true)
                .requiresRestart(requiresRestart)
                .build();
            docxSettingRepository.save(setting);
            log.info("DOCX setting initialized: {} = {} (category: {})", settingKey, settingValue, category);
        } catch (Exception e) {
            log.warn("Failed to initialize DOCX setting {}: {}", settingKey, e.getMessage());
        }
    }
}
