# Knowing where a lead came from

Until this shipped, every booking inquiry, contact message and newsletter signup was stored with
the literal source `WEBSITE`, hardcoded in `BookingInquiryService`. A lead from a paid Google ad,
one from an Instagram post, one cited by ChatGPT and one from somebody typing the domain were
indistinguishable in the panel.

That is fine while nothing is being spent. It is unworkable the moment campaigns run on several
platforms, because no row anywhere says which of them produced a customer.

---

## How to tag a campaign

Every ad, post and mailshot must point at a URL carrying `utm_` tags. Without them the lead still
arrives, but it can only be classified by referrer, which tells you the platform and never the
campaign.

```
https://kabengosafaris.com/de/safaris/ITI-5D4N-1050
  ?utm_source=facebook
  &utm_medium=paid_social
  &utm_campaign=serengeti-migration-jul27-de
  &utm_content=carousel-a
```

| Tag | What it must say | Why |
|---|---|---|
| `utm_source` | The platform: `google`, `facebook`, `instagram`, `tiktok`, `bing`, `newsletter` | Who sent them |
| `utm_medium` | How it was bought: `cpc`, `paid_social`, `display`, `email`, `organic`, `referral` | **This is what decides paid vs organic.** Get it wrong and spend is invisible |
| `utm_campaign` | One label per budget line, e.g. `serengeti-migration-jul27-de` | This is what a spend figure gets divided by |
| `utm_content` | The creative: `carousel-a`, `video-b` | The unit of an A/B test |
| `utm_term` | The keyword bid on | Paid search only; Google Ads fills this itself with `{keyword}` |

**Keep campaign names exact and stable.** They are matched whole, never by prefix, so
`serengeti-jul` and `serengeti-july-retarget` are two budgets and stay two budgets. Rename a live
campaign and its leads split across two labels.

Use lower case with hyphens. Include the language when a campaign is language-targeted, because
one itinerary runs in English and German at once.

### Platform auto-tagging

Google Ads, Microsoft Ads and Meta can append their own click identifier. Leave that switched on:
`gclid`, `gbraid`, `wbraid` and `msclkid` only ever exist on a click that was paid for, and they
outrank `utm_medium` when classifying, so a mistagged campaign is still counted as paid.

`fbclid` is deliberately **not** treated as proof of spend. Facebook and Instagram stamp it on every
outbound link, an organic post as readily as an ad. A Meta ad must therefore carry
`utm_medium=paid_social` or it will be counted as organic social.

---

## What gets stored

Two touches per lead, on `booking_inquiries`, `contact_messages` and `newsletter_subscriptions`
(migration `V16__lead_attribution.sql`).

- **Last touch** — the visit that produced the lead. This is what a campaign gets judged on.
- **First touch** — the visit that introduced them. For a trip decided over weeks it is routinely a
  different channel, and crediting the last click alone makes the campaign that actually found the
  customer look worthless.

`attr_touch_count` says how many visits preceded the form. A high count against a paid first touch
means the ad worked and the closing channel is merely taking the credit.

### The channel

Derived on the server by `AttributionService`, never sent by the browser — the browser's values all
came out of a query string anybody can edit, and a channel a visitor could set is a channel nobody
can budget against. Deriving it here also means the rule can be corrected and old rows re-derived.

`AI_ASSISTANT` · `PAID_SEARCH` · `PAID_SOCIAL` · `PAID_DISPLAY` · `ORGANIC_SEARCH` ·
`ORGANIC_SOCIAL` · `EMAIL` · `AFFILIATE` · `REFERRAL` · `DIRECT` · `OTHER`

Order of trust: a paid click identifier first, then the declared `utm_medium`, then the source's own
identity, then the referrer. Answer engines are matched **before** search engines on purpose, or
`gemini.google.com` and `bard.google.com` would be filed as Google search and the AI number would
stay near zero while the traffic grew.

`AI_ASSISTANT` exists because of a real lead: a German customer arrived on a safari page with
`?utm_source=chatgpt.com`, already knowing the itinerary code, and the panel recorded it as
`WEBSITE` like everything else.

---

## Reading it in the panel

**Sales → Inquiries.** A "Came from" column, a "Came from" facet, a "Campaign" facet whose options
are read back off the leads themselves, and a "Spend" facet with two values.

Three stat cards, each clickable as a filter:

- **From an ad we paid for** — the leads a budget can be divided by
- **From an AI assistant**
- **No arrival recorded** — shown deliberately beside the others. Leads taken before this shipped,
  anything entered by hand, and anyone whose browser blocks storage land here. Without it the
  channel figures read as if they covered every lead, and they do not.

The full arrival is on the record page under **Where they came from**, including the click id, which
is the only thing that can be matched against an ad platform's own report when their count and ours
disagree.

---

## How the browser side works

`src/lib/attribution.ts` in the website, mounted once in the locale layout by
`AttributionCapture`.

The tags only exist on the landing URL, and a safari is not booked on the landing page — people read
three or four before they write. So the tags are read on arrival and held in `localStorage` until a
form is actually submitted, then sent with it.

- A page reached by clicking around our own site is not a new arrival. Only a tagged URL or a link
  from another host counts, or every click would inflate the touch count and overwrite a real
  channel with a blank one.
- Every storage call is guarded: private browsing and a full quota both throw, and no page should
  break because a marketing tag could not be saved.
- Stored values are dropped after a year.

**Privacy.** This is first-party only: stored under our own origin, read back only when the visitor
chooses to send us a form, never handed to a third party. No cross-site identifier is set, and
nothing is recorded about somebody who never contacts us.

---

## Not trusting any of it

Every value arrived in a query string. `AttributionService` strips control characters and angle
brackets, drops a URL that is not `http(s)` (and a protocol-relative `//host`, which would navigate
off-site from any href it reached), caps each value to its column, and discards a touch count
outside a sane range.

The existing `source` column is **not** reused for any of this. It is parsed into `CustomerSource`
when an inquiry is converted, so writing `chatgpt.com` into it would silently fall back to `WEBSITE`
and lose the very thing being recorded.

Guarded by `AChannelIsDerivedNotDeclaredTest` and `NothingAVisitorTypedIsTrustedTest`.

---

## Still to do

- The office notification email does not yet name the channel. Adding it means one variable and one
  template edit per event.
- Contact messages and newsletter signups store attribution but do not yet expose a facet or card;
  the columns are there when those lists want them.
- Nothing reconciles our count against an ad platform's. The click id is stored so that it can be.
