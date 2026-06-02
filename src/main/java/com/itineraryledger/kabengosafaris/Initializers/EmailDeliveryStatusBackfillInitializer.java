package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * One-time back-fill so emails sent before the {@code resend_email_id} +
 * {@code delivery_status} columns existed get their status populated from
 * the Resend webhook events that have already been received.
 *
 * <p>Two passes, both idempotent:
 * <ol>
 *   <li>Copy the bare UUID out of {@code message_id} (stored as
 *       {@code &lt;uuid&gt;}) into {@code resend_email_id} on every row
 *       still missing it.</li>
 *   <li>For every row that now has a {@code resend_email_id} but no
 *       {@code delivery_status}, join against {@code resend_webhook_events}
 *       and derive the row's status from the strongest event we've seen.
 *       Priority: complained &gt; bounced &gt; delivered &gt; delivery_delayed
 *       &gt; sent, mirroring the live webhook handler.</li>
 * </ol>
 *
 * <p>Re-running this is safe — pass 1 only touches null columns, pass 2
 * only touches rows that still have no {@code delivery_status}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailDeliveryStatusBackfillInitializer implements ApplicationRunner, Ordered {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int getOrder() {
        // Run after schema migrations (the column has to exist), well before
        // anything that might read delivery status.
        return -50;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            backfillResendEmailId();
            backfillDeliveryStatus();
        } catch (Exception e) {
            // Never block startup over a back-fill — log and move on.
            log.warn("Email delivery status back-fill skipped: {}", e.getMessage());
        }
    }

    /**
     * Pass 1 — copy the embedded resend email id out of the legacy wrapped
     * messageId. Only operates on rows with the exact {@code &lt;uuid&gt;}
     * shape, leaving regular RFC-2822 IDs untouched.
     */
    private void backfillResendEmailId() {
        int updated = jdbcTemplate.update(
                "UPDATE email_messages " +
                "SET resend_email_id = SUBSTRING(message_id, 2, CHAR_LENGTH(message_id) - 2) " +
                "WHERE resend_email_id IS NULL " +
                "  AND message_id LIKE '<%>' " +
                "  AND message_id REGEXP '^<[0-9a-fA-F-]{36}>$'");
        if (updated > 0) {
            log.info("Back-filled resend_email_id on {} email_messages row(s)", updated);
        }
    }

    /**
     * Pass 2 — derive {@code delivery_status} + the matching timestamp
     * columns from the strongest event seen in {@code resend_webhook_events}
     * for each message.
     */
    private void backfillDeliveryStatus() {
        // One UPDATE per terminal state, strongest first; the WHERE clause
        // on delivery_status IS NULL keeps each pass from clobbering an
        // earlier-set stronger state.
        updateFromEvents("COMPLAINED",      "complained_at", "email.complained");
        updateFromEvents("BOUNCED",         "bounced_at",    "email.bounced");
        updateFromEvents("DELIVERED",       "delivered_at",  "email.delivered");
        updateFromEvents("DELIVERY_DELAYED", null,           "email.delivery_delayed");
        updateFromEvents("SENT",            null,            "email.sent");

        // Anything that has a resend_email_id (so we know it's outgoing)
        // but no event has been recorded yet should at least show "SENT".
        int defaulted = jdbcTemplate.update(
                "UPDATE email_messages " +
                "SET delivery_status = 'SENT' " +
                "WHERE delivery_status IS NULL " +
                "  AND resend_email_id IS NOT NULL");
        if (defaulted > 0) {
            log.info("Defaulted delivery_status=SENT on {} outgoing email_messages row(s) with no webhook events yet", defaulted);
        }
    }

    private void updateFromEvents(String status, String tsColumn, String eventType) {
        String sql;
        if (tsColumn != null) {
            sql = "UPDATE email_messages em " +
                  "JOIN (SELECT email_id, MAX(event_timestamp) AS ts " +
                  "      FROM resend_webhook_events " +
                  "      WHERE event_type = ? " +
                  "      GROUP BY email_id) e " +
                  "  ON e.email_id = em.resend_email_id " +
                  "SET em.delivery_status = ?, em." + tsColumn + " = e.ts, em.last_event_type = ? " +
                  "WHERE em.delivery_status IS NULL " +
                  "  AND em.resend_email_id IS NOT NULL";
            int updated = jdbcTemplate.update(sql, eventType, status, eventType);
            if (updated > 0) log.info("Back-filled delivery_status={} on {} row(s)", status, updated);
        } else {
            sql = "UPDATE email_messages em " +
                  "JOIN (SELECT DISTINCT email_id FROM resend_webhook_events WHERE event_type = ?) e " +
                  "  ON e.email_id = em.resend_email_id " +
                  "SET em.delivery_status = ?, em.last_event_type = ? " +
                  "WHERE em.delivery_status IS NULL " +
                  "  AND em.resend_email_id IS NOT NULL";
            int updated = jdbcTemplate.update(sql, eventType, status, eventType);
            if (updated > 0) log.info("Back-filled delivery_status={} on {} row(s)", status, updated);
        }
    }
}
