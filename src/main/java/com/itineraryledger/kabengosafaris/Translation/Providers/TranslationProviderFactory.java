package com.itineraryledger.kabengosafaris.Translation.Providers;

import com.itineraryledger.kabengosafaris.EmailAccount.Components.EncryptionUtil;
import com.itineraryledger.kabengosafaris.Translation.Account.Entity.TranslationAccount;
import com.itineraryledger.kabengosafaris.Translation.Account.TranslationAccountRepository;
import com.itineraryledger.kabengosafaris.Translation.Settings.TranslationSettingGetterServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TranslationProviderFactory {

    private final TranslationAccountRepository translationAccountRepository;
    private final TranslationSettingGetterServices settingsService;

    private volatile TranslationProvider cachedProvider = null;
    private volatile Long cachedAccountId = null;

    public TranslationProvider getActiveProvider() throws TranslationProviderException {
        if (cachedProvider != null) {
            return cachedProvider;
        }

        synchronized (this) {
            if (cachedProvider != null) {
                return cachedProvider;
            }

            // Try to get a default enabled TranslationAccount
            Optional<TranslationAccount> defaultAccount =
                    translationAccountRepository.findFirstByEnabledTrueAndIsDefaultTrueOrderByCreatedAtDesc();

            if (defaultAccount.isPresent()) {
                TranslationAccount account = defaultAccount.get();
                cachedProvider = createProvider(account);
                cachedAccountId = account.getId();
                log.info("Active translation provider: {} (account: {})", account.getProviderType(), account.getName());
                return cachedProvider;
            }

            // Fallback to legacy LibreTranslate settings
            if (settingsService.isLibreTranslateEnabled()) {
                cachedProvider = new LibreTranslateProvider(
                        settingsService.getLibreTranslateBaseUrl(),
                        settingsService.getLibreTranslateApiKey(),
                        settingsService.getLibreTranslateTimeoutSeconds()
                );
                cachedAccountId = null;
                log.info("Active translation provider: LibreTranslate (legacy settings)");
                return cachedProvider;
            }

            throw new TranslationProviderException(
                    "No translation provider configured. Create a translation account or enable LibreTranslate in settings.",
                    TranslationProviderException.ErrorType.SERVICE_DISABLED
            );
        }
    }

    public TranslationProvider createProvider(TranslationAccount account) {
        String decryptedApiKey = null;
        if (account.getApiKey() != null && !account.getApiKey().isBlank()) {
            try {
                decryptedApiKey = EncryptionUtil.decrypt(account.getApiKey());
            } catch (Exception e) {
                log.warn("Failed to decrypt API key for account {}, using raw value", account.getName());
                decryptedApiKey = account.getApiKey();
            }
        }

        int timeout = account.getTimeoutSeconds() != null ? account.getTimeoutSeconds() : 30;

        return switch (account.getProviderType()) {
            case LIBRE_TRANSLATE -> new LibreTranslateProvider(account.getBaseUrl(), decryptedApiKey, timeout);
            case GOOGLE_CLOUD -> new GoogleCloudTranslationProvider(account.getBaseUrl(), decryptedApiKey, timeout);
            case DEEPL -> new DeepLTranslationProvider(account.getBaseUrl(), decryptedApiKey, timeout);
        };
    }

    public void invalidateCache() {
        synchronized (this) {
            cachedProvider = null;
            cachedAccountId = null;
            log.debug("Translation provider cache invalidated");
        }
    }

    public Long getActiveAccountId() {
        return cachedAccountId;
    }

    public boolean hasActiveProvider() {
        if (cachedProvider != null) {
            return true;
        }
        Optional<TranslationAccount> defaultAccount =
                translationAccountRepository.findFirstByEnabledTrueAndIsDefaultTrueOrderByCreatedAtDesc();
        if (defaultAccount.isPresent()) {
            return true;
        }
        return settingsService.isLibreTranslateEnabled();
    }
}
