package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateMuteRuleDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services.MuteRuleService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/email-accounts/{accountId}")
@Validated
@RequiredArgsConstructor
public class MuteRuleController {

    private final MuteRuleService muteRuleService;

    @GetMapping("/mute-rules")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> listRules(@PathVariable("accountId") String accountId) {
        return muteRuleService.list(accountId);
    }

    @PostMapping("/mute-rules")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> createRule(
            @PathVariable("accountId") String accountId,
            @RequestBody @Validated CreateMuteRuleDTO dto) {
        return muteRuleService.create(accountId, dto);
    }

    @DeleteMapping("/mute-rules/{ruleId}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> deleteRule(
            @PathVariable("accountId") String accountId,
            @PathVariable("ruleId") String ruleId) {
        return muteRuleService.delete(accountId, ruleId);
    }

    @GetMapping("/folders/{folderId}/muted-summary")
    @PreAuthorize("hasAuthority('PERM_READ_EMAIL_MESSAGE')")
    public ResponseEntity<ApiResponse<?>> getMutedSummary(
            @PathVariable("accountId") String accountId,
            @PathVariable("folderId") String folderId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return muteRuleService.getMutedSummary(accountId, folderId, date);
    }
}
