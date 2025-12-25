package com.itineraryledger.kabengosafaris.EmailAccount;

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

import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.CreateEmailAccountDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.DTOs.UpdateEmailAccountDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailAccountCreateService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailAccountDeleteService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailAccountGetService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailAccountTestService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountServices.EmailAccountUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * REST Controller for Email Account management endpoints
 *
 * Provides endpoints for:
 * - Creating new email accounts with validation and encryption
 * - Retrieving email accounts with pagination and filtering
 * - Updating email accounts with partial updates
 * - Testing email account SMTP connections with retry logic
 * - Searching email accounts by various criteria
 */
@RestController
@RequestMapping("/api/email-accounts")
public class EmailAccountController {

    @Autowired
    private EmailAccountGetService emailAccountGetService;

    @Autowired
    private EmailAccountCreateService emailAccountCreateService;

    @Autowired
    private EmailAccountUpdateService emailAccountUpdateService;

    @Autowired
    private EmailAccountTestService emailAccountTestService;

    @Autowired
    private EmailAccountDeleteService emailAccountDeleteService;

    /**
     * Create a new email account
     *
     * @param createDTO The request DTO with email account details
     * @return ResponseEntity with ApiResponse containing created account details
     *
     * Example request:
     * POST /api/email-accounts
     * {
     *   "email": "noreply@kabengosafaris.com",
     *   "name": "Sales",
     *   "description": "Sales team email account",
     *   "smtpHost": "smtp.gmail.com",
     *   "smtpPort": 587,
     *   "smtpUsername": "sales@gmail.com",
     *   "smtpPassword": "app-password",
     *   "useTls": true,
     *   "useSsl": false,
     *   "enabled": true,
     *   "isDefault": false,
     *   "providerType": 1,
     *   "rateLimitPerMinute": 60,
     *   "maxRetryAttempts": 3,
     *   "retryDelaySeconds": 5,
     *   "verifyOnSave": true
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createEmailAccount(
        @Valid
        @RequestBody CreateEmailAccountDTO createDTO
    ) {
        return emailAccountCreateService.createEmailAccount(createDTO);
    }

    /**
     * Update an existing email account with partial updates
     *
     * @param id The obfuscated email account ID
     * @param updateDTO The request DTO with fields to update (only provided fields will be updated)
     * @return ResponseEntity with ApiResponse containing updated account details
     *
     * Example request:
     * PUT /api/email-accounts/{id}
     * {
     *   "enabled": true,
     *   "isDefault": true,
     *   "smtpPassword": "new-password"
     * }
     *
     * Note: All fields are optional. Only provided fields will be updated.
     * If isDefault is set to true, all other accounts will automatically have isDefault set to false.
     * SMTP password will be automatically encrypted before storing.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateEmailAccount(
        @PathVariable String id,
        @Valid @RequestBody UpdateEmailAccountDTO updateDTO
    ) {

        return emailAccountUpdateService.updateEmailAccount(id, updateDTO);
    }

    /**
     * Test email account SMTP connection with retry logic
     *
     * @param id The obfuscated email account ID
     * @return ResponseEntity with ApiResponse containing test result and updated account
     *
     * Test behavior:
     * - Attempts connection up to maxRetryAttempts times
     * - Waits retryDelaySeconds between attempts
     * - On success: sets enabled=true, records lastTestedAt, clears lastErrorMessage
     * - On failure: records lastErrorMessage with attempt count
     * - Respects rateLimitPerMinute configuration
     *
     * Example request:
     * POST /api/email-accounts/{id}/test
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<?>> testEmailAccount(@PathVariable String id) {
        return emailAccountTestService.testEmailAccount(id);
    }

    /**
     * Toggle email account default status
     *
     * @param id The obfuscated email account ID
     * @param setAsDefault Query parameter: true to set as default, false to unset
     * @return ResponseEntity with ApiResponse containing updated account
     *
     * Validation:
     * - Can only set as default if account is enabled (has passed test)
     * - Setting an account as default automatically unsets all other accounts
     * - Can unset default status at any time
     *
     * Example requests:
     * POST /api/email-accounts/{id}/toggle-default?setAsDefault=true
     * POST /api/email-accounts/{id}/toggle-default?setAsDefault=false
     */
    @PostMapping("/{id}/toggle-default")
    public ResponseEntity<ApiResponse<?>> toggleDefaultStatus(
            @PathVariable String id,
            @RequestParam boolean setAsDefault) {

        return emailAccountUpdateService.toggleDefaultStatus(id, setAsDefault);
    }

