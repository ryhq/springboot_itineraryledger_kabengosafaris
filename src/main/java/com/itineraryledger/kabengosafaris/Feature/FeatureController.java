package com.itineraryledger.kabengosafaris.Feature;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.itineraryledger.kabengosafaris.Response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * What this installation has.
 *
 * Read by every signed-in user, because the panel builds its sidebar, command palette and breadcrumbs
 * from the answer — a nav entry leading to a 404 is worse than no entry. Nothing here is sensitive:
 * it is a list of which parts of the product this company bought.
 */
@RestController
@RequestMapping("/api/features")
@RequiredArgsConstructor
@Slf4j
public class FeatureController {

    private final FeatureService featureService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<?>> getFeatures() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("features", featureService.view());
        return ResponseEntity.ok(ApiResponse.success(200, "Features retrieved successfully", payload));
    }

    @PutMapping("/{key}")
    @PreAuthorize("hasAuthority('PERM_UPDATE_FEATURE')")
    public ResponseEntity<ApiResponse<?>> setFeature(@PathVariable String key,
                                                     @RequestBody Map<String, Object> body) {
        Feature feature = Feature.byKey(key);
        if (feature == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "There is no feature called '" + key + "'", "NOT_FOUND"));
        }

        Object value = body.get("enabled") != null ? body.get("enabled") : body.get("settingValue");
        if (value == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "Send {\"enabled\": true|false}", "VALIDATION_ERROR"));
        }

        boolean enabled = Boolean.parseBoolean(String.valueOf(value));

        /*
         * A deployment-fixed feature cannot be changed here. Refusing loudly beats saving a row that
         * the property will keep overruling — which would look like the switch is broken.
         */
        boolean fixed = featureService.view().stream()
            .anyMatch(row -> feature.getKey().equals(row.get("key")) && Boolean.TRUE.equals(row.get("fixedByDeployment")));
        if (fixed) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(409,
                feature.getLabel() + " is fixed by this deployment's configuration and cannot be changed here.",
                "FEATURE_FIXED"));
        }

        featureService.set(feature, enabled);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("features", featureService.view());
        return ResponseEntity.ok(ApiResponse.success(200,
            feature.getLabel() + (enabled ? " enabled" : " disabled"), payload));
    }
}
