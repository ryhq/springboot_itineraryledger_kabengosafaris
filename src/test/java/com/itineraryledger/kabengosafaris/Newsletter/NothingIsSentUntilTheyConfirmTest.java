package com.itineraryledger.kabengosafaris.Newsletter;

import com.itineraryledger.kabengosafaris.Customer.Repository.CustomerEmailRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailSendingService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.CustomerAcknowledgementSender;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailTemplateRenderer;
import com.itineraryledger.kabengosafaris.Newsletter.DTOs.NewsletterSubscribeRequest;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.NewsletterSubscription;
import com.itineraryledger.kabengosafaris.Newsletter.Entity.SubscriptionStatus;
import com.itineraryledger.kabengosafaris.Newsletter.Repository.NewsletterSubscriptionRepository;
import com.itineraryledger.kabengosafaris.Newsletter.Services.NewsletterService;
import com.itineraryledger.kabengosafaris.NotificationSetting.NotificationSettingGetterServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A subscription nobody agreed to is a liability, not an asset.
 *
 * <p>A form on a public page will be filled in with other people's addresses, by mistake and on
 * purpose. Writing to an unconfirmed list collects bounces and spam complaints, and the sending
 * reputation that costs is shared with every quote and invoice the company emails. This account
 * already fails a third of its sends; it cannot afford more.
 *
 * <p>So the rule these tests hold is narrow and absolute: until somebody proves they own the
 * address, the ONLY thing that address may receive is the request to confirm it.
 */
class NothingIsSentUntilTheyConfirmTest {

    private NewsletterSubscriptionRepository subscriptions;
    private CustomerAcknowledgementSender acknowledgements;
    private NewsletterService service;
    private List<NewsletterSubscription> saved;

