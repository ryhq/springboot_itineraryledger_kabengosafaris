package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailAttachmentRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailFolderRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailAttachment;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolder;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailFolderType;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailMessageDeleteService {

    private final EmailMessageRepository emailMessageRepository;
    private final EmailFolderRepository emailFolderRepository;
    private final EmailAttachmentRepository emailAttachmentRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final EmailStorageService emailStorageService;
    private final IdObfuscator idObfuscator;

    /**
     * Delete a message — moves to trash first, permanently deletes if already in trash
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> deleteMessage(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }

            if (message.getFolder().getType() == EmailFolderType.TRASH) {
                // Already in trash — permanently delete
                return permanentlyDelete(message, accountId);
            } else {
                // Move to trash
                return moveToTrash(message, accountId);
            }
        } catch (Exception e) {
            log.error("Error deleting message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete message", "DELETE_MESSAGE_FAILED"));
        }
    }

    /**
     * Batch delete messages
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> batchDelete(String accountIdObfuscated, List<String> messageIdsObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            if (!emailAccountRepository.existsById(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }

            int trashed = 0;
            int deleted = 0;

            for (String idObs : messageIdsObfuscated) {
                Long msgId = idObfuscator.decodeId(idObs);
                EmailMessage message = emailMessageRepository.findById(msgId).orElse(null);
                if (message == null || !message.getEmailAccount().getId().equals(accountId)) continue;

                if (message.getFolder().getType() == EmailFolderType.TRASH) {
                    permanentlyDeleteInternal(message, accountId);
                    deleted++;
                } else {
                    moveToTrashInternal(message, accountId);
                    trashed++;
                }
            }

            return ResponseEntity.ok(ApiResponse.success(200,
                "Batch delete completed: " + trashed + " trashed, " + deleted + " permanently deleted", null));
        } catch (Exception e) {
            log.error("Error batch deleting messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to batch delete messages", "BATCH_DELETE_FAILED"));
        }
    }

    /**
     * Move a message to a target folder
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> moveMessage(String accountIdObfuscated, String messageIdObfuscated, String targetFolderIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);
            Long targetFolderId = idObfuscator.decodeId(targetFolderIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }

            EmailFolder targetFolder = emailFolderRepository.findById(targetFolderId).orElse(null);
            if (targetFolder == null || !targetFolder.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Target folder not found", "FOLDER_NOT_FOUND"));
            }

            EmailFolder sourceFolder = message.getFolder();

            // Move .eml file on disk
            emailStorageService.moveEmlFile(accountId,
                sourceFolder.getName().toLowerCase(),
                targetFolder.getName().toLowerCase(),
                message.getFileName());

            // Update DB
            message.setFolder(targetFolder);
            message.setStoragePath(targetFolder.getName().toLowerCase());
            emailMessageRepository.save(message);

            // Update folder counts
            emailFolderRepository.incrementMessageCount(sourceFolder.getId(), -1);
            emailFolderRepository.incrementMessageCount(targetFolder.getId(), 1);
            if (!message.getIsRead()) {
                emailFolderRepository.incrementUnreadCount(sourceFolder.getId(), -1);
                emailFolderRepository.incrementUnreadCount(targetFolder.getId(), 1);
            }

            return ResponseEntity.ok(ApiResponse.success(200, "Message moved successfully", null));
        } catch (Exception e) {
            log.error("Error moving message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to move message", "MOVE_MESSAGE_FAILED"));
        }
    }

    /**
     * Toggle read status
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> toggleRead(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }

            boolean wasRead = message.getIsRead();
            message.setIsRead(!wasRead);
            emailMessageRepository.save(message);

            // Update unread count
            int delta = wasRead ? 1 : -1;
            emailFolderRepository.incrementUnreadCount(message.getFolder().getId(), delta);

            return ResponseEntity.ok(ApiResponse.success(200,
                "Message marked as " + (wasRead ? "unread" : "read"), null));
        } catch (Exception e) {
            log.error("Error toggling read status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update read status", "TOGGLE_READ_FAILED"));
        }
    }

    /**
     * Toggle star status
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> toggleStar(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long messageId = idObfuscator.decodeId(messageIdObfuscated);

            EmailMessage message = emailMessageRepository.findById(messageId).orElse(null);
            if (message == null || !message.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Message not found", "MESSAGE_NOT_FOUND"));
            }

            message.setIsStarred(!message.getIsStarred());
            emailMessageRepository.save(message);

            return ResponseEntity.ok(ApiResponse.success(200,
                "Message " + (message.getIsStarred() ? "starred" : "unstarred"), null));
        } catch (Exception e) {
            log.error("Error toggling star status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to update star status", "TOGGLE_STAR_FAILED"));
        }
    }

    /**
     * Batch mark read/unread
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> batchMarkRead(String accountIdObfuscated, List<String> messageIdsObfuscated, boolean markAsRead) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            int updated = 0;

            for (String idObs : messageIdsObfuscated) {
                Long msgId = idObfuscator.decodeId(idObs);
                EmailMessage message = emailMessageRepository.findById(msgId).orElse(null);
                if (message == null || !message.getEmailAccount().getId().equals(accountId)) continue;

                if (message.getIsRead() != markAsRead) {
                    message.setIsRead(markAsRead);
                    emailMessageRepository.save(message);
                    int delta = markAsRead ? -1 : 1;
                    emailFolderRepository.incrementUnreadCount(message.getFolder().getId(), delta);
                    updated++;
                }
            }

            return ResponseEntity.ok(ApiResponse.success(200,
                updated + " messages marked as " + (markAsRead ? "read" : "unread"), null));
        } catch (Exception e) {
            log.error("Error batch marking read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to batch update read status", "BATCH_READ_FAILED"));
        }
    }

    /**
     * Batch move messages
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> batchMove(String accountIdObfuscated, List<String> messageIdsObfuscated, String targetFolderIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long targetFolderId = idObfuscator.decodeId(targetFolderIdObfuscated);

            EmailFolder targetFolder = emailFolderRepository.findById(targetFolderId).orElse(null);
            if (targetFolder == null || !targetFolder.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Target folder not found", "FOLDER_NOT_FOUND"));
            }

            int moved = 0;
            for (String idObs : messageIdsObfuscated) {
                Long msgId = idObfuscator.decodeId(idObs);
                EmailMessage message = emailMessageRepository.findById(msgId).orElse(null);
                if (message == null || !message.getEmailAccount().getId().equals(accountId)) continue;

                EmailFolder sourceFolder = message.getFolder();
                emailStorageService.moveEmlFile(accountId,
                    sourceFolder.getName().toLowerCase(),
                    targetFolder.getName().toLowerCase(),
                    message.getFileName());

                message.setFolder(targetFolder);
                message.setStoragePath(targetFolder.getName().toLowerCase());
                emailMessageRepository.save(message);

                emailFolderRepository.incrementMessageCount(sourceFolder.getId(), -1);
                emailFolderRepository.incrementMessageCount(targetFolder.getId(), 1);
                if (!message.getIsRead()) {
                    emailFolderRepository.incrementUnreadCount(sourceFolder.getId(), -1);
                    emailFolderRepository.incrementUnreadCount(targetFolder.getId(), 1);
                }
                moved++;
            }

            return ResponseEntity.ok(ApiResponse.success(200, moved + " messages moved", null));
        } catch (Exception e) {
            log.error("Error batch moving messages", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to batch move messages", "BATCH_MOVE_FAILED"));
        }
    }

    /**
     * Archive a single message — moves it to the account's ARCHIVE folder.
     * §4 in EMAIL_INBOX_API.md.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> archiveMessage(String accountIdObfuscated, String messageIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailFolder archiveFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.ARCHIVE)
                .orElse(null);
            if (archiveFolder == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Archive folder not found", "ARCHIVE_FOLDER_NOT_FOUND"));
            }
            return moveMessage(accountIdObfuscated, messageIdObfuscated,
                idObfuscator.encodeId(archiveFolder.getId()));
        } catch (Exception e) {
            log.error("Error archiving message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to archive message", "ARCHIVE_MESSAGE_FAILED"));
        }
    }

    /**
     * Archive a batch of messages — convenience wrapper around batchMove
     * targeting the account's ARCHIVE folder so the frontend doesn't need
     * to look the folder id up first.
     */
    @Transactional
    public ResponseEntity<ApiResponse<?>> batchArchive(String accountIdObfuscated, List<String> messageIdsObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailFolder archiveFolder = emailFolderRepository
                .findByEmailAccountIdAndType(accountId, EmailFolderType.ARCHIVE)
                .orElse(null);
            if (archiveFolder == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Archive folder not found", "ARCHIVE_FOLDER_NOT_FOUND"));
            }
            return batchMove(accountIdObfuscated, messageIdsObfuscated,
                idObfuscator.encodeId(archiveFolder.getId()));
        } catch (Exception e) {
            log.error("Error batch archiving", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to batch archive", "BATCH_ARCHIVE_FAILED"));
        }
    }

    // =====================================================================
    // Internal helpers
    // =====================================================================

    private ResponseEntity<ApiResponse<?>> moveToTrash(EmailMessage message, Long accountId) {
        moveToTrashInternal(message, accountId);
        return ResponseEntity.ok(ApiResponse.success(200, "Message moved to trash", null));
    }

    private void moveToTrashInternal(EmailMessage message, Long accountId) {
        EmailFolder trashFolder = emailFolderRepository
            .findByEmailAccountIdAndType(accountId, EmailFolderType.TRASH)
            .orElse(null);

        if (trashFolder == null) {
            log.error("No trash folder found for account {}", accountId);
            return;
        }

        EmailFolder sourceFolder = message.getFolder();

        emailStorageService.moveEmlFile(accountId,
            sourceFolder.getName().toLowerCase(),
            trashFolder.getName().toLowerCase(),
            message.getFileName());

        message.setFolder(trashFolder);
        message.setStoragePath(trashFolder.getName().toLowerCase());
        emailMessageRepository.save(message);

        emailFolderRepository.incrementMessageCount(sourceFolder.getId(), -1);
        emailFolderRepository.incrementMessageCount(trashFolder.getId(), 1);
        if (!message.getIsRead()) {
            emailFolderRepository.incrementUnreadCount(sourceFolder.getId(), -1);
            emailFolderRepository.incrementUnreadCount(trashFolder.getId(), 1);
        }
    }

    private ResponseEntity<ApiResponse<?>> permanentlyDelete(EmailMessage message, Long accountId) {
        permanentlyDeleteInternal(message, accountId);
        return ResponseEntity.ok(ApiResponse.success(200, "Message permanently deleted", null));
    }

    void permanentlyDeleteInternal(EmailMessage message, Long accountId) {
        EmailFolder folder = message.getFolder();

        // Delete attachments from disk
        List<EmailAttachment> attachments = emailAttachmentRepository.findByEmailMessageId(message.getId());
        for (EmailAttachment att : attachments) {
            emailStorageService.deleteAttachment(accountId, att.getFileName());
        }

        // Delete .eml from disk
        emailStorageService.deleteEmlFile(accountId, message.getStoragePath(), message.getFileName());

        // Update folder counts
        emailFolderRepository.incrementMessageCount(folder.getId(), -1);
        if (!message.getIsRead()) {
            emailFolderRepository.incrementUnreadCount(folder.getId(), -1);
        }

        // Delete from DB (cascades to attachments)
        emailMessageRepository.delete(message);
    }
}
