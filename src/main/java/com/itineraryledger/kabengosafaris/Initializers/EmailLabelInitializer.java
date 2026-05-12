package com.itineraryledger.kabengosafaris.Initializers;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailLabelRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailLabelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Backfill the four system labels (Quote / Booking / Vendor / Internal) on
 * every existing email account. Runs after EmailFolderInitializer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailLabelInitializer implements ApplicationRunner, Ordered {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailLabelRepository emailLabelRepository;
    private final EmailLabelService emailLabelService;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 22;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<EmailAccount> accounts = emailAccountRepository.findAll();
        int seeded = 0;
        for (EmailAccount account : accounts) {
            int existing = emailLabelRepository
                .findByEmailAccountIdOrderByIsSystemDescNameAsc(account.getId()).size();
            if (existing == 0) {
                emailLabelService.seedSystemLabels(account);
                seeded++;
            }
        }
        if (seeded > 0) log.info("Seeded system email labels for {} accounts", seeded);
    }
}
