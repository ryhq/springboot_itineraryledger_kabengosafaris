package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity storing DOCX generation + JODConverter/LibreOffice settings in the DB.
 *
 * Mirrors the TranslationSetting pattern: key-value rows seeded by an initializer
 * from application.properties, then editable at runtime without restart.
 *
 * Settings registered here:
 *   docx.engine                                   (ENGINE)
 *   jodconverter.local.enabled                    (LIBREOFFICE, requires restart)
 *   jodconverter.local.office-home                (LIBREOFFICE, requires restart)
 *   jodconverter.local.port-numbers               (LIBREOFFICE, requires restart)
 *   jodconverter.local.max-tasks-per-process      (LIBREOFFICE, requires restart)
 *   jodconverter.local.task-execution-timeout     (LIBREOFFICE, requires restart)
 *   jodconverter.local.task-queue-timeout         (LIBREOFFICE, requires restart)
 */
@Entity
@Table(name = "docx_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = "setting_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocxSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Setting key (e.g., 'docx.engine', 'jodconverter.local.enabled'). */
    @Column(nullable = false, length = 150)
    private String settingKey;

    /** Setting value (stored as string, parsed based on dataType). */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String settingValue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SettingDataType dataType;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    /** Marks seeded defaults — cannot be deleted, can be reset. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystemDefault = false;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Category category;

    /**
     * True for jodconverter.local.* settings whose values are baked into the
     * OfficeManager Spring bean at startup — changes need an app restart to
     * take effect. The router for {@code docx.engine} re-reads on every
     * request so it's {@code false}.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean requiresRestart = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /** Categories for organizing DOCX settings. */
    public enum Category {
        ENGINE("DOCX Engine Selection"),
        LIBREOFFICE("JODConverter / LibreOffice Pool");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
