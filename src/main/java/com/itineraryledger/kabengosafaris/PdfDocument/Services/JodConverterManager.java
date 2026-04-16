package com.itineraryledger.kabengosafaris.PdfDocument.Services;

import com.itineraryledger.kabengosafaris.PdfDocument.Settings.DocxSettingGetterServices;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFamily;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.document.DocumentFormatRegistry;
import org.jodconverter.core.document.JsonDocumentFormatRegistry;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * JodConverterManager — owns the lifecycle of the headless LibreOffice pool
 * so {@code jodconverter.local.*} settings can be reloaded at runtime without
 * restarting the whole Spring Boot app.
 *
 * Concurrency model:
 *  - Reads (conversions) acquire the read lock via {@link #withConverter(Function)}.
 *    Multiple conversions can run concurrently.
 *  - {@link #reload()} acquires the write lock, waits for any in-flight
 *    conversions to complete, stops the old manager, starts a fresh one
 *    from the latest DB settings, and releases.
 *
 * Failure handling:
 *  - If the pool fails to start (e.g. LibreOffice not installed) the manager
 *    stays in FAILED state with the error message on {@link #getLastError()}.
 *  - {@link #isAvailable()} reports whether the pool is currently serving
 *    requests. Callers are expected to fall back to docx4j when it isn't.
 */
@Component
@Slf4j
public class JodConverterManager {

    public enum Status {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        FAILED
    }

    private final DocxSettingGetterServices settings;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile OfficeManager officeManager;
    private volatile DocumentConverter converter;

    @Getter private volatile Status status = Status.STOPPED;
    @Getter private volatile String lastError;
    @Getter private volatile LocalDateTime startedAt;
    @Getter private volatile LocalDateTime lastReloadedAt;

    // Snapshot of the settings that built the current running pool — surfaced
    // via the status endpoint so admins can tell what config is actually live.
    @Getter private volatile String activeOfficeHome;
    @Getter private volatile String activePortNumbers;
    @Getter private volatile Integer activeMaxTasksPerProcess;
    @Getter private volatile Long activeTaskExecutionTimeout;
    @Getter private volatile Long activeTaskQueueTimeout;

    public JodConverterManager(DocxSettingGetterServices settings) {
        this.settings = settings;
    }

    @PostConstruct
    public void init() {
        if (Boolean.TRUE.equals(settings.isJodconverterEnabled())) {
            log.info("JodConverterManager: autostarting LibreOffice pool (jodconverter.local.enabled=true)");
            safeStart();
        } else {
            log.info("JodConverterManager: LibreOffice pool disabled (jodconverter.local.enabled=false)");
            status = Status.STOPPED;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("JodConverterManager: shutting down LibreOffice pool");
        safeStop();
    }

    /**
     * @return true iff the pool is currently running and ready to serve conversions.
     */
    public boolean isAvailable() {
        return status == Status.RUNNING && converter != null;
    }

    /**
     * Run a conversion with the current {@link DocumentConverter} under a read lock.
     * Multiple conversions can execute concurrently; a simultaneous {@link #reload()}
     * will wait until this returns.
     *
     * @throws IllegalStateException if the pool is not running.
     */
    public <T> T withConverter(Function<DocumentConverter, T> fn) {
        lock.readLock().lock();
        try {
            if (status != Status.RUNNING || converter == null) {
                throw new IllegalStateException(
                    "LibreOffice pool is not running (status=" + status + ")");
            }
            return fn.apply(converter);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Stop the current pool (if any) and start a fresh one with the latest
     * settings from the DB. Blocks until in-flight conversions finish.
     *
     * @return the status snapshot after reload.
     */
    public ReloadResult reload() {
        log.info("JodConverterManager: reload requested");
        lock.writeLock().lock();
        try {
            safeStopInternal();
            if (!Boolean.TRUE.equals(settings.isJodconverterEnabled())) {
                status = Status.STOPPED;
                lastError = null;
                lastReloadedAt = LocalDateTime.now();
                log.info("JodConverterManager: reload complete — pool intentionally stopped (jodconverter.local.enabled=false)");
                return snapshot("LibreOffice pool is disabled. Enable 'jodconverter.local.enabled' and reload to start.");
            }
            try {
                startInternal();
                lastReloadedAt = LocalDateTime.now();
                log.info("JodConverterManager: reload complete — pool is RUNNING");
                return snapshot("LibreOffice pool reloaded and running.");
            } catch (Exception e) {
                status = Status.FAILED;
                lastError = e.getMessage();
                log.error("JodConverterManager: reload failed to start pool", e);
                return snapshot("LibreOffice pool failed to start: " + e.getMessage());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Stop the pool without restarting it (equivalent to flipping
     * jodconverter.local.enabled=false and reloading).
     */
    public ReloadResult stop() {
        log.info("JodConverterManager: stop requested");
        lock.writeLock().lock();
        try {
            safeStopInternal();
            status = Status.STOPPED;
            lastError = null;
            lastReloadedAt = LocalDateTime.now();
            return snapshot("LibreOffice pool stopped.");
        } finally {
            lock.writeLock().unlock();
        }
    }

    public ReloadResult snapshot(String message) {
        return new ReloadResult(
            status,
            Boolean.TRUE.equals(settings.isJodconverterEnabled()),
            activeOfficeHome,
            activePortNumbers,
            activeMaxTasksPerProcess,
            activeTaskExecutionTimeout,
            activeTaskQueueTimeout,
            startedAt,
            lastReloadedAt,
            lastError,
            message
        );
    }

    // =====================================================================
    // Internals
    // =====================================================================

    private void safeStart() {
        lock.writeLock().lock();
        try {
            startInternal();
        } catch (Exception e) {
            status = Status.FAILED;
            lastError = e.getMessage();
            log.error("JodConverterManager: failed to start LibreOffice pool", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void startInternal() throws Exception {
        status = Status.STARTING;
        lastError = null;

        String officeHome = settings.getJodconverterOfficeHome();
        String portsStr = settings.getJodconverterPortNumbers();
        Integer maxTasks = settings.getJodconverterMaxTasksPerProcess();
        Long execTimeout = settings.getJodconverterTaskExecutionTimeout();
        Long queueTimeout = settings.getJodconverterTaskQueueTimeout();

        int[] ports = parsePorts(portsStr);

        LocalOfficeManager.Builder builder = LocalOfficeManager.builder()
            .portNumbers(ports)
            .maxTasksPerProcess(maxTasks != null ? maxTasks : 100)
            .taskExecutionTimeout(execTimeout != null ? execTimeout : 120_000L)
            .taskQueueTimeout(queueTimeout != null ? queueTimeout : 30_000L);

        if (officeHome != null && !officeHome.isBlank()) {
            builder.officeHome(officeHome);
        }

        OfficeManager mgr = builder.build();
        mgr.start();

        this.officeManager = mgr;
        this.converter = LocalConverter.builder()
            .officeManager(mgr)
            .formatRegistry(buildFormatRegistry())
            .build();
        this.activeOfficeHome = officeHome;
        this.activePortNumbers = portsStr;
        this.activeMaxTasksPerProcess = maxTasks;
        this.activeTaskExecutionTimeout = execTimeout;
        this.activeTaskQueueTimeout = queueTimeout;
        this.startedAt = LocalDateTime.now();
        this.status = Status.RUNNING;

        log.info("JodConverterManager started: officeHome={}, ports={}, maxTasks={}, execTimeout={}ms, queueTimeout={}ms",
            officeHome == null || officeHome.isBlank() ? "<autodiscover>" : officeHome,
            portsStr, maxTasks, execTimeout, queueTimeout);
    }

    private void safeStopInternal() {
        if (officeManager == null) {
            return;
        }
        status = Status.STOPPING;
        try {
            officeManager.stop();
            log.info("JodConverterManager stopped LibreOffice pool cleanly");
        } catch (OfficeException e) {
            log.warn("JodConverterManager: non-fatal error stopping pool", e);
        } catch (Exception e) {
            log.warn("JodConverterManager: unexpected error stopping pool", e);
        } finally {
            officeManager = null;
            converter = null;
            activeOfficeHome = null;
            activePortNumbers = null;
            activeMaxTasksPerProcess = null;
            activeTaskExecutionTimeout = null;
            activeTaskQueueTimeout = null;
            startedAt = null;
        }
    }

    private void safeStop() {
        lock.writeLock().lock();
        try {
            safeStopInternal();
            status = Status.STOPPED;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Build a format registry based on the defaults but with the DOCX format
     * extended to support WEB → DOCX conversion.
     *
     * JODConverter's default DOCX format only has store properties for the TEXT
     * document family. LibreOffice loads HTML files as WebDocuments (WEB family),
     * so {@code docxFormat.getStoreProperties(WEB)} returns null and the
     * conversion is rejected with "Unsupported conversion". Adding the WEB
     * store filter here fixes HTML → DOCX without affecting other conversions.
     */
    private DocumentFormatRegistry buildFormatRegistry() {
        try (InputStream is = getClass().getResourceAsStream("/document-formats.json")) {
            JsonDocumentFormatRegistry registry = JsonDocumentFormatRegistry.create(is);

            // Extend DOCX: add the "MS Word 2007 XML" store filter for WEB family
            DocumentFormat docxWithWeb = DocumentFormat.builder()
                .from(DefaultDocumentFormatRegistry.DOCX)
                .storeFilterName(DocumentFamily.WEB, "MS Word 2007 XML")
                .build();
            registry.addFormat(docxWithWeb);

            log.debug("Custom format registry built: DOCX now supports WEB → DOCX conversion");
            return registry;
        } catch (Exception e) {
            log.warn("Failed to build custom format registry, falling back to defaults. " +
                "HTML → DOCX conversion may fail.", e);
            return DefaultDocumentFormatRegistry.getInstance();
        }
    }

    private static int[] parsePorts(String portsCsv) {
        if (portsCsv == null || portsCsv.isBlank()) {
            return new int[] { 2002 };
        }
        return Arrays.stream(portsCsv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .mapToInt(Integer::parseInt)
            .toArray();
    }

    /**
     * Status snapshot returned from {@link #reload()}, {@link #stop()}, and
     * exposed by the /api/docx-settings/status/libreoffice endpoint.
     */
    public record ReloadResult(
        Status status,
        boolean enabled,
        String officeHome,
        String portNumbers,
        Integer maxTasksPerProcess,
        Long taskExecutionTimeout,
        Long taskQueueTimeout,
        LocalDateTime startedAt,
        LocalDateTime lastReloadedAt,
        String lastError,
        String message
    ) {}
}
