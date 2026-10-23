# The client portal: one link per journey

Status: design, agreed in conversation 2026-10-22. Nothing built.

## Why

A client's journey runs enquiry, itinerary, quotation, agreement, invoice, safari. Today every
step of it is a PDF that we generate and send. The document is a dead end: the client reads it,
then comes back by email or WhatsApp for every reaction they have, and every reaction becomes
work for us even when it is something they could have done themselves.

The portal replaces the file with a link. The link is the live record, not a snapshot of it, and
the client can act on it.

## Scope of a link

**One link per client per itinerary, covering the whole journey.** Behind it: the itinerary, the
quotation, the invoice, receipts, our recommendations, and the safari once it exists. The client
never has to remember which email held which PDF.

A client with two trips has two links. The link is scoped to one journey and can see nothing
else, including that client's other bookings.

## How a client gets one

**From a published itinerary.** They browse the public site, pick a trip, and request a
quotation, which is effectively creating a booking request. They verify their email address, and
the portal opens. The verification is not optional: without it, anybody can type a stranger's
address into the public form and be handed a link to that stranger's journey. It is also what
keeps bots out of the booking pipeline.

**From the booking form.** When nothing published fits, they describe what they want. That
arrives with us, we build the itinerary, publish it, and issue the quotation. From there they are
on the same rails.

A stranger never reaches the customiser. The portal is only ever for somebody who has already
asked for something concrete.

## Where the portal runs

**React pages built from the panel's own source, served from `my.<domain>`.**

The first attempt server-rendered them from the API with Thymeleaf, for three good reasons: a
same-origin session cookie, no third build, and the company's branding already available where
the PDFs are rendered. It was the wrong call, and seeing it decided the matter. A page hand-made
in a template engine, next to an application that already has a component library and a brand
system, looks exactly like what it is, and this is the first thing a customer sees of the
company that is not a PDF.

So the pages live in the panel's repository, use its components and its brand tokens, and are
deployed by the same build to a second hostname. One codebase, one artifact, two addresses.

**Why a second hostname rather than a route on `management.<domain>`.** The panel keeps a staff
token in local storage. A page designed to be opened by strangers on unknown devices must not
share an origin with that, so `my.<domain>` serves the same files from the same directory and the
browser keeps the two apart. It costs one DNS record and one virtual host per company, and no
change to CI at all.

**The session is a header, not a cookie.** `management.kabengosafaris.com` and
`api.kabengosafaris.ryhqtech.com` are different registrable domains, so a `SameSite` cookie is
never sent: a cookie would have worked for one company and silently not for the other. Verifying
returns the session in the body and the page sends it as `X-Portal-Session`. A header the browser
does not attach by itself is also what removes the need for a CSRF token.

**The server-rendered pages remain** at `api.<domain>/portal/…`, sharing every service, view model
and access check with the JSON surface. They are the fallback for a company with no portal
hostname yet. Both front ends resolve access through the same `PortalAccessService`, and a test
fails the build if either starts deciding for itself or if the two claim one path without saying
what each consumes, which is a startup failure rather than a bug.

## What the client may change

The quotation, never the itinerary. The itinerary is the blueprint that the next client will be
sold; the quotation already carries a full copy of it, day by day, which is why it can be the
sandbox. A client moving a hotel is editing their own copy, and the price moves as they do it.

### Editing levels

| Level | What the client may do | What it exposes |
|---|---|---|
| **0 Respond** | View, accept, decline, ask for a discount, leave a note | Nothing new |
| **1 Choose what is offered** (default) | Swap a night to an alternative we already recorded, include or drop optional activities, change rooms and pax | Nothing new. Everything is already priced |
| **2 Choose from the catalogue** | Any accommodation we hold rates for in that place on that date, any activity in the parks on the route. Route and days fixed | Our supplier list for those locations |

**Level 3, rebuilding the trip, is not offered.** The quotation is a snapshot that has to stay in
step with the itinerary it came from, so adding, removing or reordering days is not something a
client can do to their own copy and have it remain coherent. Instead the page lets them **suggest**
it in words, and we build it for them. A suggestion is a sentence; a broken day tree is an
afternoon.

The commercial point about level 2 is worth restating: anybody who can request a quotation can
then read which lodges we work with, in which season, at what price. One competitor with an email
address harvests the supplier list in an afternoon. Level 2 is a decision to take per product, not
a default.

### Where the level is set

**Company, then itinerary, then quotation. Most specific wins.**

The quotation override is the operational one. If a particular request smells wrong, we drop that
single quotation to level 0, or lock it entirely, without touching the product or anybody else's
journey.

## The review loop

1. The client customises their quotation. Prices update as they go.
2. They **submit**. The quotation locks: it is with us, and neither side is editing underneath the
   other.
3. We review and **comment against the specific thing that is wrong**, the day or the line, not a
   blanket rejection. A client can honestly choose a Serengeti lodge for a night they will be in
   Arusha; software cannot always tell a clever change from an impossible one, and a person can.
4. They read the comments and either accept them, in which case we rebuild from the agreed
   version, or revise and send it back.

## Access

No accounts. No passwords. A password is a credential to steal, a reset flow to abuse, and the
thing a client will have forgotten nine months before the trip.

### The URL is the address, the code is the key

