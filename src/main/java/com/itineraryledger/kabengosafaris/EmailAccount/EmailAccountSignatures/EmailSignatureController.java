package com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.CreateEmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.DTOs.UpdateEmailAccountSignatureDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureCreateService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureDeleteService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureGetService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountSignatures.Services.EmailAccountSignatureUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;

/**
 * REST Controller for Email Signature management endpoints
 *
 * Provides endpoints for:
 * - Creating new email signatures with variable support
 * - Retrieving signatures for email accounts
 * - Updating signature content and settings
 * - Managing default signatures
 * - Previewing signatures with variable substitution
 * - Deleting signatures
 */
@RestController
@RequestMapping("/api/email-accounts/{emailAccountId}/signatures")
public class EmailSignatureController {

    @Autowired
    private EmailAccountSignatureCreateService emailAccountSignatureCreateService;

    @Autowired
    private EmailAccountSignatureGetService emailAccountSignatureGetService;

    @Autowired
    private EmailAccountSignatureUpdateService emailAccountSignatureUpdateService;

    @Autowired
    private EmailAccountSignatureDeleteService emailAccountSignatureDeleteService;

    /**
     * Create a new email signature for an account
     *
     * @param emailAccountId The obfuscated email account ID
     * @param createDTO The request DTO with signature details
     * @return ResponseEntity with ApiResponse containing created signature
     *
     * Example request:
     * POST /api/email-accounts/{emailAccountId}/signatures
     * {
     *   "emailAccountId": "encoded_id",
     *   "description": "Main sales signature",
     *   "content": "<div>Best regards, {senderName}</div>",
     *   "variables": [
     *     {
     *       "name": "senderName",
     *       "defaultValue": "Sales Representative",
     *       "description": "Name of the sender"
     *     }
     *   ],
     *   "isDefault": true,
     *   "enabled": true
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createSignature(
            @PathVariable String emailAccountId,
            @Valid @RequestBody CreateEmailAccountSignatureDTO createDTO) {
        return emailAccountSignatureCreateService.createSignature(emailAccountId, createDTO);
    }

    /**
     * Get all signatures for an email account with optional filtering and sorting
     *
     * @param emailAccountId The obfuscated email account ID
     * @param enabled Filter by enabled status (optional)
     * @param isDefault Filter by default status (optional)
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param sortDir Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated signatures
     *
     * Example request:
     * GET /api/email-accounts/{emailAccountId}/signatures?enabled=true&page=0&size=10&sortDir=desc
     *
     * Response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Signatures retrieved successfully",
     *   "data": {
     *     "signatures": [ EmailSignatureDTO, ... ],
     *     "currentPage": 0,
     *     "totalItems": 5,
     *     "totalPages": 1
     *   }
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllSignatures(
            @PathVariable String emailAccountId,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean isDefault,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        return emailAccountSignatureGetService.getAllSignatures(emailAccountId, enabled, isDefault, page, size, sortDir);
    }

    /**
     * Get a single signature by ID
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @return ResponseEntity with ApiResponse containing signature details
     *
     * Example request:
     * GET /api/email-accounts/{emailAccountId}/signatures/{signatureId}
     */
    @GetMapping("/{signatureId}")
    public ResponseEntity<ApiResponse<?>> getSignature(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId) {
        return emailAccountSignatureGetService.getSignature(emailAccountId, signatureId);
    }

    /**
     * Get signature content with variable substitution preview
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @param variables Query parameters with variable values for substitution
     * @return ResponseEntity with signature content with variables replaced
     *
     * Example request:
     * GET /api/email-accounts/{emailAccountId}/signatures/{signatureId}/preview?senderName=John%20Doe&userAccountName=Sales
     *
     * Response:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Signature preview retrieved successfully",
     *   "data": {
     *     "id": "encoded_id",
     *     "name": "sales_default",
     *     "signature": "<div>Best regards, John Doe</div>",
     *     "variables": [...]
     *   }
     * }
     */
    @GetMapping("/{signatureId}/preview")
    public ResponseEntity<ApiResponse<?>> getSignaturePreview(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId,
            @RequestParam(required = false) Map<String, String> variables) {
        return emailAccountSignatureGetService.getSignaturePreview(emailAccountId, signatureId, variables);
    }

