# Translation: how it works, and what to do when it does not

Verified 2026-09-14 against both live installations (Kabengo Safaris, Jatelo African Travels)
and against the engine itself.

Written because translation fails **silently by design**, which means the symptom of a dead
provider is identical to the symptom of a mis-wired field, and the two get diagnosed the wrong
way round.

## The path a string takes

```
public website
   -> GET /api/public/<whatever>            the DTO carries @Translatable fields
   -> the translation walker                per field, catches its own errors
   -> TranslationCache                      hit? return it, done
   -> TranslationProviderFactory            newest account with enabled AND isDefault
   -> LibreTranslateProvider                POST {baseUrl}/translate with the api key
   -> https://translate.ryhqtech.com        Caddy -> 127.0.0.1:5000 -> libretranslate
```

Two facts about that chain decide almost every question below:

1. **The walker keeps the English original when a field fails.** The website never breaks. It
   also never says anything. A provider outage looks exactly like a field somebody forgot to
   annotate.
2. **Only cache misses reach the provider.** Content translated while the provider worked keeps
   rendering translated after it dies, so "most of the site is fine" is not evidence the
   provider is fine. It is evidence the cache is fine.

## The two-minute diagnosis

Before concluding that a new `@Translatable` field is mis-wired, translate a string nothing can
have cached:

```bash
curl -s -X POST https://api.kabengosafaris.ryhqtech.com/api/public/translation/translate-messages \
  -H 'Content-Type: application/json' \
  -d '{"texts":["A game drive at sunrise. '"$(date +%s)"'"],"targetLanguage":"fr"}'
```

| Answer | Means |
| --- | --- |
| `"failed":0` and French back | The provider is healthy. Your field is the problem. |
| `"failed":1` and the English back | The provider, its account, its key or its quota is the problem. Nothing to do with your field. |

The timestamp matters. Without it you may be reading a cache entry from months ago.

Then, if it failed: **Settings -> Translation accounts** in the panel. The row shows the base
URL, whether it is enabled and default, when it was last tested and **the last error message**,
which is usually the whole answer.

## The engine

| | |
| --- | --- |
| Host | `translate.ryhqtech.com` (DigitalOcean, FRA1, 1 vCPU / 2 GB + 4 GB swap) |
| Engine | LibreTranslate, **pinned** to a version tag in `/opt/libretranslate/docker-compose.yml` |
| Languages | `en, fr, de, es, it, pt, sw` — every pair, pivoting through English |
| TLS | Caddy + Let's Encrypt, auto-renewing. LibreTranslate binds `127.0.0.1` only |
| Firewall | UFW: 22, 80, 443 |
| Shared by | Both companies, each with its own API key. The engine is stateless; no company record is stored on it |

`POST /translate` requires an API key. `GET /languages` deliberately does **not**: the app's own
health check (`LibreTranslateProvider.isServiceAvailable`) fetches it without one, and requiring
a key there would report the service down while it worked perfectly.

There is a longer operator note on the box itself at `/opt/libretranslate/README.md`.

## API keys

There is no signup. You mint them on the droplet.

```bash
ssh -i ~/.ssh/translate_droplet root@159.223.25.212

# issue — 300 is requests per minute for this key
docker exec libretranslate ./venv/bin/python manage.py keys add 300 --key "kab_$(openssl rand -hex 24)"

# revoke — positional, no --key flag on remove. Effective immediately.
docker exec libretranslate ./venv/bin/python manage.py keys remove "<the key>"

# list what exists
docker exec libretranslate ./venv/bin/python -c "
import sqlite3
for row in sqlite3.connect('/app/db/api_keys.db').execute('select * from api_keys'): print(row)
"
```

A company may hold **as many keys as you like** — the database is keys and limits, with no notion
of a user. That is how you rotate without an outage:

1. Issue a new key.
2. Paste it into that company's Translation account in the panel and save.
3. Translate a throwaway string (above) and confirm `failed:0`.
4. Revoke the old key.

The app stores **one** key per account, so only one is ever in use at a time. The live values are
in each panel under Settings -> Translation accounts, and in the droplet's key database. They are
deliberately not written down here.

