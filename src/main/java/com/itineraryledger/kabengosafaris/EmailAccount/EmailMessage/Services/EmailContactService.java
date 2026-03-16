package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailContactRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact.ContactSource;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Auto-harvest service — called from EmailComposeService and EmailReceivingService.
 * CRUD operations are in EmailContactGetService, EmailContactCreateService,
 * EmailContactUpdateService, and EmailContactDeleteService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailContactService {

    private final EmailContactRepository emailContactRepository;

    /**
     * Upsert a contact from an email interaction.
     * If the contact exists, increment frequency and update lastContactedAt.
     * If not, create a new one.
     */
    @Transactional
    public void harvestContact(EmailAccount account, String emailAddress, String displayName, ContactSource source) {
        if (emailAddress == null || emailAddress.isBlank()) return;

        String normalizedEmail = emailAddress.toLowerCase().trim();

        // Don't harvest the account's own email
        if (normalizedEmail.equalsIgnoreCase(account.getEmail())) return;

        try {
            EmailContact existing = emailContactRepository
                .findByEmailAccountIdAndEmailAddress(account.getId(), normalizedEmail)
                .orElse(null);

            if (existing != null) {
                existing.setFrequency(existing.getFrequency() + 1);
                existing.setLastContactedAt(LocalDateTime.now());
                if ((existing.getDisplayName() == null || existing.getDisplayName().isBlank())
                        && displayName != null && !displayName.isBlank()) {
                    existing.setDisplayName(displayName);
                }
                emailContactRepository.save(existing);
            } else {
                EmailContact contact = EmailContact.builder()
                    .emailAccount(account)
                    .emailAddress(normalizedEmail)
                    .displayName(displayName)
                    .frequency(1)
                    .lastContactedAt(LocalDateTime.now())
                    .source(source)
                    .isStarred(false)
                    .build();
                emailContactRepository.save(contact);
            }
        } catch (Exception e) {
            log.debug("Failed to harvest contact {}: {}", emailAddress, e.getMessage());
        }
    }

    /**
     * Harvest multiple addresses at once (e.g., TO, CC, BCC from a sent email)
     */
    @Transactional
    public void harvestContacts(EmailAccount account, List<String> emailAddresses, ContactSource source) {
        if (emailAddresses == null) return;
        for (String email : emailAddresses) {
            harvestContact(account, email, null, source);
        }
    }
}
