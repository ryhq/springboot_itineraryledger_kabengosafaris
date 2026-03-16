package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.EmailFolderDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolder;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailFolderService {

    private final EmailFolderRepository emailFolderRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailStorageService emailStorageService;
    private final IdObfuscator idObfuscator;

    /**
     * Create system default folders for an email account
     */
    @Transactional
    public void createSystemFolders(EmailAccount account) {
        createSystemFolder(account, "Inbox", EmailFolderType.INBOX, "INBOX");
        createSystemFolder(account, "Sent", EmailFolderType.SENT, "Sent");
        createSystemFolder(account, "Drafts", EmailFolderType.DRAFTS, "Drafts");
        createSystemFolder(account, "Trash", EmailFolderType.TRASH, "Trash");
        createSystemFolder(account, "Archive", EmailFolderType.ARCHIVE, "Archive");

        // Initialize disk directories
        emailStorageService.initializeAccountDirectories(account.getId());

        log.info("Created system folders for email account: {}", account.getEmail());
    }

    private void createSystemFolder(EmailAccount account, String name, EmailFolderType type, String remoteFolderName) {
        if (!emailFolderRepository.existsByEmailAccountIdAndName(account.getId(), name)) {
            EmailFolder folder = EmailFolder.builder()
                .emailAccount(account)
                .name(name)
                .type(type)
                .isSystem(true)
                .messageCount(0)
                .unreadCount(0)
                .remoteFolderName(remoteFolderName)
                .build();
            emailFolderRepository.save(folder);
        }
    }

    /**
     * Get all folders for an account
     */
    public ResponseEntity<ApiResponse<?>> getFolders(String accountIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            if (!emailAccountRepository.existsById(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            List<EmailFolder> folders = emailFolderRepository.findByEmailAccountIdOrderByTypeAsc(accountId);
            List<EmailFolderDTO> dtos = folders.stream().map(this::toDTO).toList();

            return ResponseEntity.ok(ApiResponse.success(200, "Folders retrieved successfully", dtos));
        } catch (Exception e) {
            log.error("Error getting folders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get folders", "GET_FOLDERS_FAILED"));
        }
    }

    /**
     * Create a custom folder
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> createFolder(String accountIdObfuscated, String folderName) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            if (emailFolderRepository.existsByEmailAccountIdAndName(accountId, folderName)) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "Folder name already exists", "DUPLICATE_FOLDER_NAME"));
            }

            EmailFolder folder = EmailFolder.builder()
                .emailAccount(account)
                .name(folderName)
                .type(EmailFolderType.CUSTOM)
                .isSystem(false)
                .messageCount(0)
                .unreadCount(0)
                .build();
            folder = emailFolderRepository.save(folder);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Folder created successfully", toDTO(folder)));
        } catch (Exception e) {
            log.error("Error creating folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create folder", "CREATE_FOLDER_FAILED"));
        }
    }

    /**
     * Rename a custom folder
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> renameFolder(String accountIdObfuscated, String folderIdObfuscated, String newName) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long folderId = idObfuscator.decodeId(folderIdObfuscated);

            EmailFolder folder = emailFolderRepository.findById(folderId).orElse(null);
            if (folder == null || !folder.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Folder not found", "FOLDER_NOT_FOUND"));
            }

            if (folder.getIsSystem()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "System folders cannot be renamed", "SYSTEM_FOLDER"));
            }

            folder.setName(newName);
            folder = emailFolderRepository.save(folder);

            return ResponseEntity.ok(ApiResponse.success(200, "Folder renamed successfully", toDTO(folder)));
        } catch (Exception e) {
            log.error("Error renaming folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to rename folder", "RENAME_FOLDER_FAILED"));
        }
    }

    /**
     * Delete a custom folder
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteFolder(String accountIdObfuscated, String folderIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long folderId = idObfuscator.decodeId(folderIdObfuscated);

            EmailFolder folder = emailFolderRepository.findById(folderId).orElse(null);
            if (folder == null || !folder.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Folder not found", "FOLDER_NOT_FOUND"));
            }

            if (folder.getIsSystem()) {
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(400, "System folders cannot be deleted", "SYSTEM_FOLDER"));
            }

            emailFolderRepository.delete(folder);

            return ResponseEntity.ok(ApiResponse.success(200, "Folder deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting folder", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete folder", "DELETE_FOLDER_FAILED"));
        }
    }

    private EmailFolderDTO toDTO(EmailFolder folder) {
        return EmailFolderDTO.builder()
            .id(idObfuscator.encodeId(folder.getId()))
            .name(folder.getName())
            .type(folder.getType())
            .isSystem(folder.getIsSystem())
            .messageCount(folder.getMessageCount())
            .unreadCount(folder.getUnreadCount())
            .remoteFolderName(folder.getRemoteFolderName())
            .build();
    }
}
