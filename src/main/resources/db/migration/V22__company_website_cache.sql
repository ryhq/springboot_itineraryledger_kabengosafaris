-- Where the public website lives, and the key that lets this API empty its cache.
--
-- The website renders itself from this API and then KEEPS the answer: a trip page for five
-- minutes, most other pages for an hour. So an edit made in the panel is not visible to a
-- visitor until that timer runs out, and the office has no way to say "no, now".
--
-- The website therefore accepts a POST that throws pages away. It must not accept it from
-- anybody: an open endpoint that forces a re-render is a way to make the site rebuild itself
-- continuously, which is a denial of service with extra steps. So the website is handed a
-- secret, and the same secret is recorded here.
--
-- The secret is stored ENCRYPTED (the same AES helper the mail passwords use) and is never
-- returned by any endpoint — the API answers whether one is set, never what it is. Rotating it
-- means pasting a new one here and into the website's environment; there is no recovery path
-- for a value nobody wrote down, which is the correct behaviour for a credential.
--
-- The three "last call" columns exist because Next.js will not tell anyone what it is holding.
-- They are not a picture of the cache; they are a record of what this API asked for and whether
-- anybody answered, which is the question people actually have ("is what I am looking at
-- current?"). Null everywhere means nothing has ever been cleared.

ALTER TABLE company_profile
    ADD COLUMN website_url                 VARCHAR(300) NULL AFTER brand_font,
    ADD COLUMN website_cache_secret        VARCHAR(512) NULL AFTER website_url,
    ADD COLUMN website_cache_auto          TINYINT(1)   NOT NULL DEFAULT 1 AFTER website_cache_secret,
    ADD COLUMN website_cache_last_called_at DATETIME    NULL AFTER website_cache_auto,
    ADD COLUMN website_cache_last_ok       TINYINT(1)   NULL AFTER website_cache_last_called_at,
    ADD COLUMN website_cache_last_detail   VARCHAR(500) NULL AFTER website_cache_last_ok;
