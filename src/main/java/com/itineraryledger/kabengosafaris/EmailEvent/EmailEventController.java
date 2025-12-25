package com.itineraryledger.kabengosafaris.EmailEvent;

import com.itineraryledger.kabengosafaris.EmailEvent.DTOs.UpdateEmailEventDTO;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailEventGetService;
import com.itineraryledger.kabengosafaris.EmailEvent.Services.EmailEventUpdateService;
import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Email Event management endpoints
 *
 * Provides endpoints for:
 * - Retrieving email events (system-wide notification events)
 * - Updating event settings (description, enabled status)
 *
 * Note: Email events cannot be created or deleted via API - they are system-defined
 * and initialized on application startup.
 */
@RestController
@RequestMapping("/api/email-events")
@RequiredArgsConstructor
public class EmailEventController {

    private final EmailEventGetService emailEventGetService;
    private final EmailEventUpdateService emailEventUpdateService;

    /**
     * Get all email events
     *
     * @return ResponseEntity with list of all email events
     *
     * Example request:
     * GET /api/email-events
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Email events retrieved successfully",
     *   "data": [
     *     {
     *       "id": "encoded_id",
     *       "name": "USER_REGISTRATION",
     *       "description": "Sent when a new user registers",
     *       "enabled": true,
     *       "templateCount": 2,
     *       "hasSystemDefaultTemplate": true,
     *       "createdAt": "2025-01-15T10:30:00",
     *       "updatedAt": "2025-01-15T10:30:00"
     *     }
     *   ]
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllEmailEvents() {
        return emailEventGetService.getAllEmailEvents();
    }

    /**
     * Get email event by ID
     *
     * @param eventId The obfuscated email event ID
     * @return ResponseEntity with email event details
     *
     * Example request:
     * GET /api/email-events/{eventId}
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Email event retrieved successfully",
     *   "data": {
     *     "id": "encoded_id",
     *     "name": "USER_REGISTRATION",
     *     "description": "Sent when a new user registers",
     *     "enabled": true,
     *     "templateCount": 2,
     *     "hasSystemDefaultTemplate": true,
     *     "createdAt": "2025-01-15T10:30:00",
     *     "updatedAt": "2025-01-15T10:30:00"
     *   }
     * }
     */
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<?>> getEmailEventById(@PathVariable String eventId) {
        return emailEventGetService.getEmailEventById(eventId);
    }

    /**
     * Update email event settings
     *
     * Only description and enabled status can be updated.
     * The event name cannot be changed as it's used as a system identifier.
     *
     * @param eventId The obfuscated email event ID
     * @param updateDTO The update DTO with fields to modify
     * @return ResponseEntity with updated email event
     *
     * Example request:
     * PUT /api/email-events/{eventId}
     * {
     *   "description": "Updated description for user registration emails",
     *   "enabled": false
     * }
     *
     * Example response:
     * {
     *   "status": 200,
     *   "message": "Email event updated successfully",
     *   "data": {
     *     "id": "encoded_id",
     *     "name": "USER_REGISTRATION",
     *     "description": "Updated description for user registration emails",
     *     "enabled": false,
     *     "templateCount": 2,
     *     "hasSystemDefaultTemplate": true,
     *     "createdAt": "2025-01-15T10:30:00",
     *     "updatedAt": "2025-01-15T12:45:00"
     *   }
     * }
     */
    @PutMapping("/{eventId}")
    public ResponseEntity<ApiResponse<?>> updateEmailEvent(
            @PathVariable String eventId,
            @RequestBody UpdateEmailEventDTO updateDTO) {
        return emailEventUpdateService.updateEmailEvent(eventId, updateDTO);
    }
}
