package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailContactRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateEmailContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailContact;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailContactUpdateService {

    private final EmailContactRepository emailContactRepository;
    private final EmailContactGetService emailContactGetService;
    private final IdObfuscator idObfuscator;

    @Transactional
    public ResponseEntity<ApiResponse<?>> updateContact(String accountIdObfuscated, String contactIdObfuscated, CreateEmailContactDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long contactId = idObfuscator.decodeId(contactIdObfuscated);

            EmailContact contact = emailContactRepository.findById(contactId).orElse(null);
            if (contact == null || !contact.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Contact not found", "CONTACT_NOT_FOUND"));
            }

            if (dto.getEmailAddress() != null) {
                contact.setEmailAddress(dto.getEmailAddress().toLowerCase().trim());
            }
            if (dto.getDisplayName() != null) {
                contact.setDisplayName(dto.getDisplayName());
            }
            if (dto.getIsStarred() != null) {
                contact.setIsStarred(dto.getIsStarred());
            }

            contact = emailContactRepository.save(contact);

            return ResponseEntity.ok(ApiResponse.success(200, "Contact updated successfully", emailContactGetService.toDTO(contact)));
        } catch (Exception e) {
            log.error("Error updating contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update contact", "UPDATE_CONTACT_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> toggleStar(String accountIdObfuscated, String contactIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long contactId = idObfuscator.decodeId(contactIdObfuscated);

            EmailContact contact = emailContactRepository.findById(contactId).orElse(null);
            if (contact == null || !contact.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Contact not found", "CONTACT_NOT_FOUND"));
            }

            contact.setIsStarred(!contact.getIsStarred());
            emailContactRepository.save(contact);

            return ResponseEntity.ok(ApiResponse.success(200,
                contact.getIsStarred() ? "Contact starred" : "Contact unstarred",
                contact.getIsStarred()));
        } catch (Exception e) {
            log.error("Error toggling contact star", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to toggle star", "TOGGLE_STAR_FAILED"));
        }
    }
}
