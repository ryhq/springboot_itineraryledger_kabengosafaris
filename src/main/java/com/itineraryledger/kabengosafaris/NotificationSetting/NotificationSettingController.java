package com.itineraryledger.kabengosafaris.NotificationSetting;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notification-settings")
@Slf4j
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationSettingServices notificationSettingServices;

    @GetMapping
    @PreAuthorize("hasAuthority('PERM_READ_NOTIFICATION_SETTING')")
    public ResponseEntity<ApiResponse<?>> getAllSettings() {
        log.info("GET /api/notification-settings - Fetching all notification settings");
        return notificationSettingServices.getAllSettings();
    }

    @GetMapping("/category/{category}")
    @PreAuthorize("hasAuthority('PERM_READ_NOTIFICATION_SETTING')")
    public ResponseEntity<ApiResponse<?>> getSettingsByCategory(@PathVariable NotificationSetting.Category category) {
        log.info("GET /api/notification-settings/category/{} - Fetching notification settings by category", category);
        return notificationSettingServices.getSettingsByCategory(category);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('PERM_READ_NOTIFICATION_SETTING')")
    public ResponseEntity<ApiResponse<?>> getActiveSettings() {
        log.info("GET /api/notification-settings/active - Fetching active notification settings");
        return notificationSettingServices.getActiveSettings();
    }

    @GetMapping("/{settingKey}")
    @PreAuthorize("hasAuthority('PERM_READ_NOTIFICATION_SETTING')")
    public ResponseEntity<ApiResponse<?>> getSettingByKey(@PathVariable String settingKey) {
        log.info("GET /api/notification-settings/{} - Fetching notification setting by key", settingKey);
        return notificationSettingServices.getSettingByKey(settingKey);
    }

    @PutMapping("/{settingKey}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_NOTIFICATION_SETTING')")
    public ResponseEntity<ApiResponse<?>> updateSetting(
            @PathVariable String settingKey,
            @RequestBody UpdateNotificationSettingDTO updateDTO) {
        log.info("PUT /api/notification-settings/{} - Updating notification setting", settingKey);
        return notificationSettingServices.updateSetting(settingKey, updateDTO);
    }

    @PostMapping("/{settingKey}/reset")
    @PreAuthorize("hasAuthority('PERM_UPDATE_NOTIFICATION_SETTING')")
    public ResponseEntity<ApiResponse<?>> resetSetting(@PathVariable String settingKey) {
        log.info("POST /api/notification-settings/{}/reset - Resetting notification setting", settingKey);
        return notificationSettingServices.resetSetting(settingKey);
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> health() {
        return ResponseEntity.ok(
                ApiResponse.success(200, "Notification Settings API is healthy", null)
        );
    }
}
