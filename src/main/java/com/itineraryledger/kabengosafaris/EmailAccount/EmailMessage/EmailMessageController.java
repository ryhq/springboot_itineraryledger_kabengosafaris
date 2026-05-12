package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.ComposeEmailDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.MoveEmailDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailAttachment;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailComposeService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailMessageDeleteService;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailMessageGetService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}/messages")
@Validated
@RequiredArgsConstructor
public class EmailMessageController {

    private final EmailMessageGetService emailMessageGetService;
    private final EmailMessageDeleteService emailMessageDeleteService;
    private final EmailComposeService emailComposeService;

    // =====================================================================
    // List & Get
    // =====================================================================

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<?> getMessages(
            @PathVariable("accountId") String accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String folderId,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isStarred,
            @RequestParam(required = false) Boolean hasAttachments,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromAddress,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime sentBefore,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection
    ) {
        return emailMessageGetService.getMessages(accountId, page, size, folderId,
            isRead, isStarred, hasAttachments, search, fromAddress, subject,
            sentAfter, sentBefore, sortBy, sortDirection);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getMessage(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        return emailMessageGetService.getMessage(accountId, id);
    }

    @GetMapping("/{id}/thread")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getThread(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        return emailMessageGetService.getThread(accountId, id);
    }

    @GetMapping("/{id}/raw")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<byte[]> getRawEml(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        byte[] emlBytes = emailMessageGetService.getRawEml(accountId, id);
        if (emlBytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"email.eml\"")
            .contentType(MediaType.parseMediaType("message/rfc822"))
            .body(emlBytes);
    }

    @GetMapping("/{id}/attachments/{attachId}")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id,
            @PathVariable("attachId") String attachId) {
        byte[] bytes = emailMessageGetService.getAttachmentBytes(accountId, id, attachId);
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }

        EmailAttachment attachment = emailMessageGetService.getAttachmentEntity(accountId, id, attachId);
        String filename = attachment != null ? attachment.getOriginalFileName() : "attachment";
        String mimeType = attachment != null && attachment.getMimeType() != null
            ? attachment.getMimeType() : "application/octet-stream";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType(mimeType))
            .body(bytes);
    }

    // =====================================================================
    // Compose, Reply, Forward
    // =====================================================================

    @PostMapping("/compose")
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> compose(
            @PathVariable("accountId") String accountId,
            @RequestPart("email") @Validated ComposeEmailDTO dto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return emailComposeService.composeAndSend(accountId, dto, attachments);
    }

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> reply(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id,
            @RequestPart("email") @Validated ComposeEmailDTO dto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return emailComposeService.reply(accountId, id, dto, attachments);
    }

    @PostMapping("/{id}/reply-all")
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> replyAll(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id,
            @RequestPart("email") @Validated ComposeEmailDTO dto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return emailComposeService.replyAll(accountId, id, dto, attachments);
    }

    @PostMapping("/{id}/forward")
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> forward(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id,
            @RequestPart("email") @Validated ComposeEmailDTO dto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return emailComposeService.forward(accountId, id, dto, attachments);
    }

    // =====================================================================
    // Drafts
    // =====================================================================

    @PostMapping("/draft")
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> saveDraft(
            @PathVariable("accountId") String accountId,
            @RequestPart("email") @Validated ComposeEmailDTO dto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return emailComposeService.saveDraft(accountId, dto, attachments);
    }

    @PutMapping("/draft/{draftId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> updateDraft(
            @PathVariable("accountId") String accountId,
            @PathVariable("draftId") String draftId,
            @RequestPart("email") @Validated ComposeEmailDTO dto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        return emailComposeService.updateDraft(accountId, draftId, dto, attachments);
    }

    @PostMapping("/draft/{draftId}/send")
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> sendDraft(
            @PathVariable("accountId") String accountId,
            @PathVariable("draftId") String draftId) {
        return emailComposeService.sendDraft(accountId, draftId);
    }

    // =====================================================================
    // Read, Star, Move, Delete
    // =====================================================================

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> toggleRead(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        return emailMessageDeleteService.toggleRead(accountId, id);
    }

    @PutMapping("/{id}/star")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> toggleStar(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        return emailMessageDeleteService.toggleStar(accountId, id);
    }

    @PutMapping("/{id}/move")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> moveMessage(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id,
            @RequestBody @Validated MoveEmailDTO moveDTO) {
        return emailMessageDeleteService.moveMessage(accountId, id, moveDTO.getTargetFolderId());
    }

    @PutMapping("/batch/read")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> batchMarkRead(
            @PathVariable("accountId") String accountId,
            @RequestBody List<String> messageIds,
            @RequestParam(defaultValue = "true") boolean markAsRead) {
        return emailMessageDeleteService.batchMarkRead(accountId, messageIds, markAsRead);
    }

    @PutMapping("/batch/move")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> batchMove(
            @PathVariable("accountId") String accountId,
            @RequestBody List<String> messageIds,
            @RequestParam String targetFolderId) {
        return emailMessageDeleteService.batchMove(accountId, messageIds, targetFolderId);
    }

    // §4 — dedicated archive endpoints. Equivalent to a move to the
    // account's ARCHIVE folder but lets the frontend skip the lookup.

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> archiveMessage(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        return emailMessageDeleteService.archiveMessage(accountId, id);
    }

    @PostMapping("/batch/archive")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> batchArchive(
            @PathVariable("accountId") String accountId,
            @RequestBody List<String> messageIds) {
        return emailMessageDeleteService.batchArchive(accountId, messageIds);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> deleteMessage(
            @PathVariable("accountId") String accountId,
            @PathVariable("id") String id) {
        return emailMessageDeleteService.deleteMessage(accountId, id);
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> batchDelete(
            @PathVariable("accountId") String accountId,
            @RequestBody List<String> messageIds) {
        return emailMessageDeleteService.batchDelete(accountId, messageIds);
    }
}