    /**
     * Update a signature
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @param updateDTO The request DTO with fields to update (only provided fields updated)
     * @return ResponseEntity with ApiResponse containing updated signature
     *
     * Example request to update content:
     * PUT /api/email-accounts/{emailAccountId}/signatures/{signatureId}
     * {
     *   "description": "Updated description",
     *   "content": "<div>Updated content</div>",
     *   "variables": [...]
     * }
     *
     * Example request to set as default:
     * PUT /api/email-accounts/{emailAccountId}/signatures/{signatureId}
     * {
     *   "isDefault": true
     * }
     *
     * Example request to toggle enabled status:
     * PUT /api/email-accounts/{emailAccountId}/signatures/{signatureId}
     * {
     *   "enabled": false
     * }
     */
    @PutMapping("/{signatureId}")
    public ResponseEntity<ApiResponse<?>> updateSignature(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId,
            @Valid @RequestBody UpdateEmailAccountSignatureDTO updateDTO) {
        return emailAccountSignatureUpdateService.updateSignature(emailAccountId, signatureId, updateDTO);
    }

    /**
     * Restore system default signature to its original template
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @return ResponseEntity with ApiResponse containing restored signature
     *
     * Validation:
     * - Can only restore signatures that are system default (isSystemDefault=true)
     * - Returns 400 error if signature is not a system default
     * - Returns 404 if signature not found
     *
     * Example request:
     * POST /api/email-accounts/{emailAccountId}/signatures/{signatureId}/restore
     *
     * Response format (success):
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "System default signature restored successfully",
     *   "data": {
     *     "id": "encoded_sig_123",
     *     "emailAccountId": "encoded_id_xyz",
     *     "name": "Default Signature",
     *     "description": "System default signature - created automatically",
     *     "content": "<div>...</div>",
     *     "isDefault": true,
     *     "enabled": true,
     *     "isSystemDefault": true,
     *     ...
     *   }
     * }
     */
    @PostMapping("/{signatureId}/restore")
    public ResponseEntity<ApiResponse<?>> restoreSystemDefaultSignature(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId) {
        return emailAccountSignatureUpdateService.restoreSystemDefaultSignature(emailAccountId, signatureId);
    }

    /**
     * Delete a single signature by ID
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @return ResponseEntity with ApiResponse containing deletion result
     *
     * Validation:
     * - Cannot delete if the signature is set as default
     * - Returns 400 error if signature is default
     * - Returns 404 if signature not found
     *
     * Example request:
     * DELETE /api/email-accounts/{emailAccountId}/signatures/{signatureId}
     *
     * Response format (success):
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Signatures deleted successfully",
     *   "data": {
     *     "deletedCount": 1,
     *     "deletedFileNames": ["sales_team_standard_1732800645000.html"]
     *   }
     * }
     *
     * Response format (error - is default):
     * {
     *   "success": false,
     *   "statusCode": 400,
     *   "message": "Cannot delete signatures: some are set as default",
     *   "errorCode": "CANNOT_DELETE_DEFAULT_SIGNATURES"
     * }
     */
    @DeleteMapping("/{signatureId}")
    public ResponseEntity<ApiResponse<?>> deleteSignature(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId) {
        List<String> idList = new ArrayList<>();
        idList.add(signatureId);
        return emailAccountSignatureDeleteService.deleteSignatures(emailAccountId, idList);
    }