**No clickable magic link.** A link that logs you in is the weakest form of this, because email is
full of machines that click things: corporate mail gateways, link preview bots in WhatsApp and
Slack, antivirus that fetches every URL. Any of them can consume a one-time link before the client
does, and all of them log it.

Instead: the URL identifies the journey and opens a page that shows nothing sensitive. A short
code, typed into the page the client already has open, proves that the person reading the inbox is
the person at the browser. Nothing that leaks from the email grants access on its own.

The journey token is **its own random secret**, not one of our obfuscated ids. Hashids are derived
from a sequence, they are not secrets, and they rotate on every deploy, which makes them both
guessable and unstable.

### Layers, not a single gate

Opening the URL is one thing; reaching a document is another. Sensitivity is tiered, the same way
our media rules already split published images from private documents:

- **Open**: the trip itself, day by day, the photographs. A client can show their family without
  ceremony.
- **Verified**: the quotation, the invoice, the safari, personal details, and any action that
  commits them. These ask for the code.
- **Re-verified**: individual high-consequence actions can ask again, even inside a live session.
  Accepting a quotation and viewing an invoice are not the same risk as reading day four.

### Step-up checks from the record itself

Quotations, invoices and safaris carry codes and other metadata that only somebody holding the
paperwork would know. These make good **additional** checks on a sensitive view or action, and good
anti-automation.

One honest limit: a quote code is printed on the PDF the client forwards, so it is not a secret. It
raises the bar against casual and automated access and it is evidence of which document somebody is
holding. It is a second check, never a replacement for the emailed code.

### Devices

A trusted device is a long session, and it is safe to the extent that it is visible and killable.

- Bound to **one journey**, never to a person across journeys.
- Expires in weeks, with a hard stop after the trip plus however long the paperwork matters.
- **A "your devices" page in the portal**, listing each trusted device with its IP address and when
  it was last used, which the client can revoke. Anything beyond one device is worth their glance.
- **One short email on each new device**, saying when and roughly where. If a forwarded link is
  being used by somebody else, that email is how anybody finds out. It is the cheapest and most
  valuable control here.
- "This is not my device" for a public computer: the session dies with the browser.
- We can revoke a device or a whole journey from the panel.

## Threats, and what answers them

| Threat | Answer |
|---|---|
| Client forwards the link | The URL alone opens nothing sensitive |
| URL read by machines: referrers, history, logs, preview bots | Assume the URL is public. It is an address, not a credential |
| Token guessed | Random secret, not a Hashid, not sequential |
| Email account compromised | Irreducible. Limit the blast radius: no payment credentials, bank details displayed and never editable, new-device email, revocation |
| Code brute forced | Short expiry, few attempts, rate limit per journey and per address |
| Fishing for who is a customer | The page answers identically whether or not the address is known |
| Phishing in our name | One sending domain, strict SPF, DKIM and DMARC, and a standing line on the invoice page: we will never email you changed bank details. Invoice interception and substituted account numbers is the standard fraud in this industry |
| Cross-journey access (IDOR) | Every endpoint is scoped to the journey in the session and takes no id that could cross it. This is the one that bites in practice |
| Script injection through client notes | The portal renders no client HTML, escapes everything, and carries a strict content policy |
| CSRF on accept or submit | SameSite cookies and a token on every state change |
| Bot bookings through the public form | Email verification before the portal opens, plus rate limiting |

Every commitment a client makes through the portal, above all accepting a quotation, is written to
the audit log with the time, the address and the device, because it is a commercial commitment and
somebody will eventually dispute one.

## What we record

For support and for insight: IP address, a device label derived from the user agent, which sections
they open and how often, how long between issue and acceptance, where they stall.

Two corrections to the wish list.

**A MAC address cannot be read by a web page.** It is not exposed to browsers by any mainstream
platform, and nothing that claims to provide one is reliable. What we can have is the IP address,
the user agent, and a per-device identifier that we generate ourselves and store in the browser,
which is better for our purpose anyway because it survives a changing IP and identifies the session
rather than the hardware.

**Most of this is personal data about identified individuals, and a good share of our clients are
in the EU.** Sarah Scharfen is in Germany. Behavioural analytics tied to a named customer needs a
stated purpose, a retention period, a line in a privacy notice the client can actually read from
the portal, and the ability to hand it over or delete it on request. None of that is difficult, but
it has to be decided before we start collecting, not after. The practical version: collect what
answers a support question or improves the page, keep it for a defined period, say so plainly, and
do not keep behavioural detail longer than the trip plus the statutory window.

## What this changes in the system we have

- **The rule that client paperwork is never a link.** Today every document endpoint requires a
  staff login, so paperwork goes out as an attachment. This supersedes that rule on purpose, and
  the portal becomes the first authenticated client-facing surface we have.
- **A second kind of session**, belonging to a journey rather than to a user, with its own
  lifetime, its own revocation and no access to anything in the panel.
- **A capability flag**, off by default per company, like fleet and credit notes, so it can be
  switched on for one company first.
- **The public websites take part**: the request flow starts there, which means the booking form
  and the itinerary pages both gain a path into the portal.

## Still open

- Default retention period for behavioural data.
- Whether level 2 is enabled for any product at launch, or level 1 only.
- Whether the lead traveller of a group can invite the others to read, and at what tier.
- Whether a WhatsApp passcode, given out of band, is offered on high value bookings as a stronger
  second factor than email.
