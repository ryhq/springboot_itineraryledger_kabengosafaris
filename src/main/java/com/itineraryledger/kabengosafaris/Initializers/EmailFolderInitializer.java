package com.itineraryledger.kabengosafaris.Initializers;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailFolderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Creates system email folders (INBOX, SENT, DRAFTS, TRASH, ARCHIVE) for
 * any existing email account that doesn't have them yet.
 * Runs after EmailSettingsInitializer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailFolderInitializer implements ApplicationRunner, Ordered {

    private final EmailAccountRepository emailAccountRepository;
    private final EmailFolderRepository emailFolderRepository;
    private final EmailFolderService emailFolderService;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 21;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Initializing email folders for existing accounts...");

        List<EmailAccount> accounts = emailAccountRepository.findAll();
        int initialized = 0;

        for (EmailAccount account : accounts) {
            // Check if this account already has folders
            if (emailFolderRepository.findByEmailAccountIdOrderByTypeAsc(account.getId()).isEmpty()) {
                emailFolderService.createSystemFolders(account);
                initialized++;
            }
        }

        if (initialized > 0) {
            log.info("Initialized email folders for {} accounts", initialized);
        } else {
            log.info("All email accounts already have folders");
        }
    }
}
