package com.itineraryledger.kabengosafaris.Log.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.itineraryledger.kabengosafaris.Log.DTOs.AccessLogDTO;
import com.itineraryledger.kabengosafaris.Log.Services.AccessLogParserService;
import com.itineraryledger.kabengosafaris.Log.Services.AccessLogSettingGetterServices;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Controller for real-time access log streaming using Server-Sent Events (SSE)
 *
 * Features:
 * - Real-time log tailing with automatic updates
 * - Heartbeat mechanism to keep connection alive
 * - Multiple concurrent clients support
 * - Automatic cleanup on client disconnect
 *
 * Permissions:
 * - Requires PERM_READ_LOG permission
 */
@RestController
@RequestMapping("/api/logs/stream")
@Slf4j
public class LogSSEController {

    @Autowired
    private AccessLogParserService parserService;

    @Autowired
    private IdObfuscator idObfuscator;

    @Autowired
    private AccessLogSettingGetterServices settings;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final long HEARTBEAT_INTERVAL = 15000; // 15 seconds
    private static final long POLL_INTERVAL = 1000; // 1 second

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService logTailExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean streamingActive = false;
    private volatile long lastFilePosition = 0;
    private volatile AtomicLong logCounter = new AtomicLong(0);

    /**
     * Stream access logs in real-time
     *
     * @return SseEmitter for server-sent events
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> {
            emitters.remove(emitter);
            log.info("SSE client disconnected. Active connections: {}", emitters.size());
            if (emitters.isEmpty()) {
                stopStreaming();
            }
        });

        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(emitter);
            log.warn("SSE connection timed out. Active connections: {}", emitters.size());
            if (emitters.isEmpty()) {
                stopStreaming();
            }
        });

        emitter.onError((ex) -> {
            emitters.remove(emitter);
            log.error("SSE error occurred. Active connections: {}", emitters.size(), ex);
            if (emitters.isEmpty()) {
                stopStreaming();
            }
        });

        emitters.add(emitter);
        log.info("New SSE client connected. Active connections: {}", emitters.size());

        // Start streaming if this is the first client
        if (!streamingActive) {
            startStreaming();
        }

        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected to access log stream"));
        } catch (IOException e) {
            log.error("Error sending initial message", e);
            emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * Start log streaming and heartbeat
     */
    private synchronized void startStreaming() {
        if (streamingActive) {
            return;
        }

        streamingActive = true;
        log.info("Starting log streaming");

        // Initialize file position to end of current log file
        Path logFilePath = resolveLogFilePath(LocalDate.now());
        try {
            if (Files.exists(logFilePath)) {
                lastFilePosition = Files.size(logFilePath);
                log.info("Initialized log position to: {} bytes", lastFilePosition);
            }
        } catch (IOException e) {
            log.error("Error initializing log position", e);
        }

        // Start heartbeat
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

        // Start log tailing
        logTailExecutor.scheduleAtFixedRate(this::tailLogFile, 0, POLL_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop log streaming and heartbeat
     */
    private synchronized void stopStreaming() {
        if (!streamingActive) {
            return;
        }

        streamingActive = false;
        log.info("Stopping log streaming");

        heartbeatExecutor.shutdown();
        logTailExecutor.shutdown();
    }

    /**
     * Send heartbeat to all connected clients
     */
    private void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("ping"));
            } catch (IOException e) {
                log.warn("Error sending heartbeat, removing emitter", e);
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Tail log file and send new entries to all connected clients
     */
    private void tailLogFile() {
        if (emitters.isEmpty()) {
            return;
        }

        Path logFilePath = resolveLogFilePath(LocalDate.now());

        if (!Files.exists(logFilePath)) {
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(logFilePath.toFile(), "r")) {
            long fileLength = file.length();

            // Check if file was rotated (new day started)
            if (fileLength < lastFilePosition) {
                log.info("Log file rotated, resetting position");
                lastFilePosition = 0;
                logCounter.set(0);
            }

            // Check if there's new content
            if (fileLength > lastFilePosition) {
                file.seek(lastFilePosition);

                String line;
                while ((line = file.readLine()) != null) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }

                    // Parse and enrich log entry
                    long id = logCounter.incrementAndGet();
                    String encodedId = idObfuscator.encodeId(id);
                    AccessLogDTO dto = parserService.parse(line, encodedId);

                    // Send to all connected clients
                    sendLogEntry(dto);
                }

                lastFilePosition = file.getFilePointer();
            }

        } catch (IOException e) {
            log.error("Error tailing log file", e);
        }
    }

    /**
     * Send log entry to all connected clients
     */
    private void sendLogEntry(AccessLogDTO dto) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name("log")
                    .data(dto));
            } catch (IOException e) {
                log.warn("Error sending log entry, removing emitter", e);
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Resolve log file path for given date
     */
    private Path resolveLogFilePath(LocalDate date) {
        String directory = settings.getLogDirectory();
        String prefix = settings.getLogPrefix();
        String suffix = settings.getLogSuffix();

        String filename = prefix + "." + date.format(DATE_FORMATTER) + suffix;
        return Paths.get(directory, filename);
    }

    /**
     * Get current streaming status (for monitoring)
     */
    @GetMapping("/status")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<StreamStatus>> getStatus() {
        StreamStatus status = new StreamStatus(
            streamingActive,
            emitters.size(),
            lastFilePosition,
            logCounter.get()
        );

        return ResponseEntity.ok(
            ApiResponse.success(200, "Stream status retrieved successfully", status)
        );
    }

    /**
     * Disconnect all active SSE connections
     *
     * This endpoint allows administrators to force-close all active log streaming connections.
     * Useful for maintenance, emergency shutdown, or clearing stale connections.
     *
     * @return Response with number of connections closed
     */
    @DeleteMapping("/disconnect")
    @PreAuthorize("hasAuthority('PERM_READ_LOG')")
    public ResponseEntity<ApiResponse<DisconnectResponse>> disconnectAll() {
        int activeConnections = emitters.size();

        if (activeConnections == 0) {
            DisconnectResponse response = new DisconnectResponse(
                0,
                "No active connections to disconnect"
            );
            return ResponseEntity.ok(
                ApiResponse.success(200, "No active connections found", response)
            );
        }

        // Complete all emitters
        for (SseEmitter emitter : emitters) {
            try {
                emitter.complete();
            } catch (Exception e) {
                log.warn("Error completing emitter during disconnect", e);
            }
        }

        // Clear the list
        emitters.clear();

        // Stop streaming
        stopStreaming();

        DisconnectResponse response = new DisconnectResponse(
            activeConnections,
            "All connections successfully closed"
        );

        log.info("Disconnected {} SSE connection(s) via API", activeConnections);

        return ResponseEntity.ok(
            ApiResponse.success(200, "All connections disconnected successfully", response)
        );
    }

    /**
     * Status response DTO
     */
    public record StreamStatus(
        boolean streamingActive,
        int activeConnections,
        long lastFilePosition,
        long logsProcessed
    ) {}

    /**
     * Disconnect response DTO
     */
    public record DisconnectResponse(
        int disconnectedConnections,
        String message
    ) {}
}
