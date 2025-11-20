package com.itineraryledger.kabengosafaris.AuditLog;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.itineraryledger.kabengosafaris.AuditLog.AuditLogAnnotation)")
    public Object auditLog(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        AuditLogAnnotation annotation = method.getAnnotation(AuditLogAnnotation.class);

        String action = annotation.action();
        String entityType = annotation.entityType();
        String description = annotation.description();

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String username = "SYSTEM";

        if (authentication != null && authentication.isAuthenticated() &&
            !authentication.getPrincipal().equals("anonymousUser")) {
            username = authentication.getName();
            Object principal = authentication.getPrincipal();
            if (principal instanceof com.itineraryledger.kabengosafaris.User.User) {
                userId = ((com.itineraryledger.kabengosafaris.User.User) principal).getId();
            }
        }

        // Get request context
        String ipAddress = "N/A";
        String userAgent = "N/A";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = getClientIpAddress(request);
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            log.debug("Could not extract request context for audit logging", e);
        }

        // Extract entity ID if specified
        Long entityId = null;
        if (!annotation.entityIdParamName().isEmpty()) {
            entityId = extractEntityId(joinPoint, annotation.entityIdParamName());
        }

        // Capture old values
        String oldValues = null;
        try {
            oldValues = serializeArguments(joinPoint.getArgs(), methodSignature.getParameterNames());
        } catch (Exception e) {
            log.debug("Could not serialize old values for audit log", e);
        }

        String status = "SUCCESS";
        String errorMessage = null;
        String newValues = null;

        try {
            // Execute the actual method
            Object result = joinPoint.proceed();

            // Capture new values (result)
            try {
                newValues = objectMapper.writeValueAsString(result);
            } catch (Exception e) {
                log.debug("Could not serialize new values for audit log", e);
            }

            return result;

        } catch (Exception e) {
            status = "FAILURE";
            errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.error("Error in audited method: {} - {}", action, errorMessage, e);
            throw e;

        } finally {
            // Save audit log
            try {
                AuditLog auditLog = AuditLog.builder()
                        .userId(userId)
                        .username(username)
                        .action(action)
                        .entityType(entityType)
                        .entityId(entityId)
                        .description(description)
                        .oldValues(oldValues)
                        .newValues(newValues)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .status(status)
                        .errorMessage(errorMessage)
                        .build();

                auditLogService.logAction(auditLog);

            } catch (Exception e) {
                log.error("Failed to save audit log for action: {}", action, e);
            }
        }
    }

    private Long extractEntityId(ProceedingJoinPoint joinPoint, String paramName) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] parameterNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(paramName)) {
                    Object arg = args[i];
                    if (arg instanceof Long) {
                        return (Long) arg;
                    } else if (arg instanceof Integer) {
                        return ((Integer) arg).longValue();
                    } else if (arg instanceof String) {
                        try {
                            return Long.parseLong((String) arg);
                        } catch (NumberFormatException e) {
                            log.debug("Could not parse entity ID from string: {}", arg);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error extracting entity ID", e);
        }
        return null;
    }

    private String serializeArguments(Object[] args, String[] paramNames) throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            if (paramNames != null && i < paramNames.length) {
                // Skip password and sensitive fields
                if (!isSensitiveField(paramNames[i])) {
                    arguments.put(paramNames[i], args[i]);
                }
            }
        }
        return arguments.isEmpty() ? null : objectMapper.writeValueAsString(arguments);
    }

    private boolean isSensitiveField(String fieldName) {
        String lowerCaseName = fieldName.toLowerCase();
        return lowerCaseName.contains("password") ||
               lowerCaseName.contains("token") ||
               lowerCaseName.contains("secret") ||
               lowerCaseName.contains("apikey") ||
               lowerCaseName.contains("creditcard");
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

}
