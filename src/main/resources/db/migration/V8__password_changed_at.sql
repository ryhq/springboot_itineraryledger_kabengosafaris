-- When each account's password last changed (2026-08-21).
--
-- A password-reset link is a stateless JWT: signature and expiry are the only things checked, so the
-- SAME link kept working for its whole lifetime — repeatedly, and after it had already been used.
-- Anyone who came across it later (a shared inbox, a forwarded message, a proxy log, browser
-- history) could set another new password on an account whose owner had already finished resetting.
--
-- Stamping the change lets a token be judged against it: a link issued before the current password
-- was set is spent. That covers reuse of a used link AND an older link superseded by a newer
-- request, which a used-token table would need extra rows to express.
--
-- Backfilled to the row's last update so existing links do not all die at deploy: a NULL would mean
-- "no password has ever been set", which is not true of anybody.
--
alter table users
    add column password_changed_at datetime(6) null;

update users
set password_changed_at = coalesce(updated_at, created_at)
where password_changed_at is null;
