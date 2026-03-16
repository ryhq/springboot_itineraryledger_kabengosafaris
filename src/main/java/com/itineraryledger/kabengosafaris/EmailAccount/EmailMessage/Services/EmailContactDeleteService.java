package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailContactRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailContactDeleteService {

    private final EmailContactRepository emailContactRepository;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteContact(String accountIdObfuscated, String contactIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long contactId = idObfuscator.decodeId(contactIdObfuscated);

            EmailContact contact = emailContactRepository.findById(contactId).orElse(null);
            if (contact == null || !contact.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Contact not found", "CONTACT_NOT_FOUND"));
            }

            emailContactRepository.delete(contact);
            log.info("Deleted contact {} from account {}", contact.getEmailAddress(), accountId);

            return ResponseEntity.ok(ApiResponse.success(200, "Contact deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete contact", "DELETE_CONTACT_FAILED"));
        }
    }
}
