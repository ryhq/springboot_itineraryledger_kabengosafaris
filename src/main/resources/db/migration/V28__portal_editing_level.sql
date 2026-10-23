-- The client portal's two company-level settings (2026-10-23).
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

alter table company_profile
    add column portal_editing_level varchar(30) null,
    -- Where customers open their own trip, e.g. https://mytrip.example.com.
    --
    -- On the company record rather than in a server environment file, because it is needed in
    -- two places that are both the company's business: the address printed in the emails, and
    -- the origin a browser is allowed to call this API from. Keeping it here means a company
    -- can point its portal at a new hostname from Settings, with no root access and no restart.
    add column portal_url varchar(255) null;
