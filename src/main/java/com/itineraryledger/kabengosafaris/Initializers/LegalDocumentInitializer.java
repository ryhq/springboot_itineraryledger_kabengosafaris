package com.itineraryledger.kabengosafaris.Initializers;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocument;
import com.itineraryledger.kabengosafaris.Legal.Entity.LegalDocumentType;
import com.itineraryledger.kabengosafaris.Legal.Repository.LegalDocumentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A privacy notice exists from the first boot, because two live websites now read one.
 *
 * <p>Those pages used to carry the text in their own markup. The moment they read it from here
 * instead, an empty table means both of them show "our notice is being updated" where a real
 * policy used to be, which is a worse page than the one it replaced and a regression nobody
 * would attribute to a deploy of the API.
 *
 * <p>So a starter notice is seeded and published, written to be true of this system as it
 * actually stands: it describes the forms, the newsletter, and the client portal's cookie and
 * access log, including the three-month deletion that {@code PortalRetentionScheduler} carries
 * out. A company edits it at Settings and publishes a version of their own; this one exists so
 * that the gap between the deploy and that edit is not a lie.
 *
 * <p>Seeded once, by version. Re-running never overwrites an edited notice, for the same reason
 * the published text is immutable: somebody has agreed to it.
 *
 * <p>It names no company. One codebase serves several, and the page supplies the heading and the
 * contact details from the company record.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentInitializer implements ApplicationRunner, Ordered {

    /** Bumping this ships a new starter notice; it does not touch one somebody has written. */
    private static final String STARTER_VERSION = "starter-1";

    private final LegalDocumentRepository repository;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 16;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            seedPrivacyNotice();
        } catch (Exception e) {
            /*
             * A missing starter notice is a page that says so. It is not a reason for the
             * application to be unreachable.
             */
            log.error("Could not seed the starter privacy notice: {}", e.getMessage(), e);
        }
    }

    private void seedPrivacyNotice() {
        if (repository.findByDocumentTypeAndPublishedTrue(LegalDocumentType.PRIVACY_POLICY).isPresent()) {
            return;
        }
        if (repository.findByDocumentTypeAndVersion(
                LegalDocumentType.PRIVACY_POLICY, STARTER_VERSION).isPresent()) {
            /* written once and then unpublished on purpose; leave that decision alone */
            return;
        }

        LegalDocument saved = repository.save(LegalDocument.builder()
            .documentType(LegalDocumentType.PRIVACY_POLICY)
            .version(STARTER_VERSION)
            .title("Privacy policy")
            .body(STARTER_PRIVACY_NOTICE)
            .effectiveFrom(LocalDate.now())
            .published(true)
            .build());

        log.info("Seeded and published a starter privacy notice ({}). Edit it at Settings "
            + "and publish a version of your own.", saved.getVersion());
    }

    /**
     * Written in the small markup the renderer understands: a blank line is a paragraph,
     * {@code ## } a heading, {@code - } a bullet. No HTML, because none is accepted.
     */
    private static final String STARTER_PRIVACY_NOTICE = """
        We are a tour operator. This notice says what we hold about you, why, and for how long. \
        If anything here is unclear, ask us and a person will answer.

        ## What you give us

        - When you ask for a quotation or send us a message: your name, your email address, your \
        telephone number, and whatever you tell us about the trip you want.
        - When you book: the details a trip actually needs, which may include your passport \
        number, your date of birth, your nationality, and any dietary or medical requirement you \
        choose to tell us about.
        - When you subscribe to our newsletter: your email address, and nothing else.

        ## What we record when you use your trip page

        If we send you a link to your own trip, that page keeps one cookie. It holds a random \
        value and nothing else, and it is what stops you having to prove who you are on every \
        page. You can end it at any time from the page itself.

        We also record which sections you open, from which internet address and on which kind of \
        device, so that we can answer you when something is not where you expected it to be.

        We do not set advertising cookies and we do not share any of this with an advertising \
        network.

        ## How long we keep it

        - What you looked at on your trip page is destroyed three months after your trip ends, or \
        three months after a booking does not go ahead.
        - Your quotation, your invoice and your receipts are kept for as long as the law requires \
        us to keep business records. They are the record of a transaction rather than a record of \
        you.
        - Your contact details stay on your customer record until you ask us to remove them.

        ## Who else sees it

        The suppliers a trip needs: the lodges you are staying at, the airlines you are flying, \
        the parks you are entering, and the payment provider handling your money. Each of them \
        receives only what their part of the trip requires. We do not sell anything about you to \
        anybody.

        ## What you can ask for

        - A copy of everything we hold about you.
        - A correction, if something is wrong.
        - Erasure, where we are not required to keep it.
        - A copy in a form you can take elsewhere.

        Write to us and we will answer within a month, usually much sooner.

        ## Keeping it safe

        Your trip page is reached with a code sent to the address we already hold for you, and we \
        will never ask you for that code by telephone, by message, or in a reply to an email. We \
        will never email you changed bank details. If you receive a message that appears to come \
        from us asking you to pay a different account, telephone us before sending anything.

        ## Changes

        We publish a new version of this notice when it changes, and the version you agreed to \
        stays on your record.
        """;
}