## Adding a language

Two places, and **both** are required:

1. `translation.supported.languages` in `application.properties` — what the app advertises.
2. `LT_LOAD_ONLY` in `/opt/libretranslate/docker-compose.yml`, then `docker compose up -d` —
   what the engine can actually do.

Advertise a language the engine did not load and every string in it fails silently and ships in
English. Load one the app does not advertise and you have simply paid for RAM.

## Upgrading the engine

The image tag is pinned on purpose: `docker compose pull` on `latest` would change the engine
under two live websites at the next restart.

```bash
cd /opt/libretranslate
# edit the image tag in docker-compose.yml, then
docker compose up -d
docker compose logs -f      # first boot after an upgrade may fetch models
```

Then translate one string in each language before walking away.

## The cache

| | |
| --- | --- |
| Lifetime | `translation.cache.ttl.hours=168` — seven days, then re-translated on next request |
| Stats | `GET /api/translation/cache/stats` |
| Browse | `GET /api/translation/cache/entries?sourceLanguage=en&targetLanguage=fr` |
| Clear | `DELETE /api/translation/cache` (filterable by language, or `expiredOnly`) |

**You cannot edit a cache entry.** There is no update endpoint, and every row carries
`expiresAt`, so even a hand-edit in MySQL is overwritten within the week. Deleting a bad entry
re-translates it to the same wrong wording. This matters for the next section.

## Quality, and what you can actually do about it

The engine is Argos. It is free, private and self-hosted, and it is not DeepL. Safari vocabulary
is exactly where that shows — measured, not guessed:

| English | Result |
| --- | --- |
| "a game drive at sunrise" -> de | **"eine Pirschfahrt"** — correct |
| "a game drive at sunrise" -> fr | "une balade" — "game" dropped |
| "a game drive at sunrise" -> es | "una unidad de juego" — "a game unit" |
| "the crater rim is cold" -> fr | "le cratère est froid" — "rim" dropped |
| bracketed tokens -> sw | mangled into a Wikipedia category artifact |

What works today:

- **Reword the English.** "A morning safari drive" survives translation where "a game drive"
  does not. This is the only lever that needs no code, and it improves the English too.
- **Accept it per language.** German is good. French and Spanish need the rewording pass.

What does not work, despite sounding like it should:

- Editing the cached translation. No endpoint, and the TTL would overwrite it.
- Deleting the entry so it "tries again". It is deterministic; it will produce the same words.

If hand-corrected wording is wanted, the smallest honest change is a **human override**: a flag
on the cache row meaning "a person wrote this, do not expire it and do not overwrite it", plus an
endpoint to set it. That is a real feature, not a configuration change, and it is not built.

## Sharp edges, stated plainly

1. **One droplet serves both companies.** The engine holds no company data, but both companies'
   customer-facing strings cross it. Separate keys mean either can be revoked alone; they do not
   mean separate machines.
2. **An untested account cannot be made default.** The API refuses it. Run
   `POST /api/translation-accounts/{id}/test` first — this is a guard, not a bug.
3. **Nothing un-defaults the other accounts.** The factory takes the newest row that is both
   enabled and default, so a second default silently shadows the first. Demote the old one
   explicitly.
4. **2 GB is deliberate.** Measured with all seven languages loaded and every pair exercised:
   828 MiB resident, 11 MiB of swap touched. Resizing is a DigitalOcean slider and a reboot;
   nothing is pinned to the size.
5. **A cold pair is slow once.** ~6s for the first call on a pair, ~1s warm. The cache means a
   visitor pays that once per string, never per page.

## What went wrong in August 2026

Kabengo's default provider was **DeepL free**, which hit its quota and returned `456` on every
call. From 17 August until 14 September, every uncached string failed. Cached content kept
rendering in French and Spanish, so the site looked fine and the failure was invisible — it read
as "the new fields are mis-wired", which is what sent two people looking in the wrong place.

Jatelo had **no translation account at all**. Translation there had never worked, and had never
said so.

Both now point at the self-hosted engine and answer `failed:0`. The DeepL row is kept, disabled
and not default, so the history stays visible rather than being tidied away.
