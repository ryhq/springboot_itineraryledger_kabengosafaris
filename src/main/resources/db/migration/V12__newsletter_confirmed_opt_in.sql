-- Nothing is sent to an address until somebody proves they own it.
--
-- A newsletter form on a public page will be filled in with other people's addresses, by mistake
-- and on purpose. Sending to an unconfirmed list collects bounces and spam complaints, and the
-- sending reputation that costs is shared with every quote and invoice the company emails.
--
-- One token serves both the confirm link and the unsubscribe link. Unsubscribing by plain email
-- address lets anybody remove anybody, and a confirmation that can be guessed confirms nothing.
--
-- Existing rows are left ACTIVE with a null token: they subscribed under the old rules and it
-- would be wrong to silently stop writing to people who have been on the list for months. They
-- get a token the first time one is needed.
ALTER TABLE newsletter_subscriptions
    ADD COLUMN confirm_token VARCHAR(64) NULL AFTER unsubscribed_at,
    ADD COLUMN confirmed_at DATETIME(6) NULL AFTER confirm_token;

ALTER TABLE newsletter_subscriptions
    ADD CONSTRAINT uk_newsletter_confirm_token UNIQUE (confirm_token);
