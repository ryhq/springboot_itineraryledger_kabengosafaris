package com.itineraryledger.kabengosafaris.AuditLog;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogDTO {

    private String id;
    private String name;
    private String userId;
    private String username;
    private String action;
    private String entityType;
    private String entityId;
    private String description;
    private String oldValues;
    private String newValues;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private String status;
    private String errorMessage;
}
