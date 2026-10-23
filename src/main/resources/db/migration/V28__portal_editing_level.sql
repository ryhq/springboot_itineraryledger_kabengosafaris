-- How far a client may reach into their own quotation, as a company-wide default (2026-10-23).
--
-- The level is resolved most-specific-first: the journey's own setting, then this. A journey
-- with nothing set inherits whatever the company has decided, so turning the portal on for a
-- company does not require deciding the question for every booking one at a time.
--
-- Varchar, not a native enum, for the reason V27 gives: every native enum in this schema is
-- frozen at the values it was created with, ddl-auto does not widen one, and adding a Java
-- constant to such a column has failed at commit five times here.
--
-- Null means the shipped default, which is CHOOSE_OFFERED: swap between the alternatives already
-- on the quotation. That is the level that exposes nothing a client has not already been shown.

alter table company_profiles
    add column portal_editing_level varchar(30) null;
