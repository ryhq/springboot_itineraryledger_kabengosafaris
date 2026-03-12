package com.itineraryledger.kabengosafaris.NotificationSetting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    Optional<NotificationSetting> findBySettingKey(String settingKey);

    boolean existsBySettingKey(String settingKey);

    List<NotificationSetting> findByActiveTrue();

    List<NotificationSetting> findByCategory(NotificationSetting.Category category);

    List<NotificationSetting> findByCategoryAndActiveTrue(NotificationSetting.Category category);

    List<NotificationSetting> findByIsSystemDefaultTrue();

    void deleteBySettingKeyAndIsSystemDefaultFalse(String settingKey);
}
