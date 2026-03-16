package com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.itineraryledger.kabengosafaris.EmailAccount.EmailMessage.ModalEntity.EmailSetting;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailSettingRepository extends JpaRepository<EmailSetting, Long> {

    Optional<EmailSetting> findBySettingKey(String settingKey);

    @Query("SELECT s FROM EmailSetting s WHERE s.settingKey = :settingKey AND s.active = true")
    Optional<EmailSetting> findActiveBySettingKey(@Param("settingKey") String settingKey);

    @Query("SELECT s FROM EmailSetting s WHERE s.active = true ORDER BY s.category, s.settingKey")
    List<EmailSetting> findAllActive();

    @Query("SELECT s FROM EmailSetting s WHERE s.category = :category AND s.active = true ORDER BY s.settingKey")
    List<EmailSetting> findActiveByCategoryOrderBySettingKeyAsc(@Param("category") EmailSetting.Category category);

    @Query("SELECT s FROM EmailSetting s WHERE s.isSystemDefault = true ORDER BY s.category, s.settingKey")
    List<EmailSetting> findAllSystemDefaults();

    boolean existsBySettingKey(String settingKey);
}
