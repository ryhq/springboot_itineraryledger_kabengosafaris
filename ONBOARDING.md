# Onboarding a company

One codebase serves several tour companies. Everything that differs is either a row in that company's
database or a line in one file on its host. This is the checklist, in the order it actually works.

Timed on Jatelo African Travels (2026-08-19): **about 40 minutes of work**, most of it waiting for a
first boot and a TLS certificate.

---

## Before you start

| You need | Why |
| --- | --- |
| A host with MySQL, Java 21 and nginx | The API, its database and its files all live there |
| Two hostnames | `api.<domain>` and `management.<domain>` |
| SMTP credentials | Without them the first account's activation email cannot be sent, and nobody can sign in |
| An email address for the first administrator | It receives the activation link |
| The company's brand files | Two icon logos, two full logos, two favicons, one raster logo for email |

Check the SMTP port your host allows outbound. DigitalOcean blocks 25, 465 and 587 on some droplets;
Brevo answers on **2525**, and a blocked port looks exactly like a wrong password.

---

## 1. Provision the API (5 minutes, then ~3 minutes of first boot)

Run **on the host, as root**. `--dry-run` first if you want to read what it will do; it prints every
command and no secret.

```bash
./provision-company.sh \
  --id jatelo \
  --name "Jatelo African Travels" \
  --api-url https://api.jateloafricantravels.com \
  --panel-url https://management.jateloafricantravels.com \
  --admin-email ops@example.com \
  --port 4450 \
  --smtp-host smtp-relay.brevo.com --smtp-port 2525 \
  --smtp-user "$SMTP_USER" --smtp-password "$SMTP_PASS" \
  --smtp-from info@jateloafricantravels.com \
  --jar /root/app.jar
```

It creates the database and a user scoped to it, `/etc/<id>.env` (root-only: database password,
encryption key, and this company's notification address), `/opt/<id>/application.properties`, storage under `/srv/<id>`,
a hardened systemd unit, and starts the service.

Re-running is safe. It never rewrites an existing `/etc/<id>.env`, because that would rotate a live
install's database password out from under the running service.

**Watch the first boot.** It runs the migrations and about 33 seeding runners, and it says what it did:

```
Features (5 switch(es) created): fleet on · credit-notes on · …
Company profile created: Jatelo African Travels (0 emails, 0 phones, 0 addresses, 0 links)
IdObfuscator: salt generated for this run — ids are re-derived on every start …
Mail account created for info@… via smtp-relay.brevo.com — the first activation email can be sent
First account created: ops@example.com (admin) with SUPERADMIN. An activation email has been sent
Skipping seeded blog posts (app.seed.demo-content.enabled is false)
```

Readiness is `http://127.0.0.1:<port+1>/actuator/health/readiness`. Allow three minutes; seeding is
most of it.

If the mail account line is missing, no activation email was sent. Fix the credentials in
`/etc/<id>.env` and restart: the first account is created on the next boot that can send it.

## 2. nginx and TLS (10 minutes, mostly certbot)

One site proxying `api.<domain>` to `127.0.0.1:<port>`, one serving the panel's docroot with
`try_files $uri $uri/ /index.html` — without that last part every deep link 404s on refresh. Then
`certbot --nginx` for both names.

## 3. Build and deploy the panel (5 minutes)

The panel is one bundle per company. Only the API URL is required; the three brand variables exist so
the sign-in screen is right on the very first paint, before `GET /api/public/brand` answers.

```bash
VITE_API_BASE_URL="https://api.<domain>/api" \
VITE_BRAND_NAME="Jatelo African Travels" \
VITE_BRAND_ACCENT="#014225" \
VITE_BRAND_MARK="J" \
npm run build
```

Copy `dist/` to the docroot. Replace `favicon-green.svg` and `favicon-white.svg` with the company's
own — they are the fallback the tab shows until the favicons are uploaded in the panel.

## 4. Hand it over

Send the administrator:

1. the panel URL,
2. a note that an activation email is waiting (nobody has a password — that is deliberate; an audit
   entry has to name a person, and a shared password names nobody).

Then they, or you with them:

- **Settings → Company** — name, legal name, TIN, emails, phones, addresses, links, and the seven
  brand files. Every document, email and PDF reads from here; the page lists what is still missing and
  what each blank costs. A **TIN** is blocking: invoices print an empty tax line without it.
- **Settings → Company → Brand → Look** — accent, roundness, font.
- **Settings → Features** — switch off what this company did not buy.
- **Finance → Bank accounts** — at least one default account, or proforma invoices print no payment
  details and nobody can pay.

## 5. CI (one merge, every company)

Add the company to `companies.json` in both repos and create a GitHub Environment named after its
`id`:

| Repo | Environment holds |
| --- | --- |
| API | `DROPLET_HOST`, `DROPLET_SSH_KEY` (the `deploy` account's key) |
| Panel | `DROPLET_HOST`, `DROPLET_SSH_KEY`, `PANEL_DIR` — or the five `CPANEL_*` for a cPanel target |

The API is built and tested once and released to the canary first; the panel is built per company,
since its API URL and fallback brand are inlined. Nothing in the workflows needs editing.

The host also needs the deploy contract: the `deploy` account with the CI public key, a root-owned
pre-deploy dump wrapper, and sudoers limited to three exact commands (restart, is-active, dump). See
`/etc/sudoers.d/50-deploy-<id>` on an existing host for the shape.

## 6. Watch it (optional, five minutes)

```bash
scp scripts/health-watch.sh root@host:/usr/local/sbin/
echo 'PORT=<actuator port>' > /etc/health-watch-<service>.env
systemctl enable --now health-watch@<service>.timer
```

Every two minutes it asks the same readiness endpoint the deploy gates on, and after three
consecutive failures it says so once — to the journal, and to `HEALTH_WEBHOOK` if set. It fixes
nothing on purpose: a watchdog that restarts things hides the fault it was meant to report.

---

## Rollback

Before anything destructive, take both halves:

```bash
mysqldump --single-transaction --routines --triggers <db> | gzip > /root/pre-change/db.sql.gz
tar czf /root/pre-change/opt.tar.gz -C /opt <id>
```

To go back: stop the service, restore the dump, extract the tarball, start. The nightly backup
scheduler writes to `/srv/<id>/backups` as well, but a backup nobody has restored is a hypothesis —
restore one on a demo company at least once.

## What is deliberately NOT in the script

DNS, TLS, the nginx sites, SMTP credentials, the GitHub Environment, and the company's own identity.
Each needs a decision or a credential that belongs to a person, and a script that pretends otherwise
just fails later with a worse message.

## Traps found the hard way

- **`~/.my.cnf` on the host may point at an application user** with rights to one database. The script
  asks for administrative access explicitly; if you run the SQL by hand, do the same.
- **The encryption key must be `openssl rand -base64 32`.** Anything not decodable as base64 fails at
  the first mail account with "Encryption failed".
- **Seeded blog posts and FAQs belong to one company.** They stay off unless
  `app.seed.demo-content.enabled=true`. A new company writes its own.
- **The backup filename prefix is a setting**, seeded once. Check it says this company's name, not the
  one whose default it inherited.
- **Check the notification addresses after the first boot**
  (`notification_settings`, `backup_settings`). They are seeded from configuration, and a shared
  default once sent a second company's booking inquiries to the first company's mailbox.
- **Ids rotate on every restart by default** — deliberate: an identifier that leaks stops resolving.
  It also means no saved deep link survives a deploy. `--stable-ids` fixes a salt per company instead.
