package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.CreateEmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.EmailAccountSignature;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Repository.EmailAccountSignatureRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.extern.slf4j.Slf4j;

/**
 * EmailSignatureCreateService - Service for creating new email signatures
 *
 * Responsibilities:
 * - Validate email account exists
 * - Create signature file on disk
 * - Save signature metadata to database
 * - Handle default signature logic (only one per account)
 * - Support variable definitions
 */
@Service
@Slf4j
@Transactional
public class EmailAccountSignatureCreateService {

    @Autowired
    private EmailAccountRepository emailAccountRepository;

    @Autowired
    private EmailAccountSignatureRepository emailAccountSignatureRepository;

    @Autowired
    private EmailAccountSignatureService emailAccountSignatureService;

    @Autowired
    private EmailAccountSignatureGetService emailAccountSignatureGetService;

    @Autowired
    private IdObfuscator idObfuscator;

    /**
     * Create a new email signature for an account
     *
     * @param emailAccountIdObfuscated The obfuscated email account ID
     * @param createDTO The DTO with signature details
     * @return ResponseEntity with ApiResponse containing created signature
     */
    public ResponseEntity<ApiResponse<?>> createSignature(String emailAccountIdObfuscated, CreateEmailAccountSignatureDTO createDTO) {
        log.info("Creating signature for email account: {}", emailAccountIdObfuscated);

        try {
            // Decode email account ID
            Long emailAccountId = idObfuscator.decodeId(emailAccountIdObfuscated);

            // Verify email account exists
            EmailAccount emailAccount = emailAccountRepository.findById(emailAccountId).orElse(null);
            if (emailAccount == null) {
                log.warn("Email account not found: {}", emailAccountId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                        ApiResponse.error(
                                404,
                                "Email account not found",
                                "EMAIL_ACCOUNT_NOT_FOUND"
                        )
                );
            }

            // Check if signature name already exists for this account
            if (emailAccountSignatureRepository.existsByEmailAccountIdAndName(emailAccountId, createDTO.getName())) {
                log.warn("Signature already exists for account: {} with name: {}", emailAccountId, createDTO.getName());
                return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                                400,
                                "Signature with this name already exists for this account",
                                "SIGNATURE_ALREADY_EXISTS"
                        )
                );
            }

            // Generate filename using the provided signature name
            String fileName = emailAccountSignatureService.generateFileName(emailAccount.getName(), createDTO.getName());

            // Save signature file to disk
            boolean fileSaved = emailAccountSignatureService.saveSignatureFile(createDTO.getContent(), fileName);
            if (!fileSaved) {
                log.error("Failed to save signature file: {}", fileName);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        ApiResponse.error(
                                500,
                                "Failed to save signature file",
                                "SIGNATURE_FILE_SAVE_FAILED"
                        )
                );
            }

            // Get file size
            long fileSize = emailAccountSignatureService.getFileSize(fileName);

            // Convert variables to JSON
            String variablesJson = emailAccountSignatureService.variablesToJson(createDTO.getVariables());

            // Create EmailSignature entity
            EmailAccountSignature signature = EmailAccountSignature.builder()
                    .emailAccount(emailAccount)
                    .name(createDTO.getName())
                    .description(createDTO.getDescription())
                    .fileName(fileName)
                    .isDefault(Boolean.TRUE.equals(createDTO.getIsDefault()))
                    .enabled(Boolean.TRUE.equals(createDTO.getEnabled()))
                    .variablesJson(variablesJson)
                    .fileSize(fileSize)
                    .build();

            // If marking as default, clear other defaults for this account
            if (Boolean.TRUE.equals(createDTO.getIsDefault())) {
                emailAccountSignatureRepository.clearAllDefaults(emailAccountId);
            }

            // Save signature to database
            EmailAccountSignature savedSignature = emailAccountSignatureRepository.save(signature);

            log.info("Signature created successfully: {}", savedSignature.getId());

            // Convert to DTO
            var signatureDTO = emailAccountSignatureGetService.convertToDTO(savedSignature);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.success(
                            201,
                            "Signature created successfully",
                            signatureDTO
                    )
            );

        } catch (Exception e) {
            log.error("Error creating signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ApiResponse.error(
                            500,
                            "Failed to create signature",
                            "SIGNATURE_CREATE_FAILED"
                    )
            );
        }
    }

    /**
     * Create system default signature for a new email account
     * This signature is created automatically when an email account is created
     * It cannot be deleted (only when the email account is deleted)
     * Users can modify it or restore it to default template
     *
     * @param emailAccount The email account entity
     * @return true if signature created successfully, false otherwise
     */
    public boolean createSystemDefaultSignature(EmailAccount emailAccount) {
        log.info("Creating system default signature for email account: {}", emailAccount.getId());

        try {
            // Check if system default signature already exists
            var existingDefault = emailAccountSignatureRepository.findByEmailAccountIdAndIsDefaultTrue(emailAccount.getId());
            if (existingDefault.isPresent() && existingDefault.get().getIsSystemDefault()) {
                log.warn("System default signature already exists for account: {}", emailAccount.getId());
                return false;
            }

            // Generate default signature HTML template
            String defaultContent = emailAccountSignatureService.generateDefaultSignatureTemplate(
                emailAccount.getName(),
                emailAccount.getEmail()
            );

            // Generate filename
            String fileName = emailAccountSignatureService.generateFileName(
                emailAccount.getName(),
                "Default Signature"
            );

            // Save signature file to disk
            boolean fileSaved = emailAccountSignatureService.saveSignatureFile(defaultContent, fileName);
            if (!fileSaved) {
                log.error("Failed to save system default signature file: {}", fileName);
                return false;
            }

            // Get file size
            long fileSize = emailAccountSignatureService.getFileSize(fileName);

            // Create signature entity
            EmailAccountSignature systemSignature = EmailAccountSignature.builder()
                .emailAccount(emailAccount)
                .name("Default Signature")
                .description("System default signature - created automatically")
                .fileName(fileName)
                .isDefault(true)
                .enabled(true)
                .isSystemDefault(true)
                .variablesJson("[]")
                .fileSize(fileSize)
                .build();

            // Save to database
            emailAccountSignatureRepository.save(systemSignature);

            log.info("System default signature created successfully for account: {}", emailAccount.getId());
            return true;

        } catch (Exception e) {
            log.error("Error creating system default signature for account: {}", emailAccount.getId(), e);
            return false;
        }
    }
}
