package com.itineraryledger.kabengosafaris.Backup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSetting;
import com.itineraryledger.kabengosafaris.Backup.BackupSettings.BackupSettingsRepository;
import com.itineraryledger.kabengosafaris.GlobalEnums.SettingDataType;
import com.itineraryledger.kabengosafaris.Initializers.BackupSettingsInitializer;

/**
 * A label that is wrong stays wrong forever, and this one was telling people to leave a field empty.
 *
 * <p>Settings are seeded once and then skipped on every later start, so that nothing a redeploy
 * does can overwrite what an operator typed. Correct, and it froze the <em>description</em> along
 * with the value. The notification address list was described as "backup notifications", which it
 * was when it was written; months later the client portal began sending a customer's acceptance
 * to the same list, and the field went on saying it was about backups.
 *
 * <p>The label is what somebody reads before deciding whether a field concerns them. A stale one
 * is not untidy, it is misleading, and this is the field whose emptiness means nobody hears that
 * a customer has agreed to a quotation.
 *
 * <p>So the description is brought up to date and the value is still never touched. Both halves
 * are held here, and the second is the one that would be expensive to get wrong: a redeploy that
 * reset everybody's settings to the shipped defaults would be a far worse bug than the one being
 * fixed.
 */
class ASettingsLabelCanBeCorrectedWithoutTouchingItsValueTest {

    private BackupSetting existing(String value, String description) {
        BackupSetting setting = BackupSetting.builder()
            .settingKey("backup.notification.emails")
            .settingValue(value)
            .description(description)
            .dataType(SettingDataType.STRING)
            .category(BackupSetting.Category.NOTIFICATION)
            .active(true)
            .build();
        return setting;
    }

    private void seed(BackupSettingsRepository repository, String description) throws Exception {
        BackupSettingsInitializer initializer = new BackupSettingsInitializer(repository);
        Method method = BackupSettingsInitializer.class.getDeclaredMethod("createOrUpdateSetting",
            String.class, String.class, SettingDataType.class, String.class,
            BackupSetting.Category.class, Boolean.class);
        method.setAccessible(true);
        method.invoke(initializer, "backup.notification.emails", "shipped@default.test",
            SettingDataType.STRING, description, BackupSetting.Category.NOTIFICATION, false);
    }

    @Test
    @DisplayName("a reworded label reaches an installation that already has the setting")
    void theDescriptionIsBroughtUpToDate() throws Exception {
        BackupSettingsRepository repository = mock(BackupSettingsRepository.class);
        BackupSetting stored = existing("ops@theirdomain.test", "Email addresses to send backup notifications");
        when(repository.findBySettingKey("backup.notification.emails")).thenReturn(Optional.of(stored));

        seed(repository, "Who hears from the system, including the client portal");

        ArgumentCaptor<BackupSetting> saved = ArgumentCaptor.forClass(BackupSetting.class);
        verify(repository).save(saved.capture());
        assertEquals("Who hears from the system, including the client portal",
            saved.getValue().getDescription(),
            "without this the wording is frozen at whatever it was the day the row was written");
    }

    @Test
    @DisplayName("the operator's own value is never touched by a redeploy")
    void theValueIsLeftAlone() throws Exception {
        BackupSettingsRepository repository = mock(BackupSettingsRepository.class);
        BackupSetting stored = existing("ops@theirdomain.test", "old wording");
        when(repository.findBySettingKey("backup.notification.emails")).thenReturn(Optional.of(stored));

        seed(repository, "new wording");

        ArgumentCaptor<BackupSetting> saved = ArgumentCaptor.forClass(BackupSetting.class);
        verify(repository).save(saved.capture());
        assertEquals("ops@theirdomain.test", saved.getValue().getSettingValue(),
            "a deploy that reset everybody's addresses to the shipped default would be a far "
                + "worse bug than the stale label it was fixing");
    }

    @Test
    @DisplayName("an unchanged label writes nothing at all")
    void nothingToSayMeansNoWrite() throws Exception {
        BackupSettingsRepository repository = mock(BackupSettingsRepository.class);
        BackupSetting stored = existing("ops@theirdomain.test", "the same wording");
        when(repository.findBySettingKey("backup.notification.emails")).thenReturn(Optional.of(stored));

        seed(repository, "the same wording");

        verify(repository, never()).save(any(BackupSetting.class));
    }

    @Test
    @DisplayName("a setting nobody has yet is created with the shipped default")
    void aMissingSettingIsStillCreated() throws Exception {
        BackupSettingsRepository repository = mock(BackupSettingsRepository.class);
        when(repository.findBySettingKey(anyString())).thenReturn(Optional.empty());

        seed(repository, "first wording");

        ArgumentCaptor<BackupSetting> saved = ArgumentCaptor.forClass(BackupSetting.class);
        verify(repository).save(saved.capture());
        assertEquals("shipped@default.test", saved.getValue().getSettingValue());
    }

    /**
     * The label that sent two people to the wrong page.
     *
     * <p>It was first reworded to say the client portal mails here, which was true and was the
     * wrong fix: the portal had no business on a page about backup scheduling and retention in
     * the first place. The portal now has its own entry under Settings > Notification, beside
     * the newsletter and contact-us lists, and this one says plainly that it is backups only and
     * where the other thing lives.
     */
    @Test
    @DisplayName("the backup list says it is backups, and says where the customer alerts are")
    void theLabelSaysWhatItActuallyGoverns() throws IOException {
        String source = Files.readString(Path.of("src/main/java/com/itineraryledger/"
            + "kabengosafaris/Initializers/BackupSettingsInitializer.java"));
        int at = source.indexOf("\"backup.notification.emails\",");
        assertTrue(at > 0, "the setting key has moved");

        String declaration = source.substring(at, at + 900);
        assertTrue(declaration.contains("Backups only"),
            "an unqualified 'notifications' here is what made two people look in the wrong place");
        assertTrue(declaration.contains("Settings > Notification"),
            "say where the customer-facing alerts actually are, or the next person hunts too");
    }
}
