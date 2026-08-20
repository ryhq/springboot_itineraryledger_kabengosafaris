# Can one company see another's data?

No. Here is exactly why, what enforces it, and where the sharp edges are — written down because
"trust me" is not an answer anybody can act on.

Verified 2026-08-20 against the two live installations (Kabengo Safaris, Jatelo African Travels).

## What is separate

| | Separation | Enforced by |
| --- | --- | --- |
| **Process** | Its own systemd unit, its own service account (`jatelo` runs Jatelo's API), `NoNewPrivileges`, `ProtectSystem=full`, `ProtectHome`, `PrivateTmp`, and `ReadWritePaths` limited to that company's two directories | systemd |
| **Database** | Its own database and its own MySQL user, granted on that database only | MySQL grants |
| **Secrets** | `/etc/<id>.env`, mode 600 root:root. The service reads it through systemd, never as a file it could open itself | filesystem + systemd |
| **Files** | `/srv/<id>/data` and `/srv/<id>/backups`, mode 750, owned by that company's service account | filesystem |
| **Identifiers** | A separate id salt per installation, and by default a new one at every restart | configuration |
| **Cache** | None is shared. Every cache is in-process, and the processes are separate | architecture |
| **HTTP** | Each API accepts credentialed requests only from its own panel origin — the *other company's panel is refused* | CORS allow-list |
| **Deploys** | Each company's host and key live in a GitHub Environment named after it, so a run cannot reach a company it was not invoked for | GitHub Environments |

Today the two companies are also on **separate hosts**, which makes most of the above belt-and-braces.
The table describes what holds if they were ever co-hosted, because that is the case worth designing
for.

## Proved, not assumed

- **CORS**: asked each API with four `Origin` headers. Each answered only for its own panel and for
  `http://localhost:5173` (a stated development exception). A random site and *the other company's
  panel* got no `Access-Control-Allow-Origin` at all.
- **Secrets**: as the `deploy` account — the identity CI logs in with — `cat /etc/jatelo.env` is
  Permission denied, and so is `/proc/<pid>/environ` of the running API. The account that ships the
  code cannot read what the code runs with.
- **Restore**: the most recent pre-deploy dump was restored into a scratch database and compared with
  the live one: 151 tables both sides, and every table counted matched (parks 26, permissions 499,
  users 1, and so on). Then the scratch database was dropped. A backup nobody has restored is a
  hypothesis; this one is not.

## Sharp edges, stated plainly

1. **MySQL runs as one server per host.** A company's user is granted on its own database only, but a
   MySQL privilege escalation would cross that line. Two companies on one host share that risk;
   separate hosts do not.
2. **`root` on the host sees everything.** That is what root means. The mitigation is that nothing
   deploys as root: CI logs in as `deploy`, which may run exactly three commands through sudo
   (restart the service, ask if it is active, trigger the pre-deploy dump) and can read neither the
   secrets file nor the process environment.
3. **The nightly backup lands on the same disk** it is protecting. It is a restore point for a bad
   migration, not for a lost host. Off-host copies are not automated yet.
4. **One shared codebase means one shared bug.** Isolation is about data, not defects: a fault in the
   product reaches every company on the next release. That is the deliberate trade for not
   maintaining forks, and it is why the release goes to a canary first and stops there if it fails.
5. **Public media is public.** Park, accommodation, activity, hero, blog and testimony images are
   served without a token by design, since a website renders them. Every document family requires
   authentication (see `MediaExposureTest`).

## If you are asked in writing

> Each company runs a separate process under its own service account, against its own database with
> its own credentials, writing to its own storage directory, on its own host. Configuration secrets are
> readable only by root and injected by systemd. Each API accepts credentialed browser requests only
> from its own panel. Deploy credentials are scoped per company. There is no shared cache, no shared
> database and no shared filesystem path. Backups are per company, and a restore has been tested.