    /**
     * Delete multiple signatures by batch request
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureIdList List of obfuscated signature IDs to delete
     * @return ResponseEntity with ApiResponse containing deletion result
     *
     * Validation:
     * - If ANY signature in the list is set as default, NO signatures will be deleted
     * - Returns 400 error if any signature is default
     * - Signatures must belong to the specified email account
     *
     * Example request:
     * DELETE /api/email-accounts/{emailAccountId}/signatures
     * Body:
     * [
     *   "encoded_sig_1",
     *   "encoded_sig_2",
     *   "encoded_sig_3"
     * ]
     *
     * Response format (success):
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Signatures deleted successfully",
     *   "data": {
     *     "deletedCount": 2,
     *     "deletedFileNames": ["sig1.html", "sig2.html"]
     *   }
     * }
     *
     * Response format (error - has default signatures):
     * {
     *   "success": false,
     *   "statusCode": 400,
     *   "message": "Cannot delete signatures: some are set as default",
     *   "errorCode": "CANNOT_DELETE_DEFAULT_SIGNATURES",
     *   "data": {
     *     "deletedIds": [],
     *     "message": "Cannot delete any signatures: 1 signature(s) in the list are set as default...",
     *     "defaultSignatureIds": [5]
     *   }
     * }
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> deleteSignaturesBatch(
            @PathVariable String emailAccountId,
            @RequestBody List<String> signatureIdList) {
        return emailAccountSignatureDeleteService.deleteSignatures(emailAccountId, signatureIdList);
    }

    /**
     * Get signature content with all metadata (for WYSIWYG editor)
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @return ResponseEntity with full signature details including raw content
     *
     * Example request:
     * GET /api/email-accounts/{emailAccountId}/signatures/{signatureId}/content
     */
    @GetMapping("/{signatureId}/content")
    public ResponseEntity<ApiResponse<?>> getSignatureContent(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId) {
        return emailAccountSignatureGetService.getSignatureContent(emailAccountId, signatureId);
    }

    /**
     * Download signature HTML file
     * Returns the raw HTML file for download or direct preview in browser
     *
     * @param emailAccountId The obfuscated email account ID
     * @param signatureId The obfuscated signature ID
     * @return ResponseEntity with HTML file content
     *
     * Response headers:
     * - Content-Type: text/html; charset=UTF-8
     * - Content-Disposition: attachment; filename="signature_name.html" (for download)
     *   OR inline (for preview in browser)
     *
     * Query parameters:
     * - download=true : Force download as file (default: false, displays inline)
     *
     * Example requests:
     * GET /api/email-accounts/{emailAccountId}/signatures/{signatureId}/download (preview in browser)
     * GET /api/email-accounts/{emailAccountId}/signatures/{signatureId}/download?download=true (download file)
     */
    @GetMapping("/{signatureId}/download")
    public ResponseEntity<?> downloadSignatureFile(
            @PathVariable String emailAccountId,
            @PathVariable String signatureId,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        return emailAccountSignatureGetService.downloadSignatureFile(emailAccountId, signatureId, download);
    }

    /**
     * Get signature HTML file by filename
     * Returns the raw HTML file using the filename from the signature DTO
     *
     * @param emailAccountId The obfuscated email account ID
     * @param fileName The signature filename (e.g., "developerks_developerks_1764855656760.html")
     * @return ResponseEntity with HTML file content
     *
     * Response headers:
     * - Content-Type: text/html; charset=UTF-8
     * - Content-Disposition: inline (displays in browser)
     *
     * Query parameters:
     * - download=true : Force download as file (default: false, displays inline)
     *
     * Example requests:
     * GET /api/email-accounts/{emailAccountId}/signatures/files/developerks_developerks_1764855656760.html
     * GET /api/email-accounts/{emailAccountId}/signatures/files/developerks_developerks_1764855656760.html?download=true
     */
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<?> getSignatureFileByName(
            @PathVariable String emailAccountId,
            @PathVariable String fileName,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        return emailAccountSignatureGetService.getSignatureFileByName(emailAccountId, fileName, download);
    }
}
