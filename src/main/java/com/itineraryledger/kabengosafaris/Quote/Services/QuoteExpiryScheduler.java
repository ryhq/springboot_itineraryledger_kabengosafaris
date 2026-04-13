package com.itineraryledger.kabengosafaris.Quote.Services;

import com.itineraryledger.kabengosafaris.Quote.Entity.Quote;
import com.itineraryledger.kabengosafaris.Quote.Enums.QuoteStatus;
import com.itineraryledger.kabengosafaris.Quote.Repository.QuoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuoteExpiryScheduler {

    private final QuoteRepository quoteRepository;

    /**
     * Runs daily at 2:00 AM to auto-expire quotes whose validTo date has passed.
     * Only READY and SENT quotes are eligible for automatic expiry.
     */
    @Scheduled(cron = "${quote.expiry.schedule.cron:0 0 2 * * ?}")
    @Transactional
    public void expireOverdueQuotes() {
        log.info("Starting scheduled quote expiry check");

        LocalDate today = LocalDate.now();
        List<QuoteStatus> expirableStatuses = List.of(QuoteStatus.READY, QuoteStatus.SENT);

        List<Quote> expiredQuotes = quoteRepository.findExpiredQuotes(today, expirableStatuses);

        if (expiredQuotes.isEmpty()) {
            log.info("No quotes to expire");
            return;
        }

        int count = 0;
        for (Quote quote : expiredQuotes) {
            quote.setStatus(QuoteStatus.EXPIRED);
            quote.setIsValid(false);
            quoteRepository.save(quote);
            count++;
            log.info("Auto-expired quote: {} (validTo: {})", quote.getQuoteCode(), quote.getValidTo());
        }

        log.info("Scheduled quote expiry check finished. Expired {} quote(s)", count);
    }
}