    @BeforeEach
    void setUp() {
        subscriptions = mock(NewsletterSubscriptionRepository.class);
        acknowledgements = mock(CustomerAcknowledgementSender.class);
        saved = new ArrayList<>();

        when(subscriptions.save(any(NewsletterSubscription.class))).thenAnswer(inv -> {
            saved.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(subscriptions.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        service = new NewsletterService(
            subscriptions,
            mock(CustomerEmailRepository.class),
            mock(NotificationSettingGetterServices.class),
            mock(EmailTemplateRenderer.class),
            mock(EmailSendingService.class),
            acknowledgements);

        ReflectionTestUtils.setField(service, "apiBaseUrl", "https://api.example.test");
        ReflectionTestUtils.setField(service, "confirmExpiryDays", 14);
    }

    @Test
    @DisplayName("a new address lands unconfirmed, with a token, and is asked to confirm")
    void subscribingOnlyAsks() {
        NewsletterSubscribeRequest request = new NewsletterSubscribeRequest();
        request.setEmail("Someone@Example.test");

        Map<String, Object> answer = service.subscribe(request);

        NewsletterSubscription row = saved.get(saved.size() - 1);
        assertEquals(SubscriptionStatus.PENDING_CONFIRMATION, row.getStatus(),
            "an address is not a subscriber until somebody proves they own it");
        assertNotNull(row.getConfirmToken(), "with no token there is no link, and no way to confirm");
        assertNull(row.getConfirmedAt());
        assertEquals("someone@example.test", row.getEmail(), "stored lowercase, so a second attempt matches the first");

        assertEquals("confirmation_sent", answer.get("status"),
            "the form must not claim they are subscribed; that trains people to ignore the email that matters");

        ArgumentCaptor<String> event = ArgumentCaptor.forClass(String.class);
        verify(acknowledgements).send(event.capture(), anyString(), anyString(), any());
        assertEquals("NEWSLETTER_CONFIRM", event.getValue(),
            "the ONLY email an unconfirmed address may receive");
    }

    @Test
    @DisplayName("confirming turns it on and sends the welcome, once")
    void confirmingSubscribes() {
        NewsletterSubscription pending = pending("someone@example.test", "tok-1");
        when(subscriptions.findByConfirmToken("tok-1")).thenReturn(Optional.of(pending));

        Map<String, Object> answer = service.confirm("tok-1");

        assertEquals("confirmed", answer.get("status"));
        assertEquals(SubscriptionStatus.ACTIVE, pending.getStatus());
        assertNotNull(pending.getConfirmedAt());
        verify(acknowledgements).send(org.mockito.ArgumentMatchers.eq("NEWSLETTER_WELCOME"),
            anyString(), anyString(), any());
    }

    @Test
    @DisplayName("a second click on the same link does not send a second welcome")
    void confirmingTwiceIsHarmless() {
        NewsletterSubscription already = pending("someone@example.test", "tok-2");
        already.setStatus(SubscriptionStatus.ACTIVE);
        already.setConfirmedAt(java.time.LocalDateTime.now());
        when(subscriptions.findByConfirmToken("tok-2")).thenReturn(Optional.of(already));

        Map<String, Object> answer = service.confirm("tok-2");

        assertEquals("already_confirmed", answer.get("status"));
        verify(acknowledgements, never()).send(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("an unknown or empty token confirms nothing and says so")
    void aBadTokenConfirmsNothing() {
        when(subscriptions.findByConfirmToken("nonsense")).thenReturn(Optional.empty());

        assertEquals("invalid", service.confirm("nonsense").get("status"));
        assertEquals("invalid", service.confirm(null).get("status"));
        assertEquals("invalid", service.confirm("   ").get("status"));
        verify(acknowledgements, never()).send(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("somebody who unsubscribed is asked again, never switched back on quietly")
    void resubscribingAsksAgain() {
        NewsletterSubscription gone = pending("someone@example.test", "tok-3");
        gone.setStatus(SubscriptionStatus.UNSUBSCRIBED);
        gone.setUnsubscribedAt(java.time.LocalDateTime.now());
        when(subscriptions.findByEmailIgnoreCase("someone@example.test")).thenReturn(Optional.of(gone));

        NewsletterSubscribeRequest request = new NewsletterSubscribeRequest();
        request.setEmail("someone@example.test");
        service.subscribe(request);

        assertEquals(SubscriptionStatus.PENDING_CONFIRMATION, gone.getStatus(),
            "re-subscribing silently is how a person who opted out starts receiving mail "
            + "again without ever agreeing to it");
        assertNull(gone.getUnsubscribedAt());
        verify(acknowledgements).send(org.mockito.ArgumentMatchers.eq("NEWSLETTER_CONFIRM"),
            anyString(), anyString(), any());
    }

    @Test
    @DisplayName("an existing confirmed subscriber is not pestered with another confirmation")
    void anExistingSubscriberIsLeftAlone() {
        NewsletterSubscription active = pending("someone@example.test", "tok-4");
        active.setStatus(SubscriptionStatus.ACTIVE);
        active.setConfirmedAt(java.time.LocalDateTime.now());
        when(subscriptions.findByEmailIgnoreCase("someone@example.test")).thenReturn(Optional.of(active));

        NewsletterSubscribeRequest request = new NewsletterSubscribeRequest();
        request.setEmail("someone@example.test");
        Map<String, Object> answer = service.subscribe(request);

        assertEquals(SubscriptionStatus.ACTIVE, active.getStatus());
        verify(acknowledgements, never()).send(
            org.mockito.ArgumentMatchers.eq("NEWSLETTER_CONFIRM"), anyString(), anyString(), any());
        assertEquals("confirmation_sent", answer.get("status"),
            "the same answer either way, so the form cannot be used to ask who is on the list");
    }

    @Test
    @DisplayName("unsubscribing takes the token from the link, never a bare address")
    void unsubscribeUsesTheToken() {
        NewsletterSubscription active = pending("someone@example.test", "tok-5");
        active.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptions.findByConfirmToken("tok-5")).thenReturn(Optional.of(active));

        Map<String, Object> answer = service.unsubscribeByToken("tok-5");

        assertEquals(SubscriptionStatus.UNSUBSCRIBED, active.getStatus());
        assertNotNull(active.getUnsubscribedAt());
        assertEquals("unsubscribed", answer.get("status"));

        // and an unknown token answers identically, so it cannot be used to probe the list
        assertEquals("unsubscribed", service.unsubscribeByToken("who-knows").get("status"));
        assertFalse(answer.containsKey("email"));
    }

    private NewsletterSubscription pending(String email, String token) {
        NewsletterSubscription sub = new NewsletterSubscription();
        sub.setEmail(email);
        sub.setStatus(SubscriptionStatus.PENDING_CONFIRMATION);
        sub.setConfirmToken(token);
        ReflectionTestUtils.setField(sub, "subscribedAt", java.time.LocalDateTime.now());
        return sub;
    }
}
