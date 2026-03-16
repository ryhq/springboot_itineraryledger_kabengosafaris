package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailContactRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateEmailContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact.ContactSource;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailContactCreateService {

    private final EmailContactRepository emailContactRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailContactGetService emailContactGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> createContact(String accountIdObfuscated, CreateEmailContactDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            String normalizedEmail = dto.getEmailAddress().toLowerCase().trim();

            if (emailContactRepository.findByEmailAccountIdAndEmailAddress(accountId, normalizedEmail).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    ApiResponse.error(409, "Contact already exists for this email address", "CONTACT_EXISTS"));
            }

            EmailContact contact = EmailContact.builder()
                .emailAccount(account)
                .emailAddress(normalizedEmail)
                .displayName(dto.getDisplayName())
                .frequency(0)
                .source(ContactSource.MANUAL)
                .isStarred(Boolean.TRUE.equals(dto.getIsStarred()))
                .build();

            contact = emailContactRepository.save(contact);
            log.info("Created manual contact {} for account {}", normalizedEmail, account.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Contact created successfully", emailContactGetService.toDTO(contact)));
        } catch (Exception e) {
            log.error("Error creating contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create contact", "CREATE_CONTACT_FAILED"));
        }
    }
}
