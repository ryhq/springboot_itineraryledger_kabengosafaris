package com.itineraryledger.kabengosafaris.DataTransfer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.PessimisticLockingFailureException;

/**
 * What a failed import says, and that two of them cannot run at once.
 *
 * A preview performs every write and rolls it back, so it holds row locks across the whole bundle
 * while it runs. "Check this bundle" then "Import" therefore had the second request waiting on the
 * first, until MySQL's lock wait timeout killed it — and what the office got was a toast containing
 * a parameterised INSERT printed twice:
 *
 *     The import failed — PessimisticLockingFailureException: could not execute statement [Lock
 *     wait timeout exceeded; try restarting transaction] [insert into park_tariff_rates
 *     (age_category_id,created_at,currency,…) values (?,?,?,?,?,?,?,?,?,?,?,?,?)]; SQL [insert
 *     into park_tariff_rates (age_category_id,created_at,…) values (?,?,?,?,?,?,?,?,?,?,?,?,?)]
 *
 * Every word of which is true and none of which says "wait a moment and try again".
 */
class ImportFailuresReadableTest {

    private PessimisticLockingFailureException theRealFailure() {
        return new PessimisticLockingFailureException(
            "could not execute statement [Lock wait timeout exceeded; try restarting transaction] "
                + "[insert into park_tariff_rates (age_category_id,created_at,currency,is_active,"
                + "nation_category_id,notes,park_id,tariff_id,rack_rate,season_id,sto_rate,"
                + "updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?)]; SQL [insert into park_tariff_rates "
                + "(age_category_id,created_at,currency,is_active,nation_category_id,notes,park_id,"
                + "tariff_id,rack_rate,season_id,sto_rate,updated_at) values (?,?,?,?,?,?,?,?,?,?,?,?)]",
            new SQLException("Lock wait timeout exceeded; try restarting transaction"));
    }

    @Test
    @DisplayName("a lock timeout is recognised as contention, whatever it is wrapped in")
    void contentionIsRecognised() {
        assertTrue(DataTransferController.isLockContention(theRealFailure()));
        assertTrue(DataTransferController.isLockContention(
            new RuntimeException("wrapped", theRealFailure())),
            "the exception the caller sees is usually two layers up");
        assertTrue(DataTransferController.isLockContention(
            new RuntimeException("Deadlock found when trying to get lock")));
    }

    @Test
    @DisplayName("a genuine bundle fault is not mistaken for contention")
    void otherFailuresAreNotContention() {
        assertFalse(DataTransferController.isLockContention(
            new IllegalStateException("Could not apply rackRate to ParkTariffRate")));
        assertFalse(DataTransferController.isLockContention(new IOException("truncated zip")));
    }

    @Test
    @DisplayName("a failure is described in one line, without the statement")
    void theStatementIsLeftOut() {
        String described = DataTransferController.describe(theRealFailure());

        assertTrue(described.contains("PessimisticLockingFailureException"),
            "the class is the part somebody can quote: " + described);
        assertFalse(described.contains("insert into park_tariff_rates"),
            "the SQL belongs in the log, not the toast: " + described);
        assertFalse(described.contains("?,?"), "no bind-parameter runs: " + described);
        assertTrue(described.length() < 320, "one line, not a screenful: " + described.length());
    }

    @Test
    @DisplayName("the detail that made failures diagnosable at all is still there")
    void detailSurvives() {
        /*
         * An earlier version answered 500 with nothing but "the import failed", which cost an
         * afternoon. Trimming the SQL must not walk that back.
         */
        String described = DataTransferController.describe(
            new IllegalStateException("Could not apply rackRate to ParkTariffRate at "
                + "ParkTariffRate[\"season\"]: Cannot construct instance of Season"));

        assertTrue(described.contains("ParkTariffRate"), described);
        assertTrue(described.contains("season"), described);
    }

    @Test
    @DisplayName("import and preview are serialised, since each locks the whole bundle")
    void oneBundleAtATime() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/DataTransfer/DataTransferController.java"));

        assertTrue(source.contains("ONE_BUNDLE_AT_A_TIME"),
            "both endpoints must pass through one permit, or a preview and an import overlap and "
                + "fight over the same rows");
        assertTrue(source.contains("tryAcquire()"), "the second caller is told to wait, not queued");
        assertTrue(source.contains("ONE_BUNDLE_AT_A_TIME.release()")
            && source.contains("} finally {"),
            "the permit must be released on every path, or one failure blocks every later import");
    }
}
