package com.itineraryledger.kabengosafaris.Health;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Which build is running here, and for whom.
 *
 * With one deployment you know what is running because you just pushed it. With one per company the
 * question "is anybody behind?" needs an answer that is not a guess — and it has to be answerable
 * without a login, because the thing asking is usually a deploy script or a monitor.
 *
 * Deliberately thin: a version, a commit, a build time and the company's name. No dependency
 * versions, no paths, no configuration — an unauthenticated endpoint should not be a map of the box.
 */
@RestController
@RequestMapping("/api/public/version")
@RequiredArgsConstructor
public class VersionController {

    /** absent when the jar was assembled without the build-info goal (an IDE run, usually) */
    private final Optional<BuildProperties> buildProperties;

    @Value("${app.company.name:}")
    private String companyName;

    @GetMapping
    public ResponseEntity<Map<String, Object>> version() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("company", companyName);

        buildProperties.ifPresentOrElse(build -> {
            payload.put("version", build.getVersion());
            payload.put("commit", build.get("sha") == null ? "unknown" : build.get("sha"));
            payload.put("builtAt", build.getTime() == null ? null : build.getTime().toString());
        }, () -> {
            payload.put("version", "unknown");
            payload.put("commit", "unknown");
            payload.put("builtAt", null);
        });

        return ResponseEntity.ok(payload);
    }
}
