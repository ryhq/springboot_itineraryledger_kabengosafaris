package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.Services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailAccountRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.ModalEntity.EmailAccount;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.EmailMessageRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.MuteRuleRepository;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.CreateMuteRuleDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.DTOs.MuteRuleDTO;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailMessage;
import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.MuteRule;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import com.itineraryledger.kabengosafaris.Security.IdObfuscator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MuteRuleService {

    private final MuteRuleRepository muteRuleRepository;
    private final EmailMessageRepository emailMessageRepository;
    private final EmailAccountRepository emailAccountRepository;
    private final IdObfuscator idObfuscator;

    public List<MuteRule> getActiveRules(Long accountId) {
        return muteRuleRepository.findByEmailAccountIdAndIsActiveTrue(accountId);
    }

    public ResponseEntity<ApiResponse<?>> list(String accountIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            List<MuteRuleDTO> dtos = muteRuleRepository
                .findByEmailAccountIdOrderByCreatedAtAsc(accountId)
                .stream().map(this::toDTO).toList();
            return ResponseEntity.ok(ApiResponse.success(200, "Mute rules retrieved", dtos));
        } catch (Exception e) {
            log.error("Error listing mute rules", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to list mute rules", "LIST_MUTE_RULES_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> create(String accountIdObfuscated, CreateMuteRuleDTO dto) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            EmailAccount account = emailAccountRepository.findById(accountId).orElse(null);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Email account not found", "EMAIL_ACCOUNT_NOT_FOUND"));
            }
            MuteRule saved = muteRuleRepository.save(MuteRule.builder()
                .emailAccount(account)
                .name(dto.getName().trim())
                .matchField(dto.getMatchField())
                .matchMode(dto.getMatchMode() == null ? MuteRule.MatchMode.CONTAINS : dto.getMatchMode())
                .matchPattern(dto.getMatchPattern())
                .isActive(true)
                .build());
            return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(201, "Mute rule created", toDTO(saved)));
        } catch (Exception e) {
            log.error("Error creating mute rule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to create mute rule", "CREATE_MUTE_RULE_FAILED"));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponse<?>> delete(String accountIdObfuscated, String ruleIdObfuscated) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long ruleId = idObfuscator.decodeId(ruleIdObfuscated);
            MuteRule rule = muteRuleRepository.findById(ruleId).orElse(null);
            if (rule == null || !rule.getEmailAccount().getId().equals(accountId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ApiResponse.error(404, "Mute rule not found", "MUTE_RULE_NOT_FOUND"));
            }
            muteRuleRepository.delete(rule);
            return ResponseEntity.ok(ApiResponse.success(200, "Mute rule deleted", null));
        } catch (Exception e) {
            log.error("Error deleting mute rule", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to delete mute rule", "DELETE_MUTE_RULE_FAILED"));
        }
    }

    /**
     * §7 — count messages in `folderId` received on `date` that matched
     * any active mute rule. Returns a small payload with the count and up
     * to 5 sample message ids the inbox can offer to expand.
     */
    public ResponseEntity<ApiResponse<?>> getMutedSummary(String accountIdObfuscated, String folderIdObfuscated, LocalDate date) {
        try {
            Long accountId = idObfuscator.decodeId(accountIdObfuscated);
            Long folderId = idObfuscator.decodeId(folderIdObfuscated);
            List<MuteRule> rules = getActiveRules(accountId);
            if (rules.isEmpty()) {
                Map<String, Object> empty = new HashMap<>();
                empty.put("count", 0);
                return ResponseEntity.ok(ApiResponse.success(200, "No mute rules", empty));
            }
            LocalDate target = date == null ? LocalDate.now() : date;
            LocalDateTime startOfDay = target.atStartOfDay();
            LocalDateTime endOfDay = target.plusDays(1).atStartOfDay();

            Specification<EmailMessage> spec = Specification.<EmailMessage>unrestricted()
                .and(EmailMessageSpecification.forAccount(accountId))
                .and(EmailMessageSpecification.inFolder(folderId))
                .and(EmailMessageSpecification.sentAfter(startOfDay))
                .and(EmailMessageSpecification.sentBefore(endOfDay))
                .and(EmailMessageSpecification.matchesAnyMuteRule(rules));

            List<EmailMessage> matched = emailMessageRepository.findAll(spec);
            List<String> sampleIds = matched.stream()
                .limit(5)
                .map(m -> idObfuscator.encodeId(m.getId()))
                .toList();

            Map<String, Object> result = new HashMap<>();
            result.put("count", matched.size());
            result.put("label", matched.isEmpty() ? null : "muted notifications");
            result.put("sampleMessageIds", sampleIds);
            return ResponseEntity.ok(ApiResponse.success(200, "Muted summary retrieved", result));
        } catch (Exception e) {
            log.error("Error getting muted summary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(500, "Failed to get muted summary", "MUTED_SUMMARY_FAILED"));
        }
    }

    private MuteRuleDTO toDTO(MuteRule r) {
        return MuteRuleDTO.builder()
            .id(idObfuscator.encodeId(r.getId()))
            .name(r.getName())
            .matchField(r.getMatchField())
            .matchMode(r.getMatchMode())
            .matchPattern(r.getMatchPattern())
            .isActive(r.getIsActive())
            .build();
    }
}
