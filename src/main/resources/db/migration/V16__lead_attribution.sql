-- Where a lead came from, kept on the lead.
--
-- Until now every inquiry, contact message and newsletter signup was stored with the literal
-- source 'WEBSITE', hardcoded at BookingInquiryService:77. A lead from a paid Google ad, one
-- from an Instagram post, one cited by ChatGPT and one from somebody typing the domain were
-- indistinguishable in the panel. That is fine while nothing is being spent; it is unworkable
-- the moment campaigns run on several platforms, because there is no row anywhere that says
-- which of them produced a customer.
--
-- The existing `source` column is NOT reused. It is parsed into the CustomerSource enum when an
-- inquiry is converted (BookingInquiryUpdateService.parseSource), so putting 'chatgpt.com' in it
-- would silently fall back to WEBSITE and lose the very thing we are recording.
--
-- Two touches are kept per lead. The last touch is what converted them, which is what an ad
-- platform gets judged on. The first touch is what introduced them, and for a trip that takes
-- months to decide it is routinely a different channel: crediting the last click alone makes the
-- campaign that actually found the customer look worthless.
--
-- Every value here arrived in a query string a visitor can edit. AttributionService strips
-- control characters and angle brackets, refuses a URL that is not http(s), and caps each value
-- to the width declared below. The channel is derived on the server and never accepted from the
-- browser, so it stays something that can be corrected and re-derived later.

-- Booking inquiries — the form that matters most, because it carries the trip.
ALTER TABLE booking_inquiries
    ADD COLUMN attr_channel          VARCHAR(30)  NULL,
    ADD COLUMN attr_source           VARCHAR(120) NULL,
    ADD COLUMN attr_medium           VARCHAR(120) NULL,
    ADD COLUMN attr_campaign         VARCHAR(180) NULL,
    ADD COLUMN attr_content          VARCHAR(180) NULL,
    ADD COLUMN attr_term             VARCHAR(180) NULL,
    ADD COLUMN attr_click_id         VARCHAR(255) NULL,
    ADD COLUMN attr_click_id_type    VARCHAR(20)  NULL,
    ADD COLUMN attr_landing_page     VARCHAR(500) NULL,
    ADD COLUMN attr_referrer         VARCHAR(500) NULL,
    ADD COLUMN attr_first_channel    VARCHAR(30)  NULL,
    ADD COLUMN attr_first_source     VARCHAR(120) NULL,
    ADD COLUMN attr_first_medium     VARCHAR(120) NULL,
    ADD COLUMN attr_first_campaign   VARCHAR(180) NULL,
    ADD COLUMN attr_first_seen_at    DATETIME     NULL,
    ADD COLUMN attr_touch_count      INT          NULL;

-- Indexed on the two columns a campaign report groups by. Channel is low cardinality and is what
-- the stat cards count; campaign is what a spend figure is divided by.
CREATE INDEX idx_inquiry_attr_channel  ON booking_inquiries (attr_channel);
CREATE INDEX idx_inquiry_attr_campaign ON booking_inquiries (attr_campaign);

-- Contact messages — a cheaper conversion, and often the one an awareness campaign buys.
ALTER TABLE contact_messages
    ADD COLUMN attr_channel          VARCHAR(30)  NULL,
    ADD COLUMN attr_source           VARCHAR(120) NULL,
    ADD COLUMN attr_medium           VARCHAR(120) NULL,
    ADD COLUMN attr_campaign         VARCHAR(180) NULL,
    ADD COLUMN attr_content          VARCHAR(180) NULL,
    ADD COLUMN attr_term             VARCHAR(180) NULL,
    ADD COLUMN attr_click_id         VARCHAR(255) NULL,
    ADD COLUMN attr_click_id_type    VARCHAR(20)  NULL,
    ADD COLUMN attr_landing_page     VARCHAR(500) NULL,
    ADD COLUMN attr_referrer         VARCHAR(500) NULL,
    ADD COLUMN attr_first_channel    VARCHAR(30)  NULL,
    ADD COLUMN attr_first_source     VARCHAR(120) NULL,
    ADD COLUMN attr_first_medium     VARCHAR(120) NULL,
    ADD COLUMN attr_first_campaign   VARCHAR(180) NULL,
    ADD COLUMN attr_first_seen_at    DATETIME     NULL,
    ADD COLUMN attr_touch_count      INT          NULL;

CREATE INDEX idx_contact_attr_channel ON contact_messages (attr_channel);

-- Newsletter signups — the usual top-of-funnel conversion goal an ad platform optimises towards,
-- so it has to carry the same tags or the platform is optimising against a number we cannot see.
ALTER TABLE newsletter_subscriptions
    ADD COLUMN attr_channel          VARCHAR(30)  NULL,
    ADD COLUMN attr_source           VARCHAR(120) NULL,
    ADD COLUMN attr_medium           VARCHAR(120) NULL,
    ADD COLUMN attr_campaign         VARCHAR(180) NULL,
    ADD COLUMN attr_content          VARCHAR(180) NULL,
    ADD COLUMN attr_term             VARCHAR(180) NULL,
    ADD COLUMN attr_click_id         VARCHAR(255) NULL,
    ADD COLUMN attr_click_id_type    VARCHAR(20)  NULL,
    ADD COLUMN attr_landing_page     VARCHAR(500) NULL,
    ADD COLUMN attr_referrer         VARCHAR(500) NULL,
    ADD COLUMN attr_first_channel    VARCHAR(30)  NULL,
    ADD COLUMN attr_first_source     VARCHAR(120) NULL,
    ADD COLUMN attr_first_medium     VARCHAR(120) NULL,
    ADD COLUMN attr_first_campaign   VARCHAR(180) NULL,
    ADD COLUMN attr_first_seen_at    DATETIME     NULL,
    ADD COLUMN attr_touch_count      INT          NULL;

CREATE INDEX idx_newsletter_attr_channel ON newsletter_subscriptions (attr_channel);
