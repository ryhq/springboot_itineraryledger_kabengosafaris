-- A lead is captured at hello, not at goodbye.
--
-- The website used to ask ten questions and write nothing until the last click, so everybody
-- who left at question six was lost without trace. The form now saves the moment it has a
-- name and an email, and appends as the visitor answers more. Three things follow from that:
-- a row can exist before it is finished, it needs a handle the browser can come back with,
-- and we need to be able to tell a half-filled lead from a complete one.
--
-- last_name becomes nullable because the first save has only a first name. It stays on the
-- table because most people do give it, and a quotation addressed to a surname reads better.

ALTER TABLE booking_inquiries
  MODIFY COLUMN last_name VARCHAR(100) NULL,
  ADD COLUMN draft_token  CHAR(36)    NULL,
  ADD COLUMN submitted_at DATETIME(6) NULL,
  ADD COLUMN last_step    SMALLINT    NULL,
  ADD COLUMN last_step_at DATETIME(6) NULL;

-- Every row that exists today arrived through the old all-at-once form, so every one of them
-- was finished. Without this the entire history reads as abandoned the moment the panel
-- learns to filter on it.
UPDATE booking_inquiries SET submitted_at = created_at WHERE submitted_at IS NULL;

-- The token is how a browser mid-form finds its own row again. It is looked up on every step,
-- and two leads must never share one.
CREATE UNIQUE INDEX uk_inquiry_draft_token ON booking_inquiries (draft_token);

-- Partial leads are a queue somebody works through, so the column is filtered on, not scanned.
CREATE INDEX idx_inquiry_submitted_at ON booking_inquiries (submitted_at);
