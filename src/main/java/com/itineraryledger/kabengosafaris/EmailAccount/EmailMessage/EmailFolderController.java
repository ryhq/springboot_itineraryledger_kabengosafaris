package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateFolderDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.EmailFolderService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}/folders")
@Validated
@RequiredArgsConstructor
public class EmailFolderController {

    private final EmailFolderService emailFolderService;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getFolders(@PathVariable("accountId") String accountId) {
        return emailFolderService.getFolders(accountId);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CREATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> createFolder(
            @PathVariable("accountId") String accountId,
            @RequestBody @Validated CreateFolderDTO dto) {
        return emailFolderService.createFolder(accountId, dto.getName());
    }

    @PutMapping("/{folderId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> renameFolder(
            @PathVariable("accountId") String accountId,
            @PathVariable("folderId") String folderId,
            @RequestBody @Validated CreateFolderDTO dto) {
        return emailFolderService.renameFolder(accountId, folderId, dto.getName());
    }

    @DeleteMapping("/{folderId}")
    @PreAuthorize("hasAuthority('PERM_DELETE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> deleteFolder(
            @PathVariable("accountId") String accountId,
            @PathVariable("folderId") String folderId) {
        return emailFolderService.deleteFolder(accountId, folderId);
    }
}
