package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.SignatureVariable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EmailSignatureDTO - Data Transfer Object for Email Signatures
 *
 * Transfers email signature information to clients with:
 * - Obfuscated IDs for security
 * - File reference and metadata
 * - Variable definitions
 * - Status and audit information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailAccountSignatureDTO {

    /**
     * Obfuscated ID encoded using IdObfuscator
     */
    private String id;

    /**
     * Email account ID (obfuscated)
     */
    private String emailAccountId;

    /**
     * Signature name (derived from email account)
     */
    private String name;

    /**
     * Description of the signature's purpose
     */
    private String description;

    /**
     * Signature content (HTML/text)
     */
    private String content;

    /**
     * Filename where the signature is stored
     */
    private String fileName;

    /**
     * Whether this is the default signature for the account
     */
    private Boolean isDefault;

    /**
     * Whether this signature is enabled/active
     */
    private Boolean enabled;

    /**
     * Whether this is a system-generated default signature
     * System default signatures cannot be deleted
     */
    private Boolean isSystemDefault;

    /**
     * List of variables defined for this signature
     */
    private List<SignatureVariable> variables;

    /**
     * File size in bytes
     */
    private Long fileSize;

    /**
     * Timestamp when this signature was created
     */
    private LocalDateTime createdAt;

    /**
     * Timestamp when this signature was last updated
     */
    private LocalDateTime updatedAt;
}
