-- A mailbox is not always on the server that sends for it.
--
-- The Jatelo droplet blocks outbound 465 and 587 (both time out; Brevo works only because it
-- listens on 2525), so that account has to send through a relay and read its mail from Namecheap
-- Private Email. Two servers means two logins, and until now the receiving code signed into IMAP
-- with the SMTP username and password.
--
-- Null in both columns means "use the SMTP credentials", which is what every existing account
-- does today -- Kabengo's cPanel account included.
ALTER TABLE email_accounts
    ADD COLUMN imap_username VARCHAR(255) NULL AFTER imap_use_tls,
    ADD COLUMN imap_password VARCHAR(255) NULL AFTER imap_username;
