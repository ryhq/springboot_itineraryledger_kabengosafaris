-- An email account that sends through an API has no SMTP server (2026-11-10).
--
-- EmailAccountCreateService has always had two branches for a Resend account. The SMTP branch
-- fills in smtp.resend.com, the username "resend" and the API key as the password. The API branch
-- sets every SMTP field to null on purpose, with the comment "API sending: no SMTP fields needed",
-- and the entity agrees: EmailAccount declares smtpHost with no nullability constraint and the
-- comment "Nullable for API-based providers (Resend, SendGrid)". V1 of this schema agrees too --
-- it writes `smtp_host varchar(255)`, with no `not null`.
--
-- The live column is `not null` regardless, because V1 is a BASELINE. spring.flyway.baseline-on-
-- migrate is true and baseline-version is 1, so V1 was stamped onto an existing database rather
-- than executed against it, and the column kept whatever ddl-auto first gave it. ddl-auto=update,
-- which production runs, only ever ADDS things: it will create a column or an index, and it will
-- never drop a constraint. So a constraint that stopped being true in the Java could sit in the
-- database indefinitely with nothing reporting the disagreement.
--
-- The effect was that creating a Resend account through POST /api/email-accounts was impossible.
-- Hibernate raised `Column 'smtp_host' cannot be null`, the controller turned it into a bare
-- 500 "Failed to create email account", and the only description of the real cause was a stack
-- trace in the application log. The one Resend account that existed had been created long before,
-- while the SMTP fields were still being filled, which is why nobody met this until a second and
-- third mailbox were added on 2026-11-10.
--
-- All six SMTP columns are relaxed, not just the one Hibernate happened to name first: MySQL
-- reports the first violation and stops, so fixing only smtp_host would have produced the same
-- 500 from smtp_username on the next attempt. Every one of them is nullable in both the entity
-- and V1, so this aligns the database with what the code has believed for months.
--
-- Note for whoever touches this next: columns that are null for a real row need null-safe
-- comparisons. EmailAccountUpdateService compared them with existing.getX().equals(...), which
-- would NPE on the first API-created account; that is fixed in the same change as this migration.
--
alter table email_accounts
    modify column smtp_host varchar(255) null,
    modify column smtp_username varchar(255) null,
    modify column smtp_password varchar(255) null,
    modify column smtp_port integer null,
    modify column use_tls bit null,
    modify column use_ssl bit null;
