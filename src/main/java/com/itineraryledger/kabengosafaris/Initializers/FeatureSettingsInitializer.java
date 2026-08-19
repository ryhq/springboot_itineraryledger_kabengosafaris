package com.itineraryledger.kabengosafaris.Initializers;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.itineraryledger.kabengosafaris.Feature.Feature;
import com.itineraryledger.kabengosafaris.Feature.FeatureSetting;
import com.itineraryledger.kabengosafaris.Feature.FeatureSettingRepository;
import com.itineraryledger.kabengosafaris.Feature.FeatureService;
import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A row per feature, so the switches are visible before anybody touches them.
 *
 * Existing rows are left exactly as they are — a company that turned its fleet off must not have it
 * turned back on by a deploy. New features arrive on their default (on), and the log says what this
 * installation currently has, which is the first thing to check when somebody reports a missing page.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureSettingsInitializer implements ApplicationRunner, Ordered {

    private final FeatureSettingRepository repository;
    private final FeatureService featureService;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int created = 0;

        for (Feature feature : Feature.values()) {
            if (repository.findBySettingKey(feature.getKey()).isPresent()) continue;

            repository.save(FeatureSetting.builder()
                .settingKey(feature.getKey())
                .settingValue(Boolean.toString(feature.isEnabledByDefault()))
                .dataType(SettingDataType.BOOLEAN)
                .description(feature.getDescription())
                .category(feature.getGroup())
                .active(true)
                .build());
            created++;
        }

        featureService.invalidate();

        StringBuilder summary = new StringBuilder();
        for (Feature feature : Feature.values()) {
            boolean on = featureService.isEnabled(feature);
            boolean fixed = environment.getProperty(feature.getPropertyName()) != null;
            summary.append(String.format("%n  %-22s %-3s%s", feature.getKey(), on ? "on" : "off",
                fixed ? "  (fixed by configuration)" : ""));
        }
        log.info("Features ({} switch(es) created):{}", created, summary);
    }

    @Override
    public int getOrder() {
        return 26;
    }
}
