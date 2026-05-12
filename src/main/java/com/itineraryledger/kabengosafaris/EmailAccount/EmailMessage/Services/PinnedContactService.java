package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.PinnedContactRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreatePinnedContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.PinnedContactDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.PinnedContact;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PinnedContactService {

    private final PinnedContactRepository pinnedContactRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final IdObfuscator idObfuscator;

    public ResponseEntity<ApiResponse<?>> list(String accountIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            List<PinnedContactDTO> dtos = pinnedContactRepository
                .findByEmailAccountIdOrderByCreatedAtAsc(accountId)
                .stream().map(this::toDTO).toList();
            return ResponseEntity.ok(ApiResponse.success(200, "Pinned contacts retrieved", dtos));
        } catch (Exception e) {
            log.error("Error listing pinned contacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list pinned contacts", "LIST_PINNED_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String accountIdObfuscated, CreatePinnedContactDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }
            String email = dto.getEmail().trim().toLowerCase();
            if (pinnedContactRepository.findByEmailAccountIdAndEmail(accountId, email).isPresent()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Contact already pinned", "PINNED_CONTACT_EXISTS"));
            }
            PinnedContact saved = pinnedContactRepository.save(PinnedContact.builder()
                .emailAccount(account)
                .email(email)
                .name(dto.getName())
                .role(dto.getRole())
                .build());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Contact pinned", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error pinning contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to pin contact", "PIN_CONTACT_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String accountIdObfuscated, String pinnedIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long pinnedId = idObfuscator.decodeId(pinnedIdObfuscated);
            PinnedContact pc = pinnedContactRepository.findById(pinnedId).orElse(null);
            if (pc == null || !pc.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Pinned contact not found", "PINNED_CONTACT_NOT_FOUND"));
            }
            pinnedContactRepository.delete(pc);
            return ResponseEntity.ok(ApiResponse.success(200, "Pinned contact removed", null));
        } catch (Exception e) {
            log.error("Error unpinning contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to unpin contact", "UNPIN_CONTACT_FAILED"));
        }
    }

    private PinnedContactDTO toDTO(PinnedContact pc) {
        return PinnedContactDTO.builder()
            .id(idObfuscator.encodeId(pc.getId()))
            .email(pc.getEmail())
            .name(pc.getName())
            .role(pc.getRole())
            .build();
    }
}
