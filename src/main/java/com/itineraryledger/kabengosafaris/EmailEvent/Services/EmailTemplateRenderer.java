package com.itineraryledger.kabengosafaris.EmailEvent.Services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailEventRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.EmailTemplateRepository;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailEvent;
import com.itineraryledger.kabengosafaris.EmailEvent.ModalEntity.EmailTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service for rendering email templates by replacing variable placeholders with actual values.
 *
 * This service:
 * 1. Loads the default enabled template for a given email event
 * 2. Validates that all required variables (defined in EmailEvent.variablesJson) are provided
 * 3. Replaces {{variableName}} placeholders in the HTML with actual values
 * 4. Returns the rendered HTML ready for sending
 *
 * Example:
 * - Template: "Hello {{username}}, click {{activationLink}}"
 * - Variables: {"username": "john_doe", "activationLink": "https://..."}
 * - Result: "Hello john_doe, click https://..."
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateRenderer {

    private final EmailEventRepository emailEventRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailTemplateService emailTemplateService;

    /**
     * Render a template with actual variable values
     *
     * @param eventName The email event name (e.g., "USER_REGISTRATION")
     * @param variables Map of variable names to their values
     * @return Rendered HTML with all {{variableName}} placeholders replaced
     * @throws IllegalArgumentException if event not found, disabled, no template, or missing required variables
     */
    public String renderTemplate(String eventName, Map<String, String> variables) {
        log.debug("Rendering template for event: {} with {} variables", eventName, variables.size());

        // 1. Get the email event
        EmailEvent event = emailEventRepository.findByName(eventName)
            .orElseThrow(() -> new IllegalArgumentException("Email event not found: " + eventName));

        if (!event.getEnabled()) {
            throw new IllegalStateException("Email event is disabled: " + eventName);
        }

        // 2. Get default enabled template for this event
        EmailTemplate template = getDefaultEnabledTemplate(event.getId());

        // 3. Load template content from file
        String htmlContent = emailTemplateService.readTemplateFile(template.getFileName());

        // 4. Validate that all required variables are provided
        validateRequiredVariables(event.getVariablesJson(), variables);

        // 5. Replace {{variableName}} placeholders with actual values
        String renderedHtml = replacePlaceholders(htmlContent, variables, event.getVariablesJson());

        log.info("Successfully rendered template for event: {}", eventName);
        return renderedHtml;
    }

    /**
     * Get the default enabled template for an event
     */
    private EmailTemplate getDefaultEnabledTemplate(Long eventId) {
        return emailTemplateRepository.findAll(
            EmailTemplateSpecification.emailEventId(eventId)
                .and(EmailTemplateSpecification.isDefault(true))
                .and(EmailTemplateSpecification.enabled(true))
        ).stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                "No default enabled template found for event ID: " + eventId
            ));
    }

    /**
     * Replace {{variableName}} placeholders with actual values
     *
     * @param html The HTML template content
     * @param providedVariables Variables provided by the caller
     * @param variablesJson JSON string defining expected variables
     * @return HTML with placeholders replaced
     */
    private String replacePlaceholders(String html, Map<String, String> providedVariables, String variablesJson) {
        String result = html;

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> varDefs = mapper.readValue(
                variablesJson,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            // Replace each variable placeholder
            for (Map<String, Object> varDef : varDefs) {
                String varName = (String) varDef.get("name");
                String placeholder = "{{" + varName + "}}";

                // Get actual value from provided variables, or use default value
                String value = providedVariables.get(varName);
                if (value == null && varDef.containsKey("defaultValue")) {
                    value = (String) varDef.get("defaultValue");
                }

                // Replace placeholder (if value is still null, replace with empty string)
                if (value != null) {
                    result = result.replace(placeholder, value);
                } else {
                    result = result.replace(placeholder, "");
                }
            }

        } catch (Exception e) {
            log.error("Error replacing placeholders in template", e);
            throw new RuntimeException("Failed to render template: " + e.getMessage(), e);
        }

        return result;
    }

    /**
     * Validate that all required variables are provided
     *
     * @param variablesJson JSON string defining required variables
     * @param providedVars Variables provided by the caller
     * @throws IllegalArgumentException if any required variable is missing
     */
    private void validateRequiredVariables(String variablesJson, Map<String, String> providedVars) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> varDefs = mapper.readValue(
                variablesJson,
                new TypeReference<List<Map<String, Object>>>() {}
            );

            List<String> missingVars = new ArrayList<>();

            for (Map<String, Object> varDef : varDefs) {
                Boolean isRequired = (Boolean) varDef.getOrDefault("isRequired", false);
                String varName = (String) varDef.get("name");

                // Check if required variable is provided
                if (Boolean.TRUE.equals(isRequired)) {
                    String value = providedVars.get(varName);
                    if (value == null || value.isBlank()) {
                        missingVars.add(varName);
                    }
                }
            }

            if (!missingVars.isEmpty()) {
                throw new IllegalArgumentException(
                    "Missing required variables: " + String.join(", ", missingVars)
                );
            }

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating variables", e);
            throw new RuntimeException("Failed to validate variables: " + e.getMessage(), e);
        }
    }

    /**
     * Get the list of variables defined for an email event
     * Useful for services that need to know what variables to provide
     *
     * @param eventName The email event name
     * @return List of variable definitions
     */
    public List<Map<String, Object>> getEventVariables(String eventName) {
        EmailEvent event = emailEventRepository.findByName(eventName)
            .orElseThrow(() -> new IllegalArgumentException("Email event not found: " + eventName));

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(
                event.getVariablesJson(),
                new TypeReference<List<Map<String, Object>>>() {}
            );
        } catch (Exception e) {
            log.error("Error parsing event variables", e);
            return new ArrayList<>();
        }
    }
}
