-- The rows that said "Delivered" about mail that bounced (2026-11-10).
--
-- Companion to the change that stopped ResendWebhookService discarding a bounce when the row
-- already held DELIVERED. That fix is forward-only: it governs events arriving from now on and
-- says nothing about the rows already written. On Kabengo that was 375 bounce events against
-- just 3 rows reading BOUNCED, because a send to several recipients produces one row and several
-- outcomes, and the delivery almost always landed first.
--
-- The damage is not the wrong word on a screen. 363 of those bounces are one address that has
-- soft-bounced every message sent to it since March -- supplier availability replies, a flight
-- inquiry -- while every one of those rows reported Delivered. Nobody chases a message marked
-- delivered. Leaving the history uncorrected would leave eight months of mail looking like it
-- arrived.
--
-- Precedence matches EmailDeliveryStatus.resolve exactly, so the history and the live handler
-- cannot disagree: BOUNCED outranks DELIVERED, DELIVERY_DELAYED and SENT, and loses only to
-- COMPLAINED. COMPLAINED rows are therefore excluded rather than merely unmatched.
--
-- delivered_at is deliberately NOT cleared. A multi-recipient send genuinely was delivered to
-- somebody and bounced for somebody else, and a row carrying both timestamps is the honest
-- record of that; the single status column reports the half that needs acting on. For the same
-- reason bounced_at is only filled where it is missing -- an existing value was written by the
-- live handler from the real event and is not improved by being recomputed.
--
-- last_event_type is left untouched. It records which event arrived last, which is a different
-- question from which outcome matters, and this migration has no way to know the answer for
-- rows whose events it is reading in aggregate.
--
-- Statement 1 repeats the resend_email_id back-fill that EmailDeliveryStatusBackfillInitializer
-- performs, because Flyway runs before ApplicationRunner: without it, any row still carrying the
-- legacy <uuid> message_id would have no join key and would be silently skipped on this one pass
-- it gets. It touches only NULL columns, exactly as the initializer does.
--
UPDATE email_messages
SET resend_email_id = SUBSTRING(message_id, 2, CHAR_LENGTH(message_id) - 2)
WHERE resend_email_id IS NULL
  AND message_id LIKE '<%>'
  AND message_id REGEXP '^<[0-9a-fA-F-]{36}>$';

UPDATE email_messages em
JOIN (SELECT email_id, MAX(event_timestamp) AS ts
      FROM resend_webhook_events
      WHERE event_type = 'email.bounced'
      GROUP BY email_id) e
  ON e.email_id = em.resend_email_id
SET em.delivery_status = 'BOUNCED',
    em.bounced_at = COALESCE(em.bounced_at, e.ts)
WHERE em.delivery_status IN ('DELIVERED', 'DELIVERY_DELAYED', 'SENT', 'BOUNCED');
