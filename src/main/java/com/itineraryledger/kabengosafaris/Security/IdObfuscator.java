package com.itineraryledger.kabengosafaris.Security;

import lombok.extern.slf4j.Slf4j;
import org.hashids.Hashids;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.itineraryledger.kabengosafaris.Security.SecuritySettings.SecuritySettingsGetterServices;

/**
 * ID Obfuscator Component
 * Encodes numeric IDs into hash strings to hide internal ID sequences.
 * Configuration values are sourced from the database via SecuritySettingsService.
 * Falls back to application.properties if database settings not available.
 *
 * Configurable Settings:
 * - idObfuscator.obfuscated.length: Length of the hash string (default: 70)
 * - idObfuscator.salt.length: Length of the salt for obfuscation (default: 21)
 */
@Component
@Slf4j
public class IdObfuscator {

    private Hashids hashids;
    private int obfuscatedIdLength;
    private int saltLength;

    // Fallback values from application.properties
    @Value("${security.idObfuscator.obfuscated.length:70}")
    private int defaultObfuscatedLength;

    @Value("${security.idObfuscator.salt.length:21}")
    private int defaultSaltLength;

    /**
     * The salt, when a deployment chooses to fix one. Unset by default, and unset is a decision.
     *
     * UNSET — a new salt each startup. Ids are re-derived on every boot, so an identifier that
     * escaped into a referrer header, a proxy log, a screenshot or a shared browser history stops
     * resolving at the next restart. The cost is that no external reference to a record survives a
     * deploy: a saved deep link, or a saved filter naming a record, goes stale.
     *
     * SET — ids hold still for the life of the salt, and references keep working. What protects the
     * record is then authorization alone, which is where the protection properly belongs: Hashids is
     * an encoding rather than encryption, and a caller who can already read records can supply plenty
     * of id-to-row pairs.
     *
     * Either way it is per company, since two installs sharing a salt would produce ids that are
     * valid shapes in each other. Changing a fixed salt invalidates everything already published,
     * which is why it is stated in configuration and never rotated by the app itself.
     */
    private final String configuredSalt;
    
    /*
     * The salt arrives as a CONSTRUCTOR parameter, not a @Value field.
     *
     * Field injection happens after construction, and the constructor is exactly where the salt is
     * needed — a field would still be null here, so a perfectly good configured salt would be
     * ignored and the warning below would fire on every boot. (The same trap already shaped the
     * length fields, which is why they carry `> 0 ?` fallbacks.)
     */
    @Autowired
    public IdObfuscator(SecuritySettingsGetterServices securitySettingsServices,
                        @Value("${security.idObfuscator.salt:}") String configuredSalt) {
        this.configuredSalt = configuredSalt;
        this.hashids = initializeHashids(securitySettingsServices);
    }

    public void reloadConfig(SecuritySettingsGetterServices securitySettingsServices) {
        this.hashids = initializeHashids(securitySettingsServices);
    }

    /**
     * Initialize Hashids with configuration from database or fallback
     */
    private Hashids initializeHashids(SecuritySettingsGetterServices securitySettingsService) {
        try {
            // Try to get configuration from database
            this.obfuscatedIdLength = securitySettingsService.getIdObfuscationLength();
            this.saltLength = securitySettingsService.getIdObfuscationSaltLength();

            log.info("IdObfuscator: Using database settings - length={}, saltLength={}",
                    obfuscatedIdLength, saltLength);
        } catch (Exception e) {
            // Fallback to application.properties values
            this.obfuscatedIdLength = defaultObfuscatedLength > 0 ? defaultObfuscatedLength : 70;
            this.saltLength = defaultSaltLength > 0 ? defaultSaltLength : 21;

            log.warn("IdObfuscator: Database settings not available, using application.properties fallback - " +
                    "length={}, saltLength={}", obfuscatedIdLength, saltLength);
        }

        // Ensure saltLength is at least 8 for StrongPasswordGenerator
        int saltLengthForGenerator = Math.max(saltLength, 8);

        /*
         * A stated salt is used as it is; otherwise one is generated, as it always was — which keeps
         * every existing installation behaving exactly as before, unstable ids included, until it
         * chooses to set one.
         */
        String salt;
        if (configuredSalt != null && !configuredSalt.isBlank()) {
            salt = configuredSalt.trim();
            log.info("IdObfuscator: fixed salt from configuration — ids hold still across restarts, "
                + "so saved links keep resolving");
        } else {
            salt = StrongPasswordGenerator.generateStrongPassword(saltLengthForGenerator);
            log.info("IdObfuscator: salt generated for this run — ids are re-derived on every start, "
                + "so a leaked identifier expires at the next restart and no external reference to a "
                + "record survives a deploy. Set security.idObfuscator.salt to fix one instead.");
        }

        // Initialize and return Hashids with configured settings
        Hashids result = new Hashids(salt, obfuscatedIdLength);
        log.info("IdObfuscator initialized successfully with length={}, saltLength={}",
                obfuscatedIdLength, saltLength);
        return result;
    }

    /**
     * Encodes a numeric ID into a hash string.
     * @param id The ID to encode
     * @return The encoded hash string
     * @throws IllegalArgumentException if id is null
     */
    public String encodeId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }
        return hashids.encode(id);
    }

    /**
     * Decodes a hash string back to numeric ID.
     * @param hash The hash to decode
     * @return The decoded ID
     * @throws IllegalArgumentException if hash is null or empty
     * @throws IllegalStateException if hash cannot be decoded
     */
    public Long decodeId(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            throw new IllegalArgumentException("Hash cannot be null or empty");
        }

        long[] decoded = hashids.decode(hash);
        if (decoded.length == 0) {
            throw new IllegalStateException("Unable to decode hash: " + hash);
        }

        return decoded[0];
    }
}
