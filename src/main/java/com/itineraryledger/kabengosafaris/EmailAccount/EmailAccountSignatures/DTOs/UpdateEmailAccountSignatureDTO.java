package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs;

import java.util.List;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.ModalEntity.SignatureVariable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateEmailSignatureDTO - Request DTO for updating existing email signatures
 *
 * All fields are optional. Only provided fields will be updated.
 * Fields not included in the request will retain their current values.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEmailAccountSignatureDTO {

    /**
     * Signature name (optional)
     * If provided, must be unique per email account
     * Will rename the signature file on disk to reflect the new name
     */
    private String name;

    /**
     * Description of the signature's purpose (optional)
     */
    private String description;

    /**
     * HTML/text content of the signature (optional)
     * If provided, the signature file will be updated
     */
    private String content;

    /**
     * List of variables used in this signature (optional)
     * Each variable should have:
     * - name: variable name (e.g., "senderName")
     * - defaultValue: fallback value if not provided
     * - description: human-readable description
     */
    private List<SignatureVariable> variables;

    /**
     * Whether this should be the default signature for the account (optional)
     * If set to true, all other signatures for this account will be set to non-default
     */
    private Boolean isDefault;

    /**
     * Whether this signature is enabled/active (optional)
     */
    private Boolean enabled;
}
