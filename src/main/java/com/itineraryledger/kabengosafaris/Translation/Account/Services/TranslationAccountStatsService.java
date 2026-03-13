package com.itineraryledger.kabengosafaris.Translation.Account.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TranslationAccountStatsService {

    private final TranslationAccountRepository translationAccountRepository;

    @Autowired
    public TranslationAccountStatsService(TranslationAccountRepository translationAccountRepository) {
        this.translationAccountRepository = translationAccountRepository;
    }

    @Async
    @Transactional
    public void recordTranslation(Long accountId, int charCount) {
        if (accountId == null) return;
        try {
            translationAccountRepository.incrementStats(accountId, charCount);
        } catch (Exception e) {
            log.warn("Failed to record translation stats for account {}: {}", accountId, e.getMessage());
        }
    }

    @Async
    @Transactional
    public void recordFailure(Long accountId) {
        if (accountId == null) return;
        try {
            translationAccountRepository.incrementFailures(accountId);
        } catch (Exception e) {
            log.warn("Failed to record translation failure for account {}: {}", accountId, e.getMessage());
        }
    }
}
