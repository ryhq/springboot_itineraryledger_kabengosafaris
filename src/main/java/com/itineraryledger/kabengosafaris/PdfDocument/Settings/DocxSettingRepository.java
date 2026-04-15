package com.itineraryledger.kabengosafaris.PdfDocument.Settings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocxSettingRepository extends JpaRepository<DocxSetting, Long> {

    Optional<DocxSetting> findBySettingKey(String settingKey);

    @Query("SELECT s FROM DocxSetting s WHERE s.settingKey = :settingKey AND s.active = true")
    Optional<DocxSetting> findActiveBySettingKey(@Param("settingKey") String settingKey);

    @Query("SELECT s FROM DocxSetting s WHERE s.active = true ORDER BY s.category, s.settingKey")
    List<DocxSetting> findAllActive();

    @Query("SELECT s FROM DocxSetting s WHERE s.category = :category AND s.active = true ORDER BY s.settingKey")
    List<DocxSetting> findActiveByCategoryOrderBySettingKeyAsc(@Param("category") DocxSetting.Category category);

    boolean existsBySettingKey(String settingKey);

    @Query("SELECT s FROM DocxSetting s WHERE s.requiresRestart = true AND s.active = true")
    List<DocxSetting> findAllThatRequireRestart();
}
