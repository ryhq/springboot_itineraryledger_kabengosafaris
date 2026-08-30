package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * That the export writes its response inside the controller, where its failures can be reported.
 *
 * A StreamingResponseBody runs on an async dispatch AFTER the controller returns, so anything that
 * goes wrong there lands in the global exception handler instead of the catch block written for it.
 * That is not theoretical: it cost three rounds on a live 500 whose message was always somebody's
 * generic sentence, because the specific one could not reach the response.
 *
 * Bounded memory was the reason for streaming, and that is still had — the bundle is a file on disk
 * and Spring copies a Resource through a buffer. The async part bought nothing and hid everything.
 */
class ExportStaysSynchronousTest {

    private static final Path CONTROLLER = Path.of("src/main/java/com/itineraryledger/"
        + "kabengosafaris/DataTransfer/DataTransferController.java");

    /**
     * The file with its comments removed.
     *
     * Because the first version of this test failed on the comment EXPLAINING why the streaming body
     * was removed — a check that cannot tell code from prose reports the fix as the fault.
     */
    private String codeOf(Path file) throws IOException {
        return Files.readString(file)
            .replaceAll("(?s)/\\*.*?\\*/", "")
            .replaceAll("(?m)//.*$", "");
    }

    @Test
    @DisplayName("the export response is a Resource, not an async streaming body")
    void noAsyncDispatch() throws IOException {
        String source = codeOf(CONTROLLER);

        assertFalse(source.contains("StreamingResponseBody"),
            "A streaming body escapes this controller's own error handling. Return an "
                + "InputStreamResource over the built file instead — same memory, reportable faults.");
        assertTrue(source.contains("InputStreamResource"),
            "the bundle should be sent as a Resource");
    }

    @Test
    @DisplayName("a failure says what kind of failure it was")
    void faultsNameThemselves() throws IOException {
        String source = codeOf(CONTROLLER);

        assertTrue(source.contains("e.getClass().getSimpleName()"),
            "e.getMessage() is null for plenty of exceptions — a 500 that says only 'could not' "
                + "costs a deploy to diagnose");
        assertTrue(source.contains("catch (Throwable e)"),
            "an Error here is as invisible as an Exception, and just as worth reporting");
    }

    @Test
    @DisplayName("the bundle is built on disk, not in a RAM-backed tmpfs")
    void builtOnDisk() throws IOException {
        String service = codeOf(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/DataTransfer/Services/BundleExportService.java"));

        assertTrue(service.contains("app.data.dir"),
            "the service runs with PrivateTmp, where /tmp is memory — building a company's galleries "
                + "there is the byte[] problem wearing a different hat");
    }
}