    /**
     * Get all email accounts with optional filtering and pagination
     *
     * @param page Page number (0-based), default: 0
     * @param size Page size, default: 10
     * @param enabled Filter by enabled status (optional)
     * @param isDefault Filter by default account status (optional)
     * @param email Filter by email address partial match (optional)
     * @param name Filter by account name partial match (optional)
     * @param providerType Filter by provider type as integer (optional)
     *                     1=GMAIL, 2=OUTLOOK, 3=SENDGRID, 4=MAILGUN, 5=AWS_SES, 6=CUSTOM
     * @param smtpHost Filter by SMTP host partial match (optional)
     * @param smtpPort Filter by SMTP port (optional)
     * @param hasErrors Filter by error status (optional)
     * @param description Filter by description partial match (optional)
     * @param useTls Filter by TLS enabled status (optional)
     * @param useSsl Filter by SSL enabled status (optional)
     * @param smtpUsername Filter by SMTP username partial match (optional)
     * @param errorMessage Filter by error message partial match (optional)
     * @param sortDir Sort direction: "asc" or "desc", default: "desc"
     * @return ResponseEntity with paginated email accounts or validation error
     *
     * Example: GET /api/email-accounts?page=0&size=10&enabled=true&sortDir=desc
     */

    @GetMapping
    public ResponseEntity<?> getAllEmailAccounts(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) Boolean enabled,
        @RequestParam(required = false) Boolean isDefault,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) Integer providerType,
        @RequestParam(required = false) String smtpHost,
        @RequestParam(required = false) Integer smtpPort,
        @RequestParam(required = false) Boolean hasErrors,
        @RequestParam(required = false) String description,
        @RequestParam(required = false) Boolean useTls,
        @RequestParam(required = false) Boolean useSsl,
        @RequestParam(required = false) String smtpUsername,
        @RequestParam(required = false) String errorMessage,
        @RequestParam(required = false) String sortDir
    ) {
        return emailAccountGetService.getAllEmailAccounts(
            page,
            size,
            enabled,
            isDefault,
            email,
            name,
            providerType != null ? providerType : 0,
            smtpHost,
            smtpPort,
            hasErrors,
            description,
            useTls,
            useSsl,
            smtpUsername,
            errorMessage,
            sortDir
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getEmailAccount(@PathVariable String id) {
        return emailAccountGetService.getEmailAccount(id);
    }

    /**
     * Delete a single email account by obfuscated ID
     *
     * @param id The obfuscated email account ID
     * @return ResponseEntity with ApiResponse containing list of deleted IDs
     *
     * Validation:
     * - Cannot delete if the account is set as default
     * - Returns 400 error if account is default
     * - Returns 404 if account not found
     *
     * Response format:
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Email accounts deleted successfully",
     *   "data": {
     *     "deletedIds": ["encoded_id"],
     *     "deletedCount": 1,
     *     "totalRequested": 1
     *   }
     * }
     *
     * Example request:
     * DELETE /api/email-accounts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteEmailAccount(@PathVariable String id) {
        List<String> idList = new ArrayList<>();
        idList.add(id);
        return emailAccountDeleteService.deleteEmailAccounts(idList);
    }

    /**
     * Delete multiple email accounts by batch request
     *
     * @param idList List of obfuscated email account IDs to delete
     * @return ResponseEntity with ApiResponse containing list of deleted IDs
     *
     * Validation:
     * - If ANY account in the list is set as default, NO accounts will be deleted
     * - Returns 400 error if any account is default
     *
     * Response format (success):
     * {
     *   "success": true,
     *   "statusCode": 200,
     *   "message": "Email accounts deleted successfully",
     *   "data": {
     *     "deletedIds": ["encoded_id_1", "encoded_id_2"],
     *     "deletedCount": 2,
     *     "totalRequested": 3
     *   }
     * }
     *
     * Response format (error - has default accounts):
     * {
     *   "success": false,
     *   "statusCode": 400,
     *   "message": "Cannot delete accounts: some are set as default",
     *   "errorCode": "CANNOT_DELETE_DEFAULT_ACCOUNTS",
     *   "data": {
     *     "deletedIds": [],
     *     "message": "Cannot delete any accounts: 1 account(s) in the list are set as default...",
     *     "defaultAccountIds": [5]
     *   }
     * }
     *
     * Example request:
     * DELETE /api/email-accounts
     * [
     *   "encoded_id_1",
     *   "encoded_id_2",
     *   "encoded_id_3"
     * ]
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> deleteEmailAccountsBatch(@RequestBody List<String> idList) {
        return emailAccountDeleteService.deleteEmailAccounts(idList);
    }
}
