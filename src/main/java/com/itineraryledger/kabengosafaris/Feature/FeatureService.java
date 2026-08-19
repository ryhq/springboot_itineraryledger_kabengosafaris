package com.itineraryledger.kabengosafaris.Feature;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Which features this installation has, and whether a given request is allowed to exist.
 *
 * Two sources, and the order matters:
 *
 *  1. **A property wins.** `app.features.translation=false` is how a deployment says "this company
 *     does not have this, and nobody here can turn it on" — right for anything holding third-party
 *     credentials, where a switch in the panel would only produce broken requests.
 *  2. **Otherwise the stored row**, which an administrator can change without a deploy. That is the
 *     point: a company deciding it wants credit notes should not need a release.
 *
 * The answer is cached, because the gate is consulted on every request. Every write invalidates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FeatureService {

    private final FeatureSettingRepository repository;
    private final Environment environment;

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private final AtomicReference<Map<Feature, Boolean>> cached = new AtomicReference<>();

    public void invalidate() {
        cached.set(null);
        log.debug("Feature cache cleared");
    }

    @Transactional(readOnly = true)
    public boolean isEnabled(Feature feature) {
        return state().getOrDefault(feature, feature.isEnabledByDefault());
    }

    /**
     * The feature a path belongs to, or null when it belongs to none.
     *
     * Most requests are for paths no feature claims, so this is the common case and it stays cheap:
     * a handful of string comparisons against a fixed catalogue.
     */
    public Feature featureFor(String path) {
        for (Feature feature : Feature.values()) {
            for (String owned : feature.getPaths()) {
                if (path.equals(owned) || path.startsWith(owned + "/") || MATCHER.match(owned, path)
                    || MATCHER.match(owned + "/**", path)) {
                    return feature;
                }
            }
        }
        return null;
    }

    /** What the panel needs to decide what to show. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> view() {
        Map<Feature, Boolean> state = state();
        List<Map<String, Object>> out = new ArrayList<>();

        for (Feature feature : Feature.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", feature.getKey());
            row.put("label", feature.getLabel());
            row.put("description", feature.getDescription());
            row.put("group", feature.getGroup());
            row.put("enabled", state.getOrDefault(feature, feature.isEnabledByDefault()));
            /*
             * Where the answer came from. A switch the panel cannot change must SAY it cannot be
             * changed here, rather than silently refusing or pretending to save.
             */
            row.put("fixedByDeployment", environment.getProperty(feature.getPropertyName()) != null);
            out.add(row);
        }
        return out;
    }

    @Transactional
    public void set(Feature feature, boolean enabled) {
        FeatureSetting row = repository.findBySettingKey(feature.getKey())
            .orElseGet(() -> FeatureSetting.builder()
                .settingKey(feature.getKey())
                .description(feature.getDescription())
                .category(feature.getGroup())
                .build());

        row.setSettingValue(Boolean.toString(enabled));
        repository.save(row);
        invalidate();
        log.info("Feature '{}' is now {}", feature.getKey(), enabled ? "on" : "off");
    }

    // ------------------------------------------------------------------ internals

    private Map<Feature, Boolean> state() {
        Map<Feature, Boolean> current = cached.get();
        if (current != null) return current;

        Map<Feature, Boolean> built = new EnumMap<>(Feature.class);
        Map<String, FeatureSetting> stored = new LinkedHashMap<>();
        try {
            repository.findAll().forEach(row -> stored.put(row.getSettingKey(), row));
        } catch (Exception e) {
            /*
             * A database that cannot answer must not turn the whole product off. Defaults are "on",
             * so a broken read degrades to the product everybody had before features existed.
             */
            log.error("Could not read the feature switches — falling back to defaults", e);
        }

        for (Feature feature : Feature.values()) {
            String override = environment.getProperty(feature.getPropertyName());
            if (override != null) {
                built.put(feature, Boolean.parseBoolean(override));
                continue;
            }
            FeatureSetting row = stored.get(feature.getKey());
            built.put(feature, row == null ? feature.isEnabledByDefault() : row.enabled());
        }

        cached.set(built);
        return built;
    }
}
