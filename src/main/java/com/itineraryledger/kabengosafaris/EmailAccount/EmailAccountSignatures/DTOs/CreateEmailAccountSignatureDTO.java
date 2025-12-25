package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs;

import java.util.List;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.SignatureVariable;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateEmailSignatureDTO - Request DTO for creating new email signatures
 *
 * Validates all required fields for email signature creation:
 * - Signature name (required - NOT derived from email account)
 * - Signature content
 * - Variables and their default values
 * - File-based storage reference
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEmailAccountSignatureDTO {

    /**
     * Email account ID (obfuscated) that this signature belongs to
     */
    @NotBlank(message = "Email account ID is required")
    private String emailAccountId;

    /**
     * Signature name - unique identifier for this signature
     * Examples: "Sales_Team", "Support_Formal", "Marketing_Standard"
     * Required field - NOT derived from email account
     */
    @NotBlank(message = "Signature name is required")
    private String name;

    /**
     * Description of the signature's purpose
     */
    private String description;

    /**
     * HTML/text content of the signature
     * Supports variable substitution: {senderName}, {userAccountName}, etc.
     */
    @NotBlank(message = "Signature content is required")
    private String content;

    /**
     * List of variables used in this signature
     * Each variable should have:
     * - name: variable name (e.g., "senderName")
     * - defaultValue: fallback value if not provided
     * - description: human-readable description
     *
     * Example:
     * [
     *   {
     *     "name": "senderName",
     *     "defaultValue": "Unknown",
     *     "description": "Name of the sender"
     *   },
     *   {
     *     "name": "userAccountName",
     *     "defaultValue": "Sales",
     *     "description": "Account name"
     *   }
     * ]
     */
    private List<SignatureVariable> variables;

    /**
     * Whether this should be the default signature for the account
     * If true, all other signatures for this account will be set to non-default
     */
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Whether this signature is enabled/active
     * Disabled signatures won't be used
     */
    @Builder.Default
    private Boolean enabled = true;
}
