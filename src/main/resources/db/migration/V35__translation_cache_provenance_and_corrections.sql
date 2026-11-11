-- Who produced a cached translation, and whether a person has since fixed it (2026-11-11).
--
-- Kabengo switched its default translation account from LibreTranslate to DeepL on 2026-11-11.
-- That changed nothing anybody can see, because the cache is consulted BEFORE the provider and
-- it holds 29,390 entries at a 99.47% hit rate. Every one of those strings still serves the old
-- engine's output, and nothing in the schema records which engine that was -- so there was no
-- way to ask "what should I re-translate now?" except "all of it".
--
-- `provider` answers that. It is left NULL for every existing row ON PURPOSE. All of them almost
-- certainly came from LibreTranslate, but a handful were translated through DeepL earlier today
-- while the account was being tried out, and there is no timestamp that separates the two
-- cleanly. Writing LIBRE_TRANSLATE across the table would be a guess recorded as a fact. NULL
-- reads as "predates provenance tracking", which is true of every one of them, and is exactly
-- the filter you want for a re-translation pass.
--
-- `verified` and `human_corrected` exist because correcting a cached translation by hand is
-- otherwise pointless. Entries carry expires_at; without a flag, a corrected row lapses, the
-- next request re-translates it, and the machine's version silently returns. Nothing reports it.
-- So: a verified row never expires (TranslationCache.isExpired) and is never overwritten by a
-- provider write (isProviderWritable). `human_corrected` records that the TEXT was changed;
-- `verified` records that a PERSON stands behind it, which can also mean "I read it and it was
-- already right". Both are set by the correction endpoint; neither is ever set by a provider.
--
-- The index serves the two queries this adds: "everything the old engine produced" and
-- "everything a person has blessed". Without it both are full scans of a table that is already
-- the busiest read path in the application.
--
alter table translation_cache
    add column provider varchar(40) null,
    add column human_corrected bit(1) not null default b'0',
    add column verified bit(1) not null default b'0',
    add column corrected_by varchar(255) null,
    add column corrected_at datetime(6) null;

create index idx_translation_cache_provider_verified
    on translation_cache (provider, verified);
