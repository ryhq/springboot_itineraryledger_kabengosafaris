package com.itineraryledger.kabengosafaris.Feature;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureSettingRepository extends JpaRepository<FeatureSetting, Long> {

    Optional<FeatureSetting> findBySettingKey(String settingKey);

    List<FeatureSetting> findAllByOrderByCategoryAscSettingKeyAsc();
}
